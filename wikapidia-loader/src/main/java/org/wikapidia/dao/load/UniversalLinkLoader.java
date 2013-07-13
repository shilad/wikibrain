package org.wikapidia.dao.load;

import gnu.trove.map.TIntIntMap;
import org.apache.commons.cli.*;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.mapper.ConceptMapper;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Generates and loads the Universal Link map into a database.
 *
 * @author Ari Weiland
 *
 */
public class UniversalLinkLoader {
    private static final Logger LOG = Logger.getLogger(DumpLoader.class.getName());

    private final LanguageSet languageSet;
    private final LocalLinkDao localLinkDao;
    private final UniversalPageDao universalPageDao;
    private final UniversalLinkDao universalLinkDao;

    public UniversalLinkLoader(LanguageSet languageSet, LocalLinkDao localLinkDao, UniversalPageDao universalPageDao, UniversalLinkDao universalLinkDao) {
        this.languageSet = languageSet;
        this.localLinkDao = localLinkDao;
        this.universalPageDao = universalPageDao;
        this.universalLinkDao = universalLinkDao;
    }

    public UniversalLinkDao getDao() {
        return universalLinkDao;
    }

    /**
     * Loads the database of UniversalLinks. Requires a database of UniversalPages and LocalLinks
     * @throws DaoException
     */
    public void loadLinkMap(int algorithmId) throws WikapidiaException {
        try {
            Iterable<LocalLink> localLinks = localLinkDao.get(new DaoFilter().setLanguages(languageSet));
            LOG.log(Level.INFO, "Fetching ID map");
            Map<Language, TIntIntMap> map = universalPageDao.getAllLocalToUnivIdsMap(algorithmId, languageSet);
            LOG.log(Level.INFO, "Loading links");
            int i=0;
            for (LocalLink localLink : localLinks) {
                i++;
                if (i%100000 == 0)
                    LOG.log(Level.INFO, "UniversalLinks loaded: " + i);
                int sourceUnivId, destUnivId;
                if (localLink.getSourceId() < 0) {
                    sourceUnivId = -1;
                } else {
                    sourceUnivId = map.get(localLink.getLanguage()).get(localLink.getSourceId());
                }
                if (localLink.getDestId() < 0) {
                    destUnivId = -1;
                } else {
                    destUnivId = map.get(localLink.getLanguage()).get(localLink.getDestId());
                }
                universalLinkDao.save(
                        localLink,
                        sourceUnivId,
                        destUnivId,
                        algorithmId
                );
            }
            LOG.log(Level.INFO, "All UniversalLinks loaded: " + i);
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
                        .create("d"));
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
            new HelpFormatter().printHelp("UniversalLinkLoader", options);
            return;
        }

        Env env = new Env(cmd);
        Configurator conf = env.getConfigurator();
        String algorithm = cmd.getOptionValue("n", null);

        LocalLinkDao localLinkDao = conf.get(LocalLinkDao.class);
        UniversalPageDao universalPageDao = conf.get(UniversalPageDao.class);
        UniversalLinkDao universalLinkDao = conf.get(UniversalLinkDao.class);
        ConceptMapper mapper = conf.get(ConceptMapper.class, algorithm);

        UniversalLinkLoader loader = new UniversalLinkLoader(
                env.getLanguages(),
                localLinkDao,
                universalPageDao,
                universalLinkDao
        );

        if (cmd.hasOption("d")) {
            LOG.log(Level.INFO, "Clearing data");
            universalLinkDao.clear();
        }
        LOG.log(Level.INFO, "Begin Load");
        universalLinkDao.beginLoad();

        loader.loadLinkMap(mapper.getId());

        LOG.log(Level.INFO, "End Load");
        universalLinkDao.endLoad();
        LOG.log(Level.INFO, "DONE");
    }
}
