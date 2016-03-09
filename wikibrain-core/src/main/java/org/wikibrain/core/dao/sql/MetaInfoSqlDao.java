package org.wikibrain.core.dao.sql;

import com.typesafe.config.Config;
import org.jooq.*;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.jooq.Tables;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.MetaInfo;
import org.wikibrain.utils.JvmUtils;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Shilad Sen
 */
public class MetaInfoSqlDao extends AbstractSqlDao<MetaInfo> implements MetaInfoDao {
    private static final Logger LOG = LoggerFactory.getLogger(MetaInfoSqlDao.class);

    private static final Object NULL_KEY = new Object();

    private static final int COUNTS_PER_FLUSH = 5000;

    private final ConcurrentHashMap<Class, Map<Language, MetaInfo>> counters =
            new ConcurrentHashMap<Class, Map<Language, MetaInfo>>();

    public MetaInfoSqlDao(WpDataSource dataSource) throws DaoException {
        super(dataSource, null, "/db/meta-info");
    }

    public boolean tableExists() throws DaoException {
        DSLContext context = getJooq();
        try {
            return JooqUtils.tableExists(context, Tables.META_INFO);
        } finally {
            freeJooq(context);
        }
    }

    public boolean tableExists(DSLContext context) {
        return JooqUtils.tableExists(context, Tables.META_INFO);
    }

    @Override
    public void clear(Class component) throws DaoException {
        if (!tableExists()) {
            return;
        }
        DSLContext context = getJooq();
        try {
            if (!context.meta().getTables().contains(Tables.META_INFO)) {
                return;
            }
            context.delete(Tables.META_INFO)
                    .where(Tables.META_INFO.COMPONENT.eq(component.getSimpleName()))
                    .execute();
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public void clear(Class component, Language lang) throws DaoException {
        if (!tableExists()) {
            return;
        }
        DSLContext context = getJooq();
        try {
            context.delete(Tables.META_INFO)
                    .where(Tables.META_INFO.COMPONENT.eq(component.getSimpleName()))
                    .and(Tables.META_INFO.LANG_ID.eq(lang.getId()))
                    .execute();
            JooqUtils.commit(context);
        } catch (RuntimeException e) {
            JooqUtils.rollbackQuietly(context);
            throw e;
        } catch (DaoException e) {
            JooqUtils.rollbackQuietly(context);
            throw e;
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public int incrementRecords(Class component, int n) throws DaoException {
        return incrementRecords(component, null, n);
    }

    @Override
    public int incrementRecords(Class component, Language lang, int n) throws DaoException {
        MetaInfo info = getInfo(component, lang);
        n = info.incrementNumRecords(n);
        maybeFlush(info);
        return n;
    }

    @Override
    public int incrementRecords(Class component) throws DaoException {
        return incrementRecords(component, 1);
    }

    @Override
    public int incrementRecords(Class component, Language lang) throws DaoException {
        return incrementRecords(component, lang, 1);
    }

    @Override
    public int incrementErrors(Class component) throws DaoException {
        return incrementErrors(component, null);
    }

    @Override
    public int incrementErrorsQuietly(Class component){
        try {
            return incrementErrors(component);
        } catch (DaoException e) {
            LOG.warn("incrementErrors failed:", e);
            return 0;
        }
    }

    @Override
    public int incrementErrorsQuietly(Class component, Language lang){
        try {
            return incrementErrors(component, lang);
        } catch (DaoException e) {
            LOG.warn("incrementErrors failed:", e);
            return 0;
        }
    }

    @Override
    public int incrementErrors(Class component, Language lang) throws DaoException {
        MetaInfo info = getInfo(component, lang);
        int n = info.incrementNumErrors();
        maybeFlush(info);
        return n;
    }

    @Override
    public void sync(Class component) throws DaoException {
        if (counters.containsKey(component)) {
            for (MetaInfo mi : counters.get(component).values()) {
                flush(mi);
            }
        }
    }

    @Override
    public void sync(Class component, Language lang) throws DaoException {
        MetaInfo info = getInfo(component, lang);
        flush(info);
    }


    @Override
    public void sync() throws DaoException {
        for (Class klass : ((Map<Class, Map<Language, MetaInfo>>)counters).keySet()) {
            for (MetaInfo mi : counters.get(klass).values()) {
                flush(mi);
            }
        }
    }

    @Override
    public MetaInfo getInfo(Class component) throws DaoException {
        sync(component);
        MetaInfo accumulated = new MetaInfo(component);
        DSLContext context = getJooq();
        try {
            if (!tableExists(context)) {
                return accumulated;
            }

            Result<Record3<Integer, Integer, Timestamp>> records =
                    context.select(Tables.META_INFO.NUM_RECORDS, Tables.META_INFO.NUM_ERRORS, Tables.META_INFO.LAST_UPDATED)
                            .from(Tables.META_INFO)
                            .where(Tables.META_INFO.COMPONENT.eq(component.getSimpleName()))
                            .fetch();

            for (Record3<Integer, Integer, Timestamp> record : records) {
                MetaInfo info = new MetaInfo(component, null, record.value1(), record.value2(), record.value3());
                accumulated.merge(info);
            }
            return accumulated;
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public boolean isLoaded(Class component) throws DaoException {
        MetaInfo info = getInfo(component);
        return (info != null && info.getNumRecords() > 0);
    }


    @Override
    public LanguageSet getLoadedLanguages() throws DaoException {
        return getLoadedLanguages(LocalPage.class);
    }

    @Override
    public LanguageSet getLoadedLanguages(Class component) throws DaoException {
        sync(component);
        DSLContext context = getJooq();
        try {
            if (!tableExists(context)) {
                return new LanguageSet();
            }
            Set<Language> langs = new HashSet<Language>();
            Result<Record1<Short>> records =
                    context.select(Tables.META_INFO.LANG_ID)
                            .from(Tables.META_INFO)
                            .where(Tables.META_INFO.COMPONENT.eq(component.getSimpleName()))
                            .and(Tables.META_INFO.LANG_ID.isNotNull())
                            .fetch();

            for (Record1<Short> record : records) {
                langs.add(Language.getById(record.value1()));
            }
            return new LanguageSet(langs);
        }
        catch (Exception e) {
            throw new DaoException("Error when getting loaded languages, check if the database exists / has been initialized\n" + e.toString());
        }
        finally {
            freeJooq(context);
        }
    }

    @Override
    public MetaInfo getInfo(Class component, Language lang) throws DaoException {
        counters.putIfAbsent(component, new ConcurrentHashMap<Language, MetaInfo>());
        Map<Language, MetaInfo> langInfos = counters.get(component);
        if (langInfos == null) {
            throw new IllegalStateException();
        }
        Object langKey = (lang == null ? NULL_KEY : lang);
        MetaInfo info = langInfos.get(langKey);
        if (info == null) {
            synchronized (langInfos) {
                if (langInfos.containsKey(langKey)) {
                    info = langInfos.get(langKey);
                } else {
                    DSLContext context = getJooq();
                    try {
                        if (!tableExists(context)) {
                            info = new MetaInfo(component, lang);
                        } else {
                            Condition langCondition = (lang == null)
                                    ? Tables.META_INFO.LANG_ID.isNull()
                                    : Tables.META_INFO.LANG_ID.eq(lang.getId());

                            Record3<Integer, Integer, Timestamp> record =
                                    context.select(Tables.META_INFO.NUM_RECORDS, Tables.META_INFO.NUM_ERRORS, Tables.META_INFO.LAST_UPDATED)
                                            .from(Tables.META_INFO)
                                            .where(Tables.META_INFO.COMPONENT.eq(component.getSimpleName()))
                                            .and(langCondition)
                                            .fetchOne();
                            if (record == null) {
                                info = new MetaInfo(component, lang);
                            } else {
                                info = new MetaInfo(component, lang, record.value1(), record.value2(), record.value3());
                            }
                        }
                    } finally {
                        freeJooq(context);
                    }
                    ((Map)langInfos).put(langKey, info);
                }
            }
        }
        return info;
    }

    @Override
    public Map<String, List<MetaInfo>> getAllInfo() throws DaoException {
        DSLContext context = getJooq();
        try {
            Map<String, List<MetaInfo>> components = new HashMap<String, List<MetaInfo>>();
            if (!tableExists(context)) {
                return components;
            }
            Result<Record> result = context
                    .select()
                    .from(Tables.META_INFO)
                    .fetch();

            for (Record record : result) {
                String klass = record.getValue(Tables.META_INFO.COMPONENT);
                if (!components.containsKey(klass)) {
                    components.put(klass, new ArrayList<MetaInfo>());
                }
                Short langId = record.getValue(Tables.META_INFO.LANG_ID);
                components.get(klass).add(
                        new MetaInfo(null,
                                (langId == null) ? null : Language.getById(langId),
                                record.getValue(Tables.META_INFO.ID),
                                record.getValue(Tables.META_INFO.NUM_RECORDS),
                                record.getValue(Tables.META_INFO.NUM_ERRORS),
                                record.getValue(Tables.META_INFO.LAST_UPDATED)
                        ));
            }
            return components;
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public Map<String, MetaInfo> getAllCummulativeInfo() throws DaoException {
        sync();
        DSLContext context = getJooq();
        try {
            Map<String, MetaInfo> components = new HashMap<String, MetaInfo>();
            if (!tableExists(context)) {
                return components;
            }

            Result<Record> result = context
                    .select()
                    .from(Tables.META_INFO)
                    .fetch();


            for (Record record : result) {
                String className = record.getValue(Tables.META_INFO.COMPONENT);
                Class klass = JvmUtils.classForShortName(className);
                if (klass == null) {
                    throw new DaoException("No class found for short name " + className);
                }
                if (!components.containsKey(className)) {
                    components.put(className, new MetaInfo(klass));
                }
                Short langId = record.getValue(Tables.META_INFO.LANG_ID);
                components.get(className).merge(
                        new MetaInfo(klass,
                                (langId == null) ? null : Language.getById(langId),
                                record.getValue(Tables.META_INFO.ID),
                                record.getValue(Tables.META_INFO.NUM_RECORDS),
                                record.getValue(Tables.META_INFO.NUM_ERRORS),
                                record.getValue(Tables.META_INFO.LAST_UPDATED)
                        ));
            }
            return components;
        } finally {
            freeJooq(context);
        }
    }

    private void maybeFlush(MetaInfo info) throws DaoException {
        if (info.numNotWritten() > COUNTS_PER_FLUSH) {
            synchronized (info) {
                if (info.numNotWritten() > COUNTS_PER_FLUSH) {
                    flush(info);
                }
            }
        }
    }

    private void flush(MetaInfo info) throws DaoException {
        synchronized (info) {
            DSLContext context = getJooq();
            try {
                Condition langCondition = (info.getLanguage() == null)
                        ? Tables.META_INFO.LANG_ID.isNull()
                        : Tables.META_INFO.LANG_ID.eq(info.getLanguage().getId());

                int n = context.update(Tables.META_INFO)
                        .set(Tables.META_INFO.NUM_ERRORS, info.getNumErrors())
                        .set(Tables.META_INFO.NUM_RECORDS, info.getNumRecords())
                        .set(Tables.META_INFO.LAST_UPDATED, new Timestamp(info.getLastUpdated().getTime()))
                        .where(Tables.META_INFO.COMPONENT.eq(info.getComponent().getSimpleName()))
                        .and(langCondition)
                        .execute();
                if (n == 0) {
                    Short langId = (info.getLanguage() == null) ? null : info.getLanguage().getId();
                    context.insertInto(Tables.META_INFO,
                            Tables.META_INFO.COMPONENT, Tables.META_INFO.LANG_ID,
                            Tables.META_INFO.NUM_RECORDS, Tables.META_INFO.NUM_ERRORS,
                            Tables.META_INFO.LAST_UPDATED)
                            .values(info.getComponent().getSimpleName(), langId,
                                    info.getNumRecords(), info.getNumErrors(),
                                    new Timestamp(info.getLastUpdated().getTime()))
                            .execute();
                }
                info.markAsWritten();
                JooqUtils.commit(context);
            } catch (RuntimeException e) {
                JooqUtils.rollbackQuietly(context);
                throw e;
            } finally {
                freeJooq(context);
            }
        }
    }

    @Override
    public void endLoad() throws DaoException {
        sync();
        super.endLoad();
        wpDs.optimize(Tables.META_INFO);
    }

    /**
     * Unimplemented methods
     * @param item the item to be saved
     * @throws DaoException
     */
    public void save(MetaInfo item) throws DaoException { throw new UnsupportedOperationException(); }
    public Iterable<MetaInfo> get(DaoFilter daoFilter) throws DaoException { throw new UnsupportedOperationException(); }
    public int getCount(DaoFilter daoFilter) throws DaoException { throw new UnsupportedOperationException(); }


    public static class Provider extends org.wikibrain.conf.Provider<MetaInfoDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class<MetaInfoDao> getType() {
            return MetaInfoDao.class;
        }

        @Override
        public String getPath() {
            return "dao.metaInfo";
        }

        @Override
        public MetaInfoDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                return new MetaInfoSqlDao(
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
