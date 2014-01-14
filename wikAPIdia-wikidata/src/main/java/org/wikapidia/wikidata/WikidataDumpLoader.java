package org.wikapidia.wikidata;

import org.apache.commons.cli.*;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.cmd.FileMatcher;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.MetaInfoDao;
import org.wikapidia.core.dao.RawPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.parser.xml.DumpPageXmlParser;
import org.wikapidia.utils.ParallelForEach;
import org.wikapidia.utils.Procedure;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Load the contents of a dump into the various daos.
 */
public class WikidataDumpLoader {
    private static final Logger LOG = Logger.getLogger(WikidataDumpLoader.class.getName());

    private final AtomicInteger counter = new AtomicInteger();

    private final LocalPageDao localPageDao;
    private final MetaInfoDao metaDao;
    private final WikidataDao wikidataDao;

    public WikidataDumpLoader(WikidataDao wikidataDao, LocalPageDao localPageDao, MetaInfoDao metaDao) {
        this.wikidataDao = wikidataDao;
        this.localPageDao = localPageDao;
        this.metaDao = metaDao;
    }

    /**
     * Expects file name format starting with lang + "wiki" for example, "enwiki"
     * @param file
     */
    public void load(File file) {
        WikidataDumpParser parser = new WikidataDumpParser(file);
        for (WikidataRawRecord rp : parser) {
            if (counter.incrementAndGet() % 10000 == 0) {
                LOG.info("processing article " + counter.get());
            }
            save(file, rp);
        }
    }

    private void save(File file, WikidataRawRecord rp) {
        for (WikidataStatement st : rp.getStatements()) {
            try {
                wikidataDao.save(st);
                metaDao.incrementRecords(st.getClass());
            } catch (DaoException e) {
                LOG.log(Level.WARNING, "parsing of " + file + " failed:", e);
                metaDao.incrementErrorsQuietly(st.getClass());
            }
        }
    }

    public static void main(String args[]) throws ClassNotFoundException, SQLException, IOException, ConfigurationException, DaoException {
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
            paths = env.getFiles(FileMatcher.ARTICLES);
        } else {
            paths = new ArrayList<File>();
            for (Object arg : cmd.getArgList()) {
                paths.add(new File((String)arg));
            }
        }

        WikidataDao wdDao = conf.get(WikidataDao.class);
        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        MetaInfoDao metaDao = conf.get(MetaInfoDao.class);

        final WikidataDumpLoader loader = new WikidataDumpLoader(wdDao, lpDao, metaDao);

        if (cmd.hasOption("d")) {
            lpDao.clear();
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
    }
}
