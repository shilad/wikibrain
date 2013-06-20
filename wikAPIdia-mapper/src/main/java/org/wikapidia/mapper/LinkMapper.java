package org.wikapidia.mapper;

import com.typesafe.config.Config;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.dao.filter.LinkFilter;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.lang.LanguageSet;

/**
 */
public class LinkMapper {

    private final LocalLinkDao localLinkDao;
    private final UniversalPageDao universalPageDao;
    private final UniversalLinkDao universalLinkDao;

    public LinkMapper(LocalLinkDao localLinkDao, UniversalPageDao universalPageDao, UniversalLinkDao universalLinkDao) {
        this.localLinkDao = localLinkDao;
        this.universalPageDao = universalPageDao;
        this.universalLinkDao = universalLinkDao;
    }

    /**
     * Loads the database of UniversalLinks. Requires a database of UniversalPages and LocalLinks
     * @throws DaoException
     */
    public void generateLinkMap(LanguageSet ls, int algorithmId) throws DaoException {
        SqlDaoIterable<LocalLink> localLinks = localLinkDao.get(new LinkFilter().setLanguages(ls));
        universalLinkDao.beginLoad();
        for (LocalLink localLink : localLinks) {
            universalLinkDao.save(
                    localLink,
                    universalPageDao.getUnivPageId(
                            localLink.getLanguage(),
                            localLink.getSourceId(),
                            algorithmId),
                    universalPageDao.getUnivPageId(
                            localLink.getLanguage(),
                            localLink.getDestId(),
                            algorithmId),
                    algorithmId
            );
        }
        universalLinkDao.endLoad();
    }

    public static class Provider extends org.wikapidia.conf.Provider<LinkMapper> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return LinkMapper.class;
        }

        @Override
        public String getPath() {
            return "mapper";
        }

        @Override
        public LinkMapper get(String name, Config config) throws ConfigurationException {
            return new LinkMapper(
                    getConfigurator().get(
                            LocalLinkDao.class,
                            config.getString("localLinkDao")),
                    getConfigurator().get(
                            UniversalPageDao.class,
                            config.getString("universalPageDao")),
                    getConfigurator().get(
                            UniversalLinkDao.class,
                            config.getString("universalLinkDao"))
            );
        }
    }
}
