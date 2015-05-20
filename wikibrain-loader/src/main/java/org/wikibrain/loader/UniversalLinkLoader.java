package org.wikibrain.loader;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Generates and loads the Universal Link map into a database.
 *
 * @author Ari Weiland
 *
 */
public class UniversalLinkLoader {
    private static final Logger LOG = LoggerFactory.getLogger(UniversalLinkLoader.class);

    private final LanguageSet languageSet;
    private final LocalLinkDao localLinkDao;
    private final UniversalPageDao universalPageDao;
    private final UniversalLinkDao universalLinkDao;
    private final UniversalLinkDao universalLinkSkeletalDao;
    private final MetaInfoDao metaDao;

    public UniversalLinkLoader(LanguageSet languageSet, LocalLinkDao localLinkDao, UniversalPageDao universalPageDao, UniversalLinkDao universalLinkDao, UniversalLinkDao universalLinkSkeletalDao, MetaInfoDao metaDao) {
        this.languageSet = languageSet;
        this.localLinkDao = localLinkDao;
        this.universalPageDao = universalPageDao;
        this.universalLinkDao = universalLinkDao;
        this.universalLinkSkeletalDao = universalLinkSkeletalDao;
        this.metaDao = metaDao;
    }

    public void beginLoad(boolean shouldClear) throws DaoException {
        if (shouldClear) {
            LOG.info("Clearing data");
            universalLinkDao.clear();
            universalLinkSkeletalDao.clear();
        }
        LOG.info("Begin Load");
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
            LOG.info("Fetching ID map");
            Map<Language, TIntIntMap> map = universalPageDao.getAllLocalToUnivIdsMap(languageSet);
            LOG.info("Loading links");
            long start = System.currentTimeMillis();
            int i=0;
            for (LocalLink localLink : localLinks) {
                i++;
                if (i%100000 == 0)
                    LOG.info("UniversalLinks loaded: " + i);
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
                metaDao.incrementRecords(UniversalLink.class);
            }
            long end = System.currentTimeMillis();
            double seconds = (end - start) / 1000.0;
            LOG.info("Time (s): " + seconds);
            LOG.info("All UniversalLinks loaded: " + i);
        } catch (DaoException e) {
            throw new WikiBrainException(e);
        }
    }

    public void endLoad() throws DaoException {
        LOG.info("End Load");
        long start = System.currentTimeMillis();
        universalLinkDao.endLoad();
        universalLinkSkeletalDao.endLoad();
        long end = System.currentTimeMillis();
        double seconds = (end - start) / 1000.0;
        LOG.info("Time (s): " + seconds);
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
        UniversalLinkDao universalLinkSkeletalDao = conf.get(UniversalLinkDao.class, "skeletal-sql-wikidata");
        ConceptMapper mapper = conf.get(ConceptMapper.class, algorithm);
        MetaInfoDao metaDao = conf.get(MetaInfoDao.class);

        UniversalLinkLoader loader = new UniversalLinkLoader(
                env.getLanguages(),
                localLinkDao,
                universalPageDao,
                universalLinkDao,
                universalLinkSkeletalDao,
                metaDao);

        System.out.println("loading " + mapper.getId());
        loader.beginLoad(cmd.hasOption("d"));
        loader.loadLinkMap(mapper.getId());
        loader.endLoad();
        LOG.info("DONE");
    }
}
