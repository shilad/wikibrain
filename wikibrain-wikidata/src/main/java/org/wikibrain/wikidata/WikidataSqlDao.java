package org.wikibrain.wikidata;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.typesafe.config.Config;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.collections.IteratorUtils;
import org.jooq.*;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.dao.sql.AbstractSqlDao;
import org.wikibrain.core.dao.sql.FastLoader;
import org.wikibrain.core.dao.sql.SimpleSqlDaoIterable;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.jooq.Tables;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.parser.WpParseException;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.wikibrain.core.jooq.tables.WikidataEntityAliases.*;
import static org.wikibrain.core.jooq.tables.WikidataEntityLabels.*;
import static org.wikibrain.core.jooq.tables.WikidataEntityDescriptions.*;
import static org.wikibrain.core.jooq.tables.WikidataStatement.*;

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
    private WikidataParser parser = new WikidataParser();

    /**
     * @param dataSource      Data source for jdbc connections
     * @throws org.wikibrain.core.dao.DaoException
     */
    public WikidataSqlDao(WpDataSource dataSource, LocalPageDao lpDao, UniversalPageDao upDao) throws DaoException {
        super(dataSource, FIELDS, "/db/wikidata");
        this.lpDao = lpDao;
        this.upDao = upDao;
    }

    @Override
    public WikidataEntity getProperty(Language language, String name) throws DaoException {
        name = name.toLowerCase();
        for (WikidataEntity entity : getProperties().values()) {
            String ename = entity.getLabels().get(language);
            if (ename != null && ename.toLowerCase().equals(name)) {
                return entity;
            }
        }
        return null;
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
            properties = (Map<Integer, WikidataEntity>) cache.get("wikidata-properties", WikidataEntity.class);
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
            if (cache != null) {
                cache.put("wikidata-properties", properties);
            }
        }
        LOG.info("loaded properties with size " + ((properties == null) ? 0 : properties.size()));
        return properties;
    }

    @Override
    public Integer getItemId(LocalPage page) throws DaoException{
        return upDao.getUnivPageId(page);
    }

    @Override
    public Integer getItemId(LocalId localId) throws DaoException {
        return upDao.getUnivPageId(localId.getLanguage(), localId.getId());
    }

    @Override
    public UniversalPage getUniversalPage(int itemId) throws DaoException {
        UniversalPage uPage = upDao.getById(itemId);
        return uPage;
    }

    @Override
    public List<WikidataStatement> getStatements(LocalPage page) throws DaoException {
        int conceptId = upDao.getUnivPageId(page);
        if (conceptId < 0) {
            return new ArrayList<WikidataStatement>();
        }

        WikidataFilter filter = new WikidataFilter.Builder()
                .withEntityType(WikidataEntity.Type.ITEM)
                .withEntityId(conceptId)
                .build();
        return IteratorUtils.toList(get(filter).iterator());
    }

    public Collection<WikidataEntity> getPropertyByName(Language language, String name) throws DaoException {
        List<WikidataEntity> matches = new ArrayList<WikidataEntity>();
        Map<Integer, WikidataEntity> props = getProperties();
        for (WikidataEntity e : props.values()) {
            if (e.getAliases().containsKey(language) && e.getAliases().get(language).contains(name)) {
                matches.add(e);
            } else if (e.getLabels().containsKey(language) && e.getLabels().get(language).contains(name)) {
                matches.add(e);
            }
        }
        return matches;
    }

    public Collection<WikidataEntity> getPropertyByName(String name) throws DaoException {
        Set<WikidataEntity> matches = new HashSet<WikidataEntity>();
        Map<Integer, WikidataEntity> props = getProperties();
        for (WikidataEntity e : props.values()) {
            if (e.getLabels().values().contains(name)) {
                matches.add(e);
                continue;
            }
            if (e.getAliases().values().contains(name)) {
                matches.add(e);
                continue;
            }
        }
        return matches;
    }

    @Override
    public Map<String, List<LocalWikidataStatement>> getLocalStatements(LocalPage page) throws DaoException {
        int conceptId = getItemId(page);
        if (conceptId < 0) {
            return new HashMap<String, List<LocalWikidataStatement>>();
        }
        return getLocalStatements(getRealLang(page.getLanguage()), WikidataEntity.Type.ITEM, conceptId);
    }

    private Language getRealLang(Language lang) {
        if (lang.getLangCode().equals("simple")) {
            return Language.getByLangCode("en");
        } else {
            return lang;
        }
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
            for (WikidataStatement st : get(filter)) {
                if (st != null) {
                    entity.getStatements().add(st);
                }
            }
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
                item.getValue().getTypeName().toLowerCase(),
                encodeValue(item.getValue()),
                item.getRank().ordinal()
        );
    }

    private String encodeValue(WikidataValue value) {
        return gson.toJson(value.getJsonValue());
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

        wpDs.optimize(WIKIDATA_ENTITY_LABELS);
        wpDs.optimize(WIKIDATA_ENTITY_ALIASES);
        wpDs.optimize(WIKIDATA_ENTITY_DESCRIPTIONS);
        wpDs.optimize(WIKIDATA_STATEMENT);
    }

    @Override
    public Map<String, List<LocalWikidataStatement>> getLocalStatements(Language lang, WikidataEntity.Type type, int id) throws DaoException {
        lang = getRealLang(lang);
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
        language = getRealLang(language );
        String item = getLabel(language, statement.getItem().getType(), statement.getItem().getId());
        String prop = getLabel(language, statement.getProperty().getType(), statement.getProperty().getId());
        String value = null;
        WikidataValue wdv = statement.getValue();
        if (wdv.getType() == WikidataValue.Type.ITEM) {
            value = getLabel(language, WikidataEntity.Type.ITEM, wdv.getItemValue());
        } else if (wdv.getValue() == null) {
            value = "unknown";
        } else {
            value = wdv.getValue().toString();
        }
        String full = item + " " + prop + " " + value;
        return new LocalWikidataStatement(language, statement, full, item, prop, value);
    }

    @Override
    public String getLabel(Language language, WikidataEntity.Type type, int id) throws DaoException {
        if (type == WikidataEntity.Type.PROPERTY) {
            WikidataEntity prop = getProperty(id);  // should be cached, fast
            if (prop.getLabels().isEmpty()) {
                LOG.warn("no labels for property " + id);
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
                    LOG.warn("no labels for item " + id);
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
    public Iterable<WikidataStatement> getByValue(WikidataEntity property, WikidataValue value) throws DaoException {
        WikidataFilter filter = new WikidataFilter.Builder()
                .withPropertyId(property.getId())
                .withValue(value)
                .build();
        return get(filter);
    }

    @Override
    public Iterable<WikidataStatement> getByValue(String propertyName, WikidataValue value) throws DaoException {
        Set<Integer> propIds = new HashSet<Integer>();
        for (WikidataEntity e : getPropertyByName(propertyName)) {
            propIds.add(e.getId());
        }
        if (propIds.isEmpty()) {
            return new ArrayList<WikidataStatement>();
        }
        WikidataFilter filter = new WikidataFilter.Builder()
                .withPropertyIds(propIds)
                .withValue(value)
                .build();
        return get(filter);
    }

    @Override
    public Set<Integer> conceptsWithValue(String propertyName, WikidataValue value) throws DaoException {
        Set<Integer> concepts = new HashSet<Integer>();
        for (WikidataStatement st : getByValue(propertyName, value)) {
            if (st.getItem().getType() == WikidataEntity.Type.ITEM) {
                concepts.add(st.getItem().getId());
            }
        }
        return concepts;
    }

    @Override
    public Set<LocalId> pagesWithValue(String propertyName, WikidataValue value, Language language) throws DaoException {
        Set<LocalId> ids = new HashSet<LocalId>();
        for (int conceptId : conceptsWithValue(propertyName, value)) {
            UniversalPage up = upDao.getById(conceptId);
            if (up != null && up.isInLanguage(language)) {
                ids.add(new LocalId(language, up.getLocalId(language)));
            }
        }
        return ids;
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
        if (filter.getPropertyIds() != null) {
            conditions.add(WIKIDATA_STATEMENT.PROP_ID.in(filter.getPropertyIds()));
        }
        if (filter.getRanks() != null) {
            conditions.add(WIKIDATA_STATEMENT.RANK.in(filter.getRankOrdinals()));
        }
        if (filter.getValues() != null) {
            String type = null;
            List<String> values = new ArrayList<String>();
            for (WikidataValue value : filter.getValues()) {
                values.add(encodeValue(value));
                if (type == null) {
                    type = value.getTypeName();
                }
                if (!type.equals(value.getTypeName())) {
                    throw new IllegalArgumentException("All wikidata filter values must have the same type");
                }
            }
            conditions.add(WIKIDATA_STATEMENT.VAL_TYPE.eq(type.toLowerCase()).and(WIKIDATA_STATEMENT.VAL_STR.in(values)));
        }
        DSLContext jooq = getJooq();
        try {
//            System.err.println("EXECUTING " + jooq.select().from(Tables.WIKIDATA_STATEMENT).where(conditions).getSQL());
            Cursor<Record> result = jooq.select().
                    from(Tables.WIKIDATA_STATEMENT).
                    where(conditions).fetchLazy(getFetchSize());

            return new SimpleSqlDaoIterable<WikidataStatement>(result, jooq) {
                @Override
                public WikidataStatement transform(Record r) {
                    try {
                        return buildStatement(r);
                    } catch (DaoException e) {
                        LOG.warn(e.getMessage(), e);
                        return null;
                    }
                }
            };
        } finally {
//            freeJooq(jooq);
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
            val = parser.jsonToValue( record.getValue(Tables.WIKIDATA_STATEMENT.VAL_TYPE), json);
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

    public static class Provider extends org.wikibrain.conf.Provider<WikidataDao> {
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
                        getConfigurator().get(UniversalPageDao.class)
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
