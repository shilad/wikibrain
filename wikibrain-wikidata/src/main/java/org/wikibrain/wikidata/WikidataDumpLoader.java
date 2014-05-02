package org.wikibrain.wikidata;

import org.apache.commons.cli.*;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.cmd.FileMatcher;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.download.DumpFileDownloader;
import org.wikibrain.download.RequestedLinkGetter;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Load the contents of a dump into the various daos.
 */
public class WikidataDumpLoader {
    private static final Logger LOG = Logger.getLogger(WikidataDumpLoader.class.getName());

    private final AtomicInteger counter = new AtomicInteger();

    private final MetaInfoDao metaDao;
    private final WikidataDao wikidataDao;

    public WikidataDumpLoader(WikidataDao wikidataDao, MetaInfoDao metaDao) {
        this.wikidataDao = wikidataDao;
        this.metaDao = metaDao;
    }

    /**
     * Expects file name format starting with lang + "wiki" for example, "enwiki"
     * @param file
     */
    public void load(File file) {
        WikidataDumpParser parser = new WikidataDumpParser(file);
        for (WikidataEntity rp : parser) {
            if (counter.incrementAndGet() % 10000 == 0) {
                LOG.info("processing wikidata entity " + counter.get());
            }
            save(file, rp);
        }
    }

    private void save(File file, WikidataEntity rp) {
        try {
            wikidataDao.save(rp);
            metaDao.incrementRecords(rp.getClass());
        } catch (DaoException e) {
            LOG.log(Level.WARNING, "parsing of " + file + " failed:", e);
            metaDao.incrementErrorsQuietly(rp.getClass());
        }
    }

    public static void main(String args[]) throws ClassNotFoundException, SQLException, IOException, ConfigurationException, DaoException, WikiBrainException, java.text.ParseException, InterruptedException {


        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("drop-tables")
                        .withDescription("drop and recreate all tables")
                        .create("d"));
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

        EnvBuilder builder = new EnvBuilder(cmd);
        if (!builder.hasExplicitLanguageSet()) {
            builder.setUseDownloadedLanguages();
        }
        Env env = builder.build();
        Configurator conf = env.getConfigurator();
        List<File> paths;
        if (cmd.getArgList().isEmpty()) {
            File dumpFile = File.createTempFile("wikiapidia", "dumplinks");
            dumpFile.deleteOnExit();

            // Write a file with the links that the need to be fetched
            RequestedLinkGetter getter = new RequestedLinkGetter(
                    Language.WIKIDATA,
                    Arrays.asList(FileMatcher.ARTICLES),
                    new Date()
            );
            FileUtils.writeLines(dumpFile, getter.getLangLinks());

            // Fetch the file (if necessary) to the standard path
            String filePath = conf.getConf().get().getString("download.path");
            DumpFileDownloader downloader = new DumpFileDownloader(new File(filePath));
            downloader.downloadFrom(dumpFile);

            paths = new ArrayList<File>();
            for (File f : env.getFiles(new LanguageSet(Language.WIKIDATA), FileMatcher.ARTICLES)) {
                if (f.getName().contains("wikidata")) {
                    paths.add(f);
                }
            }
        } else {
            paths = new ArrayList<File>();
            for (Object arg : cmd.getArgList()) {
                paths.add(new File((String)arg));
            }
        }

        WikidataDao wdDao = conf.get(WikidataDao.class);
        MetaInfoDao metaDao = conf.get(MetaInfoDao.class);

        final WikidataDumpLoader loader = new WikidataDumpLoader(wdDao, metaDao);

        if (cmd.hasOption("d")) {
            wdDao.clear();
            metaDao.clear(WikidataStatement.class);
        }
        wdDao.beginLoad();
        metaDao.beginLoad();

        // loads multiple dumps in parallel
        ParallelForEach.loop(paths,
                new Procedure<File>() {
                    @Override
                    public void call(File path) throws Exception {
                        LOG.info("processing file: " + path);
                        loader.load(path);
                    }
                });

        wdDao.endLoad();
        metaDao.endLoad();

        LOG.info("optimizing database.");
        conf.get(WpDataSource.class).optimize();
    }
}
