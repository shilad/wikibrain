package org.wikapidia.core.dao.sql;

import com.typesafe.config.Config;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.MetaInfoDao;
import org.wikapidia.core.dao.RawPageDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.MetaInfo;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class MetaInfoSqlDao extends AbstractSqlDao<MetaInfo> implements MetaInfoDao {
    private static final Logger LOG = Logger.getLogger(MetaInfoSqlDao.class.getName());

    private static final Object NULL_KEY = new Object();

    private static final int COUNTS_PER_FLUSH = 5000;

    private final Map<Class, Map<Language, MetaInfo>> counters =
            new ConcurrentHashMap<Class, Map<Language, MetaInfo>>();

    public MetaInfoSqlDao(DataSource dataSource) throws DaoException {
        super(dataSource, null, "/db/meta-info");
    }

    public boolean tableExists() throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            return (context.meta().getTables().contains(Tables.META_INFO));
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            if (conn != null) {
                quietlyCloseConn(conn);
            }
        }
    }

    @Override
    public void clear(Class component) throws DaoException {
        if (!tableExists()) {
            return;
        }
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            if (!context.meta().getTables().contains(Tables.META_INFO)) {
                return;
            }
            context.delete(Tables.META_INFO)
                    .where(Tables.META_INFO.COMPONENT.eq(component.getSimpleName()))
                    .execute();
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            if (conn != null) {
                quietlyCloseConn(conn);
            }
        }
    }

    @Override
    public void clear(Class component, Language lang) throws DaoException {
        if (!tableExists()) {
            return;
        }
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            context.delete(Tables.META_INFO)
                    .where(Tables.META_INFO.COMPONENT.eq(component.getSimpleName()))
                    .and(Tables.META_INFO.LANG_ID.eq(lang.getId()))
                    .execute();
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            if (conn != null) {
                quietlyCloseConn(conn);
            }
        }
    }

    @Override
    public int incrementRecords(Class component) throws DaoException {
        return incrementRecords(component, null);
    }

    @Override
    public int incrementRecords(Class component, Language lang) throws DaoException {
        MetaInfo info = getInfo(component, lang);
        int n = info.incrementNumRecords();
        maybeFlush(info);
        return n;
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
            LOG.log(Level.WARNING, "incrementErrors failed:", e);
            return 0;
        }
    }

    @Override
    public int incrementErrorsQuietly(Class component, Language lang){
        try {
            return incrementErrors(component, lang);
        } catch (DaoException e) {
            LOG.log(Level.WARNING, "incrementErrors failed:", e);
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
        for (Class klass : counters.keySet()) {
            for (MetaInfo mi : counters.get(klass).values()) {
                flush(mi);
            }
        }
    }

    @Override
    public MetaInfo getInfo(Class component) throws DaoException {
        sync(component);
        MetaInfo accumulated = new MetaInfo(component);
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);

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
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            if (conn != null) {
                quietlyCloseConn(conn);
            }
        }
    }


    @Override
    public LanguageSet getLoadedLanguages() throws DaoException {
        return getLoadedLanguages(LocalPage.class);
    }

    @Override
    public LanguageSet getLoadedLanguages(Class component) throws DaoException {
        sync(component);
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);

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
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            if (conn != null) {
                quietlyCloseConn(conn);
            }
        }
    }

    @Override
    public MetaInfo getInfo(Class component, Language lang) throws DaoException {
        Map<Language, MetaInfo> langInfos = counters.get(component);
        if (langInfos == null) {
            synchronized (counters) {
                if (!counters.containsKey(component)) {
                    langInfos = new ConcurrentHashMap<Language, MetaInfo>();
                    counters.put(component, langInfos);
                } else {
                    langInfos = counters.get(component);
                }
            }
        }
        Object langKey = (lang == null ? NULL_KEY : lang);
        MetaInfo info = langInfos.get(langKey);
        if (info == null) {
            synchronized (langInfos) {
                if (langInfos.containsKey(langKey)) {
                    info = langInfos.get(langKey);
                } else {
                    Connection conn = null;
                    try {
                        conn = ds.getConnection();
                        DSLContext context = DSL.using(conn, dialect);

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
                    } catch (SQLException e) {
                        throw new DaoException(e);
                    } finally {
                        if (conn != null) {
                            quietlyCloseConn(conn);
                        }
                    }
                    ((Map)langInfos).put(langKey, info);
                }
            }
        }
        return info;
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
            Connection conn = null;
            try {
                conn = ds.getConnection();
                DSLContext context = DSL.using(conn, dialect);

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
                    context.insertInto(Tables.META_INFO)
                            .values(null, info.getComponent().getSimpleName(), langId,
                                    info.getNumRecords(), info.getNumErrors(),
                                    info.getLastUpdated(), null)
                            .execute();
                }
                info.markAsWritten();
            } catch (SQLException e) {
                throw new DaoException(e);
            } finally {
                if (conn != null) {
                    quietlyCloseConn(conn);
                }
            }
        }
    }

    @Override
    public void endLoad() throws DaoException {
        sync();
        super.endLoad();
    }

    /**
     * Unimplemented methods
     * @param item the item to be saved
     * @throws DaoException
     */
    public void save(MetaInfo item) throws DaoException { throw new UnsupportedOperationException(); }
    public Iterable<MetaInfo> get(DaoFilter daoFilter) throws DaoException { throw new UnsupportedOperationException(); }
    public int getCount(DaoFilter daoFilter) throws DaoException { throw new UnsupportedOperationException(); }


    public static class Provider extends org.wikapidia.conf.Provider<MetaInfoDao> {
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
        public MetaInfoDao get(String name, Config config) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                return new MetaInfoSqlDao(
                        getConfigurator().get(
                                DataSource.class,
                                config.getString("dataSource"))
                );
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
