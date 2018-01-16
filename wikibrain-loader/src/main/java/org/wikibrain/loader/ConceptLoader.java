package org.wikibrain.loader;

import org.apache.commons.cli.*;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.cmd.FileMatcher;
import org.wikibrain.core.dao.DaoException;

import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.download.DumpFileDownloader;
import org.wikibrain.download.FileDownloader;
import org.wikibrain.download.RequestedLinkGetter;
import org.wikibrain.mapper.ConceptMapper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikibrain.mapper.algorithms.PureWikidataConceptMapper;

/**
 *
 * Loads an Iterable of mapped concepts (Universal Pages) into a database.
 *
 * @author Ari Weiland
 *
 */
public class ConceptLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ConceptLoader.class);
    public static final String WIKIDATA_JSON_URL = "https://dumps.wikimedia.org/wikidatawiki/entities/latest-all.json.bz2";
    private final LanguageSet languageSet;
    private final UniversalPageDao dao;
    private final MetaInfoDao metaDao;

    public ConceptLoader(LanguageSet languageSet, UniversalPageDao dao, MetaInfoDao metaDao) {
        this.languageSet = languageSet;
        this.dao = dao;
        this.metaDao = metaDao;
    }

    public UniversalPageDao getDao() {
        return dao;
    }

    public void load(ConceptMapper mapper) throws ConfigurationException, WikiBrainException {
        try {
            LOG.info("Loading Concepts");
            Iterator<UniversalPage> pages = mapper.getConceptMap(languageSet);
            int i = 0;
            while (pages.hasNext()) {
                dao.save(pages.next());
                i++;
                if (i%10000 == 0) LOG.info("UniversalPages loaded: " + i);
                metaDao.incrementRecords(UniversalPage.class);
            }
            LOG.info("All UniversalPages loaded: " + i);
        } catch (DaoException e) {
            metaDao.incrementErrorsQuietly(UniversalPage.class);
            throw new WikiBrainException(e);
        }
    }

    public static void downloadWikidataLinks(Configuration conf) throws IOException, WikiBrainException, java.text.ParseException, InterruptedException {
        File f = PureWikidataConceptMapper.getJsonLocation(conf);
        if (!f.exists()) {
            File tf = File.createTempFile("wikidata.json", null);
            tf.delete();
            FileDownloader dl = new FileDownloader();
            dl.download(new URL(WIKIDATA_JSON_URL), tf);
            tf.renameTo(f);
        }
    }

    public static void main(String args[]) throws ClassNotFoundException, SQLException, IOException, ConfigurationException, WikiBrainException, DaoException, java.text.ParseException, InterruptedException {
        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("drop-tables")
                        .withDescription("drop and recreate all tables")
                        .create("d"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("algorithm")
                        .withDescription("algorithm")
                        .create("n"));
        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println( "Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("ConceptLoader", options);
            return;
        }

        Env env = new EnvBuilder(cmd).build();
        Configurator conf = env.getConfigurator();
        String algorithm = cmd.getOptionValue("n", null);

        UniversalPageDao dao = conf.get(UniversalPageDao.class, algorithm);
        MetaInfoDao metaDao = conf.get(MetaInfoDao.class);

        // TODO: handle checking of purewikidata more robustly
        if (algorithm == null || algorithm.equals("purewikidata")) {
            downloadWikidataLinks(env.getConfiguration());
        }

        ConceptMapper mapper = conf.get(ConceptMapper.class, algorithm);
        final ConceptLoader loader = new ConceptLoader(env.getLanguages(), dao, metaDao);

        if (cmd.hasOption("d")) {
            LOG.info("Clearing data");
            dao.clear();
        }
        LOG.info("Begin Load");
        dao.beginLoad();

        loader.load(mapper);

        LOG.info("End Load");
        dao.endLoad();

        LOG.info("DONE");
    }
}
