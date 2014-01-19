package org.wikapidia.wikidata;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.typesafe.config.Config;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.collections.IteratorUtils;
import org.jooq.*;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.UniversalPageDao;
import org.wikapidia.core.dao.sql.AbstractSqlDao;
import org.wikapidia.core.dao.sql.FastLoader;
import org.wikapidia.core.dao.sql.SimpleSqlDaoIterable;
import org.wikapidia.core.dao.sql.WpDataSource;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.parser.WpParseException;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static org.wikapidia.core.jooq.tables.WikidataEntityAliases.*;
import static org.wikapidia.core.jooq.tables.WikidataEntityLabels.*;
import static org.wikapidia.core.jooq.tables.WikidataEntityDescriptions.*;
import static org.wikapidia.core.jooq.tables.WikidataStatement.*;

/**
 * @author Shilad Sen
 */
public class WikidataSqlDao extends AbstractSqlDao<WikidataStatement> implements WikidataDao {
    private static Language FALLBACK_LANGUAGE = Language.getByLangCode("en");

    private static TableField[] FIELDS = new TableField[] {
            WIKIDATA_STATEMENT.ID,
            WIKIDATA_STATEMENT.ENTITY_TYPE,
            WIKIDATA_STATEMENT.ENTITY_ID,
            WIKIDATA_STATEMENT.PROP_ID,
            WIKIDATA_STATEMENT.VAL_TYPE,
            WIKIDATA_STATEMENT.VAL_STR,
            WIKIDATA_STATEMENT.RANK
    };
    private final LocalPageDao lpDao;
    private final UniversalPageDao upDao;
    private Gson gson = new Gson();

    private FastLoader labelLoader = null;
    private FastLoader descLoader = null;
    private FastLoader aliasLoader = null;
    private Map<Integer, WikidataEntity> properties;

    /**
     * @param dataSource      Data source for jdbc connections
     * @throws org.wikapidia.core.dao.DaoException
     */
    public WikidataSqlDao(WpDataSource dataSource, LocalPageDao lpDao, UniversalPageDao upDao) throws DaoException {
        super(dataSource, FIELDS, "/db/wikidata");
        this.lpDao = lpDao;
        this.upDao = upDao;
    }

    @Override
    public WikidataEntity getProperty(int id) throws DaoException {
        Map<Integer, WikidataEntity> properties = getProperties();  // should be cached!
        if (properties == null || properties.size() == 0) {
            return getEntityWithoutCache(WikidataEntity.Type.PROPERTY, id);
        } else {
            return properties.get(id);
        }
    }

    @Override
    public WikidataEntity getItem(int id) throws DaoException {
        return getEntityWithoutCache(WikidataEntity.Type.ITEM, id);
    }

    @Override
    public synchronized Map<Integer, WikidataEntity> getProperties() throws DaoException {
        if (properties != null) {
            return properties;
        }
        if (cache != null) {
            properties = (Map<Integer, WikidataEntity>) cache.get("wikidata-properties", WikidataStatement.class);
        }
        if (properties == null || properties.size() == 0) {
            properties = new ConcurrentHashMap<Integer, WikidataEntity>();
            LOG.info("creating wikidata properties cache. This only happens once...");
            DSLContext context = getJooq();
            try {
                Result<Record1<Integer>> result = context
                        .select(Tables.WIKIDATA_ENTITY_LABELS.ENTITY_ID)
                        .from(Tables.WIKIDATA_ENTITY_LABELS)
                        .where(Tables.WIKIDATA_ENTITY_LABELS.ENTITY_TYPE.eq("" + WikidataEntity.Type.PROPERTY.code))
                        .fetch();
                TIntSet propIds = new TIntHashSet();
                for (Record1<Integer> record : result) {
                    propIds.add(record.value1());
                }
                for (int id : propIds.toArray()) {
                    properties.put(id, getEntityWithoutCache(WikidataEntity.Type.PROPERTY, id));
                }
            } finally {
                freeJooq(context);
            }
        }
        LOG.info("loaded properties with size " + ((properties == null) ? 0 : properties.size()));
        return properties;
    }

    @Override
    public List<WikidataStatement> getStatements(LocalPage page) throws DaoException {
        int conceptId = upDao.getUnivPageId(page, 1);
        if (conceptId < 0) {
            return new ArrayList<WikidataStatement>();
        }

        WikidataFilter filter = new WikidataFilter.Builder()
                .withEntityType(WikidataEntity.Type.ITEM)
                .withEntityId(conceptId)
                .build();
        return IteratorUtils.toList(get(filter).iterator());
    }

    @Override
    public Map<String, List<LocalWikidataStatement>> getLocalStatements(LocalPage page) throws DaoException {
        int conceptId = upDao.getUnivPageId(page, 1);
        if (conceptId < 0) {
            return new HashMap<String, List<LocalWikidataStatement>>();
        }
        return getLocalStatements(page.getLanguage(), WikidataEntity.Type.ITEM, conceptId);
    }

    private WikidataEntity getEntityWithoutCache(WikidataEntity.Type type, int id) throws DaoException {
        WikidataEntity entity = new WikidataEntity(type, id);
        DSLContext jooq = getJooq();
        try {
            Result<Record2<Short, String>> result = jooq
                    .select(Tables.WIKIDATA_ENTITY_LABELS.LANG_ID, Tables.WIKIDATA_ENTITY_LABELS.LABEL)
                    .from(Tables.WIKIDATA_ENTITY_LABELS)
                    .where(Tables.WIKIDATA_ENTITY_LABELS.ENTITY_TYPE.eq(type.code + ""))
                    .and(Tables.WIKIDATA_ENTITY_LABELS.ENTITY_ID.eq(id))
                    .fetch();
            for (Record2<Short, String> record : result) {
                entity.getLabels().put(Language.getById(record.value1()), record.value2());
            }
            Result<Record2<Short, String>> result2 = jooq
                    .select(Tables.WIKIDATA_ENTITY_DESCRIPTIONS.LANG_ID, Tables.WIKIDATA_ENTITY_DESCRIPTIONS.DESCRIPTION)
                    .from(Tables.WIKIDATA_ENTITY_DESCRIPTIONS)
                    .where(Tables.WIKIDATA_ENTITY_DESCRIPTIONS.ENTITY_TYPE.eq(type.code + ""))
                    .and(Tables.WIKIDATA_ENTITY_DESCRIPTIONS.ENTITY_ID.eq(id))
                    .fetch();
            for (Record2<Short, String> record : result2) {
                entity.getDescriptions().put(Language.getById(record.value1()), record.value2());
            }
            Result<Record2<Short, String>> result3 = jooq
                    .select(Tables.WIKIDATA_ENTITY_ALIASES.LANG_ID, Tables.WIKIDATA_ENTITY_ALIASES.ALIAS)
                    .from(Tables.WIKIDATA_ENTITY_ALIASES)
                    .where(Tables.WIKIDATA_ENTITY_ALIASES.ENTITY_TYPE.eq(type.code + ""))
                    .and(Tables.WIKIDATA_ENTITY_ALIASES.ENTITY_ID.eq(id))
                    .fetch();
            Map<Language, List<String>> aliases = entity.getAliases();
            for (Record2<Short, String> record : result3) {
                Language lang = Language.getById(record.value1());
                if (!aliases.containsKey(lang)) {
                    aliases.put(lang, new ArrayList<String>());
                }
                aliases.get(lang).add(record.value2());
            }
            WikidataFilter filter = new WikidataFilter.Builder()
                    .withEntityType(type)
                    .withEntityId(id)
                    .build();
            entity.getStatements().addAll(IteratorUtils.toList(get(filter).iterator()));
            return entity;
        } finally {
            freeJooq(jooq);
        }
    }

    @Override
    public void beginLoad() throws DaoException {
        super.beginLoad();
        if (labelLoader == null) {
            labelLoader = new FastLoader(wpDs, new TableField[] {
                                        WIKIDATA_ENTITY_LABELS.ENTITY_TYPE,
                                        WIKIDATA_ENTITY_LABELS.ENTITY_ID,
                                        WIKIDATA_ENTITY_LABELS.LANG_ID,
                                        WIKIDATA_ENTITY_LABELS.LABEL,
                                    });
        }
        if (descLoader == null) {
            descLoader = new FastLoader(wpDs, new TableField[] {
                                        WIKIDATA_ENTITY_DESCRIPTIONS.ENTITY_TYPE,
                                        WIKIDATA_ENTITY_DESCRIPTIONS.ENTITY_ID,
                                        WIKIDATA_ENTITY_DESCRIPTIONS.LANG_ID,
                                        WIKIDATA_ENTITY_DESCRIPTIONS.DESCRIPTION,
                                    });
        }
        if (aliasLoader == null) {
            aliasLoader = new FastLoader(wpDs, new TableField[] {
                                        WIKIDATA_ENTITY_ALIASES.ENTITY_TYPE,
                                        WIKIDATA_ENTITY_ALIASES.ENTITY_ID,
                                        WIKIDATA_ENTITY_ALIASES.LANG_ID,
                                        WIKIDATA_ENTITY_ALIASES.ALIAS
                                    });
        }
        properties = new HashMap<Integer, WikidataEntity>();
    }

    @Override
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
        if (entity.getType() == WikidataEntity.Type.PROPERTY) {
            synchronized (properties) {
                properties.put(entity.getId(), entity);
            }
        }
    }

    @Override
    public void save(WikidataStatement item) throws DaoException {
        insert(
                item.getId(),
                item.getItem().getType().code,
                item.getItem().getId(),
                item.getProperty().getId(),
                item.getValue().getType().toString().toLowerCase(),
                gson.toJson(item.getValue().getJsonValue()),
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
        if (cache != null) {
            cache.put("wikidata-properties", properties);
        }
    }

    @Override
    public Map<String, List<LocalWikidataStatement>> getLocalStatements(Language lang, WikidataEntity.Type type, int id) throws DaoException {
        WikidataFilter filter = new WikidataFilter.Builder()
                .withEntityType(type)
                .withEntityId(id)
                .build();
        Map<String, List<LocalWikidataStatement>> local = new HashMap<String, List<LocalWikidataStatement>>();
        for (WikidataStatement st : get(filter)) {
            LocalWikidataStatement lws = getLocalStatement(lang, st);
            if (!local.containsKey(lws.getProperty())) {
                local.put(lws.getProperty(), new ArrayList<LocalWikidataStatement>());
            }
            local.get(lws.getProperty()).add(lws);
        }
        return local;
    }

    @Override
    public LocalWikidataStatement getLocalStatement(Language language, WikidataStatement statement) throws DaoException {
        String item = getLocalName(language, statement.getItem().getType(), statement.getItem().getId());
        String prop = getLocalName(language, statement.getProperty().getType(), statement.getProperty().getId());
        String value = null;
        WikidataValue wdv = statement.getValue();
        if (wdv.getType() == WikidataValue.Type.ITEM) {
            value = getLocalName(language, WikidataEntity.Type.ITEM, wdv.getItemValue());
        } else if (wdv == null) {
            value = "unknown";
        } else {
            value = wdv.getValue().toString();
        }
        String full = item + " " + prop + " " + value;
        return new LocalWikidataStatement(language, statement, full, item, prop, value);
    }

    public String getLocalName(Language language, WikidataEntity.Type type, int id) throws DaoException {
        if (type == WikidataEntity.Type.PROPERTY) {
            WikidataEntity prop = getProperty(id);  // should be cached, fast
            if (prop.getLabels().isEmpty()) {
                LOG.warning("no labels for property " + id);
                return "unknown";
            }
            if (prop.getLabels().containsKey(language)) {
                return prop.getLabels().get(language);
            } else if (prop.getLabels().containsKey(FALLBACK_LANGUAGE)) {
                return prop.getLabels().get(FALLBACK_LANGUAGE);
            } else {
                return prop.getLabels().values().iterator().next();
            }
        } else if (type == WikidataEntity.Type.ITEM) {
            DSLContext jooq = getJooq();
            try {
                for (Language l : Arrays.asList(language, FALLBACK_LANGUAGE)) {
                    Result<Record1<String>> result = jooq.select(Tables.WIKIDATA_ENTITY_LABELS.LABEL)
                            .from(Tables.WIKIDATA_ENTITY_LABELS)
                            .where(Tables.WIKIDATA_ENTITY_LABELS.ENTITY_TYPE.eq(""+type.code))
                            .and(WIKIDATA_ENTITY_LABELS.ENTITY_ID.eq(id))
                            .and(WIKIDATA_ENTITY_LABELS.LANG_ID.eq(l.getId()))
                            .fetch();
                    if (result.size() >= 1) {
                        return result.get(0).value1();
                    }
                }
                Result<Record1<String>> result = jooq.select(Tables.WIKIDATA_ENTITY_LABELS.LABEL)
                        .from(Tables.WIKIDATA_ENTITY_LABELS)
                        .where(Tables.WIKIDATA_ENTITY_LABELS.ENTITY_TYPE.eq(""+type.code))
                        .and(WIKIDATA_ENTITY_LABELS.ENTITY_ID.eq(id))
                        .limit(1)
                        .fetch();
                if (result.size() >= 1) {
                    return result.get(0).value1();
                } else {
                    LOG.warning("no labels for item " + id);
                    return "unknown";
                }
            } finally {
                freeJooq(jooq);
            }
        } else {
            throw new IllegalArgumentException("Unknown entity type: " + type);
        }
    }

    @Override
    public Iterable<WikidataStatement> get(WikidataFilter filter) throws DaoException {
        List<Condition> conditions = new ArrayList<Condition>();
        if (filter.getLangIds() != null) {
            throw new UnsupportedOperationException("Filter doesn't support lang ids yet");
        }
        if (filter.getEntityTypes() != null) {
            conditions.add(WIKIDATA_STATEMENT.ENTITY_TYPE.in(filter.getEntityTypeCodes()));
        }
        if (filter.getEntityIds() != null) {
            conditions.add(WIKIDATA_STATEMENT.ENTITY_ID.in(filter.getEntityIds()));
        }
        if (filter.getRanks() != null) {
            conditions.add(WIKIDATA_STATEMENT.RANK.in(filter.getRankOrdinals()));
        }
        DSLContext jooq = getJooq();
        try {
            Cursor<Record> result = jooq.select().
                    from(Tables.WIKIDATA_STATEMENT).
                    where(conditions).
                    fetchLazy(getFetchSize());

            return new SimpleSqlDaoIterable<WikidataStatement>(result, jooq) {
                @Override
                public WikidataStatement transform(Record r) {
                    try {
                        return buildStatement(r);
                    } catch (DaoException e) {
                        LOG.log(Level.WARNING, e.getMessage(), e);
                        return null;
                    }
                }
            };
        } finally {
            freeJooq(jooq);
        }
    }

    @Override
    public Iterable<WikidataStatement> get(DaoFilter daoFilter) throws DaoException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getCount(DaoFilter daoFilter) throws DaoException {
        throw new UnsupportedOperationException();
    }

    protected WikidataStatement buildStatement(Record record) throws DaoException {
        if (record == null) {
            return null;
        }
        WikidataEntity item = new WikidataEntity(
                WikidataEntity.Type.getByCode(record.getValue(Tables.WIKIDATA_STATEMENT.ENTITY_TYPE).charAt(0)),
                record.getValue(Tables.WIKIDATA_STATEMENT.ENTITY_ID)
        );
        WikidataEntity prop = getProperty(record.getValue(Tables.WIKIDATA_STATEMENT.PROP_ID));
        Short rankOrdinal = record.getValue(Tables.WIKIDATA_STATEMENT.RANK);

        JsonElement json = new JsonParser().parse(record.getValue(Tables.WIKIDATA_STATEMENT.VAL_STR));
        WikidataValue val;
        try {
            val = JsonUtils.jsonToValue( record.getValue(Tables.WIKIDATA_STATEMENT.VAL_TYPE), json);
        } catch (WpParseException e) {
            throw new DaoException(e);
        }

        WikidataStatement stmt = new WikidataStatement(
                record.getValue(Tables.WIKIDATA_STATEMENT.ID),
                item,
                prop,
                val,
                WikidataStatement.Rank.values()[rankOrdinal]
        );
        return stmt;
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
                WikidataSqlDao dao = new WikidataSqlDao(
                        getConfigurator().get(
                                WpDataSource.class,
                                config.getString("dataSource")),
                        getConfigurator().get(LocalPageDao.class),
                        getConfigurator().get(UniversalPageDao.class, "purewikidata")
                );

                String cachePath = getConfig().get().getString("dao.sqlCachePath");
                File cacheDir = new File(cachePath);
                if (!cacheDir.isDirectory()) {
                    cacheDir.mkdirs();
                }
                dao.useCache(cacheDir);
                return dao;
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
