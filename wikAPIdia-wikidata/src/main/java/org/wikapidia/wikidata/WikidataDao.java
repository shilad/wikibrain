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
import org.wikapidia.core.dao.sql.FastLoader;
import org.wikapidia.core.dao.sql.WpDataSource;
import org.wikapidia.core.lang.Language;

import java.util.List;
import java.util.Map;

import static org.wikapidia.core.jooq.tables.WikidataEntityAliases.*;
import static org.wikapidia.core.jooq.tables.WikidataEntityLabels.*;
import static org.wikapidia.core.jooq.tables.WikidataEntityDescriptions.*;
import static org.wikapidia.core.jooq.tables.WikidataStatement.*;

/**
 * @author Shilad Sen
 */
public class WikidataDao extends AbstractSqlDao<WikidataStatement> {
    private static TableField[] FIELDS = new TableField[] {
            WIKIDATA_STATEMENT.ID,
            WIKIDATA_STATEMENT.ENTITY_TYPE,
            WIKIDATA_STATEMENT.ENTITY_ID,
            WIKIDATA_STATEMENT.PROP_ID,
            WIKIDATA_STATEMENT.VAL_TYPE,
            WIKIDATA_STATEMENT.VAL_STR,
            WIKIDATA_STATEMENT.RANK
    };
    private Gson gson = new Gson();

    private FastLoader labelLoader = null;
    private FastLoader descLoader = null;
    private FastLoader aliasLoader = null;

    /**
     * @param dataSource      Data source for jdbc connections
     * @throws org.wikapidia.core.dao.DaoException
     */
    public WikidataDao(WpDataSource dataSource) throws DaoException {
        super(dataSource, FIELDS, "/db/wikidata");
    }

    @Override
    public void beginLoad() throws DaoException {
        if (labelLoader != null) {
            labelLoader = new FastLoader(wpDs, new TableField[] {
                                        WIKIDATA_ENTITY_LABELS.ENTITY_TYPE,
                                        WIKIDATA_ENTITY_LABELS.ENTITY_ID,
                                        WIKIDATA_ENTITY_LABELS.LANG_ID,
                                        WIKIDATA_ENTITY_LABELS.LABEL,
                                    });
        }
        if (descLoader != null) {
            descLoader = new FastLoader(wpDs, new TableField[] {
                                        WIKIDATA_ENTITY_DESCRIPTIONS.ENTITY_TYPE,
                                        WIKIDATA_ENTITY_DESCRIPTIONS.ENTITY_ID,
                                        WIKIDATA_ENTITY_DESCRIPTIONS.LANG_ID,
                                        WIKIDATA_ENTITY_DESCRIPTIONS.DESCRIPTION,
                                    });
        }
        if (aliasLoader != null) {
            aliasLoader = new FastLoader(wpDs, new TableField[] {
                                        WIKIDATA_ENTITY_ALIASES.ENTITY_TYPE,
                                        WIKIDATA_ENTITY_ALIASES.ENTITY_ID,
                                        WIKIDATA_ENTITY_ALIASES.LANG_ID,
                                        WIKIDATA_ENTITY_ALIASES.ALIAS
                                    });
        }
        super.beginLoad();
    }

    public void save(WikidataEntity entity) throws DaoException {
        for (Map.Entry<Language, String> entry : entity.getLabels().entrySet()) {
            labelLoader.load(entity.getType().code, entity.getId(), entry.getKey().getId(), entry.getValue());
        }
        for (Map.Entry<Language, String> entry : entity.getDescriptions().entrySet()) {
            descLoader.load(entity.getType().code, entity.getId(), entry.getKey().getId(), entry.getValue());
        }
        for (Map.Entry<Language, List<String>> entry : entity.getAliases().entrySet()) {
            for (String alias : entry.getValue()) {
                aliasLoader.load(entity.getType().code, entity.getId(), entry.getKey().getId(), alias);
            }
        }
        for (WikidataStatement stmt : entity.getStatements()) {
            save(stmt);
        }
    }

    @Override
    public void save(WikidataStatement item) throws DaoException {
        insert(
                item.getId(),
                item.getItem().getType().code,
                item.getItem().getId(),
                item.getProperty().getId(),
                item.getValue().getType().ordinal(),
                gson.toJson(item.getValue()),
                item.getRank().ordinal()
        );
    }

    @Override
    public void endLoad() throws DaoException {
        if (labelLoader != null) labelLoader.endLoad();
        if (descLoader != null) descLoader.endLoad();
        if (aliasLoader != null) aliasLoader.endLoad();
        labelLoader = null;
        descLoader = null;
        aliasLoader = null;

        super.endLoad();
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
