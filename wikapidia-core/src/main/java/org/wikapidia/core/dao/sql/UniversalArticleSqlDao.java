package org.wikapidia.core.dao.sql;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.typesafe.config.Config;
import org.jooq.Record;
import org.jooq.Result;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalArticleDao;
import org.wikapidia.core.dao.UniversalArticleDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.UniversalArticle;

import javax.sql.DataSource;

/**
 */
public class UniversalArticleSqlDao extends UniversalPageSqlDao<UniversalArticle> implements UniversalArticleDao {

    public UniversalArticleSqlDao(DataSource dataSource, LocalArticleDao localArticleDao) throws DaoException {
        super(dataSource, localArticleDao);
    }

    @Override
    protected UniversalArticle buildUniversalPage(Result<Record> result) throws DaoException {
        if (result.isEmpty()) {
            return null;
        }
        Multimap<Language, LocalArticle> localPages = HashMultimap.create(result.size(), result.size());
        for(Record record : result) {
            NameSpace nameSpace = NameSpace.getNameSpaceById(record.getValue(Tables.LOCAL_PAGE.NAME_SPACE));
            if (nameSpace != NameSpace.ARTICLE) {
                throw new DaoException("Tried to get ARTICLE, but found " + nameSpace);
            }
            Language language = Language.getById(record.getValue(Tables.UNIVERSAL_PAGE.LANG_ID));
            int pageId = record.getValue(Tables.UNIVERSAL_PAGE.PAGE_ID);
            LocalArticleSqlDao localDao = new LocalArticleSqlDao(ds);
            localPages.put(language, localDao.getById(language, pageId));
        }
        return new UniversalArticle(
                result.get(0).getValue(Tables.UNIVERSAL_PAGE.UNIV_ID),
                result.get(0).getValue(Tables.UNIVERSAL_PAGE.ALGORITHM_ID),
                localPages
        );
    }

    public static class Provider extends org.wikapidia.conf.Provider<UniversalArticleDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return UniversalArticleDao.class;
        }

        @Override
        public String getPath() {
            return "dao.universalArticle";
        }

        @Override
        public UniversalArticleDao get(String name, Config config) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                return new UniversalArticleSqlDao(
                        getConfigurator().get(
                                DataSource.class,
                                config.getString("dataSource")),
                        getConfigurator().get(
                                LocalArticleDao.class,
                                config.getString("localArticleDao"))
                );
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
