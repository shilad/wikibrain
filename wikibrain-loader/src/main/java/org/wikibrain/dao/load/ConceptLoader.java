package org.wikibrain.dao.load;

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

import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.download.DumpFileDownloader;
import org.wikibrain.download.RequestedLinkGetter;
import org.wikibrain.mapper.ConceptMapper;
import org.wikibrain.mapper.algorithms.PureWikidataConceptMapper;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Loads an Iterable of mapped concepts (Universal Pages) into a database.
 *
 * @author Ari Weiland
 *
 */
public class ConceptLoader {
    private static final Logger LOG = Logger.getLogger(ConceptLoader.class.getName());
    private final LanguageSet languageSet;
    private final UniversalPageDao dao;

    public ConceptLoader(LanguageSet languageSet, UniversalPageDao dao) {
        this.languageSet = languageSet;
        this.dao = dao;
    }

    public UniversalPageDao getDao() {
        return dao;
    }

    public void load(ConceptMapper mapper) throws ConfigurationException, WikiBrainException {
        try {
            LOG.log(Level.INFO, "Loading Concepts");
            Iterator<UniversalPage> pages = mapper.getConceptMap(languageSet);
            int i = 0;
            while (pages.hasNext()) {
                dao.save(pages.next());
                i++;
                if (i%10000 == 0) LOG.log(Level.INFO, "UniversalPages loaded: " + i);
            }
            LOG.log(Level.INFO, "All UniversalPages loaded: " + i);
        } catch (DaoException e) {
            throw new WikiBrainException(e);
        }
    }

    public static void downloadWikidataLinks(Configuration conf) throws IOException, WikiBrainException, java.text.ParseException, InterruptedException {
        List<File> paths = Env.getFiles(Language.WIKIDATA, FileMatcher.WIKIDATA_ITEMS, conf);
        if (paths.isEmpty()) {
            File dumpFile = File.createTempFile("wikiapidia", "items");
            dumpFile.deleteOnExit();
            LOG.info("downloading wikidata items file");
            RequestedLinkGetter getter = new RequestedLinkGetter(
                    Language.WIKIDATA,
                    Arrays.asList(FileMatcher.WIKIDATA_ITEMS),
                    new Date()
            );
            FileUtils.writeLines(dumpFile, getter.getLangLinks());

            // Fetch the file (if necessary) to the standard path
            String filePath = conf.get().getString("download.path");
            DumpFileDownloader downloader = new DumpFileDownloader(new File(filePath));
            downloader.downloadFrom(dumpFile);
        }
    }

    public static void main(String args[]) throws ClassNotFoundException, SQLException, IOException, ConfigurationException, WikiBrainException, DaoException, java.text.ParseException, InterruptedException {
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
            System.err.println( "Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("ConceptLoader", options);
            return;
        }

        Env env = new EnvBuilder(cmd).build();
        Configurator conf = env.getConfigurator();
        String algorithm = cmd.getOptionValue("n", null);

        UniversalPageDao dao = conf.get(UniversalPageDao.class);
        if (algorithm == null) {
            algorithm = (env.getLanguages().size() <= 1) ? "monolingual" : "purewikidata";
        }

        // TODO: handle checking of purewikidata more robustly
        if (algorithm.equals("purewikidata")) {
            downloadWikidataLinks(env.getConfiguration());
        }

        ConceptMapper mapper = conf.get(ConceptMapper.class, algorithm);
        final ConceptLoader loader = new ConceptLoader(env.getLanguages(), dao);

        if (cmd.hasOption("d")) {
            LOG.log(Level.INFO, "Clearing data");
            dao.clear();
        }
        LOG.log(Level.INFO, "Begin Load");
        dao.beginLoad();

        loader.load(mapper);

        LOG.log(Level.INFO, "End Load");
        dao.endLoad();

        LOG.info("optimizing database.");
        conf.get(WpDataSource.class).optimize();

        LOG.log(Level.INFO, "DONE");
    }
}
