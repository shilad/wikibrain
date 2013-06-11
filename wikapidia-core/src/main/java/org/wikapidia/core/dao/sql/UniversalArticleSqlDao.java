package org.wikapidia.core.dao.sql;

import com.typesafe.config.Config;
import org.jooq.Record;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.UniversalArticleDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.model.UniversalArticle;

import javax.sql.DataSource;

/**
 */
public class UniversalArticleSqlDao extends UniversalPageSqlDao<UniversalArticle> implements UniversalArticleDao {

    public UniversalArticleSqlDao(DataSource dataSource) throws DaoException {
        super(dataSource);
    }

    @Override
    protected UniversalArticle buildUniversalPage(Record record) throws DaoException {
        if (record == null) {
            return null;
        }
        return new UniversalArticle(
                record.getValue(Tables.UNIVERAL_PAGE.UNIV_ID),
                null // TODO: finish this constructor call
        ) {
        };
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
                        (DataSource) getConfigurator().get(
                                DataSource.class,
                                config.getString("dataSource"))
                );
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
