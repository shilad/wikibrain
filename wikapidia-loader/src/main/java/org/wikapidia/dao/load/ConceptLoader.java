package org.wikapidia.dao.load;

import org.apache.commons.cli.*;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.dao.DaoException;

import org.wikapidia.core.dao.UniversalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.mapper.ConceptMapper;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ari Weiland
 *
 * Loads an Iterable of mapped concepts (Universal Pages) into a database.
 */
public class ConceptLoader {
    private static final Logger LOG = Logger.getLogger(DumpLoader.class.getName());
    private final LanguageSet languageSet;
    private final UniversalPageDao dao;

    public ConceptLoader(LanguageSet languageSet, UniversalPageDao dao) {
        this.languageSet = languageSet;
        this.dao = dao;
    }

    public void load(ConceptMapper mapper) throws ConfigurationException, WikapidiaException {
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
            throw new WikapidiaException(e);
        }
    }

    public static void main(String args[]) throws ClassNotFoundException, SQLException, IOException, ConfigurationException, WikapidiaException, DaoException {
        Options options = new Options();
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("drop-tables")
                        .withDescription("drop and recreate all tables")
                        .create("t"));
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("create-indexes")
                        .withDescription("create all indexes after loading")
                        .create("i"));
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArg()
                        .withLongOpt("algorithm")
                        .withDescription("the name of the algorithm to execute")
                        .create("n"));
        Env.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println( "Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("ConceptLoader", options);
            return;
        }

        Env env = new Env(cmd);
        Configurator conf = env.getConfigurator();
        String algorithm = cmd.getOptionValue("n", null);

        UniversalPageDao dao = conf.get(UniversalPageDao.class);
        ConceptMapper mapper = conf.get(ConceptMapper.class, algorithm);
        final ConceptLoader loader = new ConceptLoader(env.getLanguages(), dao);

        if (cmd.hasOption("t")) {
            LOG.log(Level.INFO, "Begin Load");
            dao.beginLoad();
        }

        loader.load(mapper);

        if (cmd.hasOption("i")) {
            LOG.log(Level.INFO, "End Load");
            dao.endLoad();
        }
        LOG.log(Level.INFO, "DONE");
    }
}
