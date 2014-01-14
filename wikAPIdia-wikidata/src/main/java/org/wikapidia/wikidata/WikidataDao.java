package org.wikapidia.wikidata;

import com.google.gson.Gson;
import com.typesafe.config.Config;
import org.jooq.TableField;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.sql.AbstractSqlDao;
import org.wikapidia.core.dao.sql.WpDataSource;

import java.util.Map;

import static org.wikapidia.core.jooq.tables.WikidataStatement.*;

/**
 * @author Shilad Sen
 */
public class WikidataDao extends AbstractSqlDao<WikidataStatement> {
    private static TableField[] FIELDS = new TableField[] {
            WIKIDATA_STATEMENT.ID,
            WIKIDATA_STATEMENT.ITEM_ID,
            WIKIDATA_STATEMENT.PROP_ID,
            WIKIDATA_STATEMENT.VAL_TYPE,
            WIKIDATA_STATEMENT.VAL_STR,
            WIKIDATA_STATEMENT.RANK
    };
    private Gson gson = new Gson();

    /**
     * @param dataSource      Data source for jdbc connections
     * @throws org.wikapidia.core.dao.DaoException
     */
    public WikidataDao(WpDataSource dataSource) throws DaoException {
        super(dataSource, FIELDS, "/db/wikidata");
    }

    @Override
    public void save(WikidataStatement item) throws DaoException {
        insert(
                item.getId(),
                item.getItem().getId(),
                item.getProperty().getId(),
                item.getValue().getType().ordinal(),
                gson.toJson(item.getValue()),
                item.getRank().ordinal()
        );
    }

    @Override
    public Iterable<WikidataStatement> get(DaoFilter daoFilter) throws DaoException {
        return null;
    }

    @Override
    public int getCount(DaoFilter daoFilter) throws DaoException {
        return 0;
    }
    public static class Provider extends org.wikapidia.conf.Provider<WikidataDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class<WikidataDao> getType() {
            return WikidataDao.class;
        }

        @Override
        public String getPath() {
            return "dao.wikidata";
        }

        @Override
        public WikidataDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                return new WikidataDao(
                        getConfigurator().get(
                                WpDataSource.class,
                                config.getString("dataSource"))
                );
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
