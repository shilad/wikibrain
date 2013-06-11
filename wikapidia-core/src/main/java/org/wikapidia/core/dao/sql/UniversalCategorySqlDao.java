package org.wikapidia.core.dao.sql;

import com.typesafe.config.Config;
import org.jooq.Record;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.UniversalCategoryDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.model.UniversalCategory;

import javax.sql.DataSource;

/**
 */
public class UniversalCategorySqlDao extends UniversalPageSqlDao<UniversalCategory> implements UniversalCategoryDao {

    public UniversalCategorySqlDao(DataSource dataSource) throws DaoException {
        super(dataSource);
    }

    @Override
    protected UniversalCategory buildUniversalPage(Record record) throws DaoException {
        if (record == null) {
            return null;
        }
        return new UniversalCategory(
                record.getValue(Tables.UNIVERAL_PAGE.UNIV_ID),
                null // TODO: finish this constructor call
        ) {
        };
    }

    public static class Provider extends org.wikapidia.conf.Provider<UniversalCategoryDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return UniversalCategoryDao.class;
        }

        @Override
        public String getPath() {
            return "dao.universalCategory";
        }

        @Override
        public UniversalCategoryDao get(String name, Config config) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                return new UniversalCategorySqlDao(
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
