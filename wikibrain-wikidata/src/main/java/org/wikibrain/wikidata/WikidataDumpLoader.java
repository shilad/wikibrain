package org.wikibrain.wikidata;

import gnu.trove.map.TIntIntMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.download.FileDownloader;
import org.wikibrain.parser.WpParseException;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpIOUtils;
import org.wikibrain.utils.WpThreadUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load the contents of a dump into the various daos.
 */
public class WikidataDumpLoader {
    private static final Logger LOG = LoggerFactory.getLogger(WikidataDumpLoader.class);

    private final AtomicInteger counter = new AtomicInteger();

    private final MetaInfoDao metaDao;
    private final WikidataDao wikidataDao;
    private final UniversalPageDao universalPageDao;
    private final LanguageSet languages;
    private final WikidataParser wdParser = new WikidataParser();
    private final TIntSet universalIds;
    private boolean keepAllLabeledEntities = false;

    public WikidataDumpLoader(WikidataDao wikidataDao, MetaInfoDao metaDao, UniversalPageDao upDao, LanguageSet langs) throws DaoException {
        this.wikidataDao = wikidataDao;
        this.metaDao = metaDao;
        this.languages = langs;
        this.universalPageDao = upDao;
        Map<Language, TIntIntMap> localMaps = universalPageDao.getAllUnivToLocalIdsMap(languages);

        // Build up set of universal ids from the local ids that we know about
        this.universalIds = new TIntHashSet();
        for(TIntIntMap langMap : localMaps.values()) {
            universalIds.addAll(langMap.keys());
        }
    }

    /**
     * Expects file name format starting with lang + "wiki" for example, "enwiki"
     *
     * @param file
     */
    public void load(final File file) throws IOException {
        LineIterator lines = new LineIterator(WpIOUtils.openBufferedReader(file));
        ParallelForEach.iterate(
                lines,
                WpThreadUtils.getMaxThreads(),
                1000,
                new Procedure<String>() {
                    @Override
                    public void call(String page) {
                        try {
                            save(file, page);
                            metaDao.incrementRecords(WikidataEntity.class);
                        } catch (WpParseException e) {
                            LOG.warn("parsing of " + file.getPath() + " failed:", e);
                            metaDao.incrementErrorsQuietly(WikidataEntity.class);
                        } catch (DaoException e) {
                            LOG.warn("parsing of " + file.getPath() + " failed:", e);
                            metaDao.incrementErrorsQuietly(WikidataEntity.class);
                        }
                    }
                },
                Integer.MAX_VALUE
        );
        lines.close();
    }

    private void save(File file, String json) throws WpParseException, DaoException {
        if (!json.contains("{")) {
            return;
        }
        json = json.trim();
        if (json.endsWith(",")) {
            json = json.substring(0, json.length()-1);
        }
        if (counter.incrementAndGet() % 100000 == 0) {
            LOG.info("processing wikidata entity " + counter.get());
        }
        WikidataEntity entity = wdParser.parse(json);
        // check if others use prune's boolean?
        entity.prune(languages);

        if (keepEntity(entity)) {
            wikidataDao.save(entity);
        }
    }

    private boolean keepEntity(WikidataEntity entity) {
        if (entity.getType() == WikidataEntity.Type.PROPERTY) {
            return true;
        } else if (universalIds.contains(entity.getId())) {
            return true;
        } else if (keepAllLabeledEntities && !entity.getLabels().isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    public void setKeepAllLabeledEntities(boolean keepAllLabeledEntities) {
        this.keepAllLabeledEntities = keepAllLabeledEntities;
    }

    public static void main(String args[]) throws ClassNotFoundException, SQLException, IOException, ConfigurationException, DaoException, WikiBrainException, java.text.ParseException, InterruptedException {


        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("drop-tables")
                        .withDescription("drop and recreate all tables")
                        .create("d"));
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("keep-labeled")
                        .withDescription("keep all labeled entities")
                        .create("k"));
        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("WikidataDumpLoader", options);
            return;
        }

        Env env =  new EnvBuilder(cmd).build();
        Configurator conf = env.getConfigurator();
        File path;
        if (cmd.getArgList().isEmpty()) {
            WikidataDumpHelper helper = new WikidataDumpHelper();

            // Fetch the file (if necessary) to the standard path
            String downloadDir = conf.getConf().get().getString("download.path");
            File dest = FileUtils.getFile(downloadDir, helper.getMostRecentFile());
            if (!dest.isFile()) {
                dest.getParentFile().mkdirs();
                File tmp = File.createTempFile("wikibrain-wikidata", "json");
                FileUtils.deleteQuietly(tmp);
                URL url = new URL(helper.getMostRecentUrl());
                FileDownloader downloader = new FileDownloader();
                downloader.download(url, tmp);
                if (dest.isFile()) {
                    throw new IllegalStateException();
                }
                FileUtils.moveFile(tmp, dest);
            }
            path = dest;
        } else if (cmd.getArgList().size() == 1) {
            path = new File(cmd.getArgList().get(0).toString());
        } else {
            System.err.println("Invalid option usage:");
            new HelpFormatter().printHelp("WikidataDumpLoader", options);
            return;
        }

        WikidataDao wdDao = conf.get(WikidataDao.class);
        UniversalPageDao upDao = conf.get(UniversalPageDao.class);
        MetaInfoDao metaDao = conf.get(MetaInfoDao.class);
        LanguageSet langs = conf.get(LanguageSet.class);

        WikidataDumpLoader loader = new WikidataDumpLoader(wdDao, metaDao, upDao, langs);

        if (cmd.hasOption("d")) {
            wdDao.clear();
            metaDao.clear(WikidataStatement.class);
        }
        if (cmd.hasOption("k")) {
            loader.setKeepAllLabeledEntities(true);
        }
        wdDao.beginLoad();
        metaDao.beginLoad();
        loader.load(path);

        LOG.info("building indexes");
        wdDao.endLoad();
        metaDao.endLoad();
        LOG.info("finished");
    }
}
