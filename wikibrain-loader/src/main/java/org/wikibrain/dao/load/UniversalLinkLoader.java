package org.wikibrain.dao.load;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import gnu.trove.map.TIntIntMap;
import org.apache.commons.cli.*;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.UniversalLink;
import org.wikibrain.mapper.ConceptMapper;

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
    private static final Logger LOG = Logger.getLogger(UniversalLinkLoader.class.getName());

    private final LanguageSet languageSet;
    private final LocalLinkDao localLinkDao;
    private final UniversalPageDao universalPageDao;
    private final UniversalLinkDao universalLinkDao;
    private final UniversalLinkDao universalLinkSkeletalDao;

    public UniversalLinkLoader(LanguageSet languageSet, LocalLinkDao localLinkDao, UniversalPageDao universalPageDao, UniversalLinkDao universalLinkDao, UniversalLinkDao universalLinkSkeletalDao) {
        this.languageSet = languageSet;
        this.localLinkDao = localLinkDao;
        this.universalPageDao = universalPageDao;
        this.universalLinkDao = universalLinkDao;
        this.universalLinkSkeletalDao = universalLinkSkeletalDao;
    }

    public void beginLoad(boolean shouldClear) throws DaoException {
        if (shouldClear) {
            LOG.log(Level.INFO, "Clearing data");
            universalLinkDao.clear();
            universalLinkSkeletalDao.clear();
        }
        LOG.log(Level.INFO, "Begin Load");
        universalLinkDao.beginLoad();
        universalLinkSkeletalDao.beginLoad();
    }

    /**
     * Loads the database of UniversalLinks. Requires a database of UniversalPages and LocalLinks
     * @throws WikiBrainException
     */
    public void loadLinkMap(int algorithmId) throws WikiBrainException {
        try {
            Iterable<LocalLink> localLinks = localLinkDao.get(new DaoFilter().setLanguages(languageSet));
            LOG.log(Level.INFO, "Fetching ID map");
            Map<Language, TIntIntMap> map = universalPageDao.getAllLocalToUnivIdsMap(algorithmId, languageSet);
            LOG.log(Level.INFO, "Loading links");
            long start = System.currentTimeMillis();
            int i=0;
            for (LocalLink localLink : localLinks) {
                i++;
                if (i%100000 == 0)
                    LOG.log(Level.INFO, "UniversalLinks loaded: " + i);
                int univSourceId, univDestId;
                if (localLink.getSourceId() < 0) {
                    univSourceId = -1;
                } else {
                    univSourceId = map.get(localLink.getLanguage()).get(localLink.getSourceId());
                }
                if (localLink.getDestId() < 0) {
                    univDestId = -1;
                } else {
                    univDestId = map.get(localLink.getLanguage()).get(localLink.getDestId());
                }
                Multimap<Language, LocalLink> linkMap = HashMultimap.create();
                linkMap.put(localLink.getLanguage(), localLink);
                UniversalLink link = new UniversalLink(univSourceId, univDestId, algorithmId, linkMap);
                universalLinkDao.save(link);
                universalLinkSkeletalDao.save(link);
            }
            long end = System.currentTimeMillis();
            double seconds = (end - start) / 1000.0;
            LOG.log(Level.INFO, "Time (s): " + seconds);
            LOG.log(Level.INFO, "All UniversalLinks loaded: " + i);
        } catch (DaoException e) {
            throw new WikiBrainException(e);
        }
    }

    public void endLoad() throws DaoException {
        LOG.log(Level.INFO, "End Load");
        long start = System.currentTimeMillis();
        universalLinkDao.endLoad();
        universalLinkSkeletalDao.endLoad();
        long end = System.currentTimeMillis();
        double seconds = (end - start) / 1000.0;
        LOG.log(Level.INFO, "Time (s): " + seconds);
    }

    public static void main(String args[]) throws ClassNotFoundException, SQLException, IOException, ConfigurationException, WikiBrainException, DaoException {
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
            new HelpFormatter().printHelp("UniversalLinkLoader", options);
            return;
        }

        Env env = new EnvBuilder(cmd).build();
        Configurator conf = env.getConfigurator();
        String algorithm = cmd.getOptionValue("n", null);

        LocalLinkDao localLinkDao = conf.get(LocalLinkDao.class);
        UniversalPageDao universalPageDao = conf.get(UniversalPageDao.class);
        UniversalLinkDao universalLinkDao = conf.get(UniversalLinkDao.class);
        UniversalLinkDao universalLinkSkeletalDao = conf.get(UniversalLinkDao.class, "skeletal-sql");
        ConceptMapper mapper = conf.get(ConceptMapper.class, algorithm);

        UniversalLinkLoader loader = new UniversalLinkLoader(
                env.getLanguages(),
                localLinkDao,
                universalPageDao,
                universalLinkDao,
                universalLinkSkeletalDao
        );

        loader.beginLoad(cmd.hasOption("d"));
        loader.loadLinkMap(mapper.getId());
        loader.endLoad();
        LOG.log(Level.INFO, "DONE");

        LOG.info("optimizing database.");
        conf.get(WpDataSource.class).optimize();
    }
}
