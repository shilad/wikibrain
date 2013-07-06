package org.wikapidia.core.dao.sql;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.typesafe.config.Config;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.io.IOUtils;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.UniversalPage;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 *
 * @author Ari Weiland
 *
 * A SQL database implementation of the UniversalPageDao.
 *
 */
public class UniversalPageSqlDao<T extends UniversalPage> extends AbstractSqlDao implements UniversalPageDao<T> {

    private final LocalPageDao localPageDao;

    public UniversalPageSqlDao(DataSource dataSource, LocalPageDao localPageDao) throws DaoException {
        super(dataSource);
        this.localPageDao = localPageDao;
    }

    @Override
    public void clear() throws DaoException {
        executeSqlResource("/db/universal-page-drop.sql");
        executeSqlResource("/db/universal-page-schema.sql");
    }

    @Override
    public void beginLoad() throws DaoException {
        executeSqlResource("/db/universal-page-schema.sql");
    }

    @Override
    public void save(UniversalPage page) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            UniversalPage<LocalPage> temp = page;
            NameSpace nameSpace = temp.getNameSpace();
            for (Language language : temp.getLanguageSetOfExistsInLangs()) {
                for (LocalPage localPage : temp.getLocalPages(language)) {
                    context.insertInto(Tables.UNIVERSAL_PAGE).values(
                            language.getId(),
                            localPage.getLocalId(),
                            nameSpace.getArbitraryId(),
                            page.getUnivId(),
                            page.getAlgorithmId()
                    ).execute();
                }
            }
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public void endLoad() throws DaoException {
        executeSqlResource("/db/universal-page-indexes.sql");
    }

    @Override
    public Iterable<T> get(DaoFilter daoFilter) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Collection<Condition> conditions = new ArrayList<Condition>();
            if (daoFilter.getNameSpaceIds() != null) {
                conditions.add(Tables.UNIVERSAL_PAGE.NAME_SPACE.in(daoFilter.getNameSpaceIds()));
            }
            if (daoFilter.isRedirect() != null) {
                conditions.add(Tables.UNIVERSAL_PAGE.ALGORITHM_ID.in(daoFilter.getAlgorithmIds()));
            }
//            if (conditions.isEmpty()) {
//                return null;
//            }
            final Cursor<Record> result = context.select().
                    from(Tables.UNIVERSAL_PAGE).
                    where(conditions).
                    fetchLazy(getFetchSize());
            final Set<Integer> univIds = new HashSet<Integer>();
            for (Record record : result) {
                univIds.add(record.getValue(Tables.UNIVERSAL_PAGE.UNIV_ID));
            }
            return new Iterable<T>() {
                @Override
                public Iterator<T> iterator() {
                    return new Iterator<T>() {
                        @Override
                        public boolean hasNext() {
                            return univIds.iterator().hasNext();
                        }

                        @Override
                        public T next() {
                            Integer id = univIds.iterator().next();
                            List<Record> records = new ArrayList<Record>();
                            for (Record record : result) {
                                if (record.getValue(Tables.UNIVERSAL_PAGE.UNIV_ID) == id) {
                                    records.add(record);
                                }
                            }
                            try {
                                return (T)buildUniversalPage(records);
                            } catch (DaoException e) {
                                LOG.log(Level.WARNING, "Failed to build links: ", e);
                            }
                            return null;
                        }

                        @Override
                        public void remove() {
                            throw new UnsupportedOperationException();
                        }
                    };
                }
            };
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public T getById(int univId, int algorithmId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Result<Record> result = context.select().
                    from(Tables.UNIVERSAL_PAGE).
                    where(Tables.UNIVERSAL_PAGE.UNIV_ID.eq(univId)).
                    and(Tables.UNIVERSAL_PAGE.ALGORITHM_ID.eq(algorithmId)).
                    fetch();
            return (T)buildUniversalPage(result);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public Map<Integer, T> getByIds(Collection<Integer> univIds, int algorithmId) throws DaoException {
        if (univIds == null || univIds.isEmpty()) {
            return null;
        }
        Map<Integer, T> map = new HashMap<Integer, T>();
        for (Integer univId : univIds){
            map.put(univId, getById(univId, algorithmId));
        }
        return map;
    }

    @Override
    public int getUnivPageId(Language language, int localPageId, int algorithmId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Record record = context.select().
                    from(Tables.UNIVERSAL_PAGE).
                    where(Tables.UNIVERSAL_PAGE.LANG_ID.eq(language.getId())).
                    and(Tables.UNIVERSAL_PAGE.PAGE_ID.eq(localPageId)).
                    and(Tables.UNIVERSAL_PAGE.ALGORITHM_ID.eq(algorithmId)).
                    fetchOne();
            if (record == null) {
                return -1;
            } else {
                return record.getValue(Tables.UNIVERSAL_PAGE.UNIV_ID);
            }
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public int getUnivPageId(LocalPage localPage, int algorithmId) throws DaoException {
        return getUnivPageId(localPage.getLanguage(), localPage.getLocalId(), algorithmId);
    }

    @Override
    public Map<Language, TIntIntMap> getAllLocalToUnivIdsMap(int algorithmId, LanguageSet ls) throws DaoException {
        Connection conn = null;
        try{
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn,dialect);
            Map<Language, TIntIntMap> map = new HashMap<Language, TIntIntMap>();
            for (Language l : ls) {
                Cursor<Record> cursor = context.select().
                        from(Tables.UNIVERSAL_PAGE).
                        where(Tables.UNIVERSAL_PAGE.ALGORITHM_ID.eq(algorithmId)).
                        and(Tables.UNIVERSAL_PAGE.LANG_ID.eq(l.getId())).
                        fetchLazy();
                TIntIntMap ids = new TIntIntHashMap(
                        gnu.trove.impl.Constants.DEFAULT_CAPACITY,
                        gnu.trove.impl.Constants.DEFAULT_LOAD_FACTOR,
                        -1, -1);
                for (Record record : cursor) {
                    ids.put(record.getValue(Tables.UNIVERSAL_PAGE.PAGE_ID),
                            record.getValue(Tables.UNIVERSAL_PAGE.UNIV_ID));
                }
                map.put(l, ids);
            }
            return map;
        } catch (SQLException e){
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    /**
     * Build a UniversalPage from a database record representation.
     * Classes that extend class this should override this method.
     *
     * @param result a list of database records
     * @return a UniversalPage representation of the given database record
     * @throws DaoException if the record is not a Page
     */
    protected UniversalPage buildUniversalPage(List<Record> result) throws DaoException {
        if (result == null) {
            return null;
        }
        Multimap<Language, LocalPage> localPages = HashMultimap.create(result.size(), result.size());
        NameSpace nameSpace = NameSpace.getNameSpaceById(result.get(0).getValue(Tables.LOCAL_PAGE.NAME_SPACE));
        for(Record record : result) {
            Language language = Language.getById(record.getValue(Tables.UNIVERSAL_PAGE.LANG_ID));
            int pageId = record.getValue(Tables.UNIVERSAL_PAGE.PAGE_ID);
            localPages.put(language, localPageDao.getById(language, pageId));
        }
        return new UniversalPage<LocalPage>(
                result.get(0).getValue(Tables.UNIVERSAL_PAGE.UNIV_ID),
                result.get(0).getValue(Tables.UNIVERSAL_PAGE.ALGORITHM_ID),
                nameSpace,
                localPages
        ){};
    }

    public static class Provider extends org.wikapidia.conf.Provider<UniversalPageDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return UniversalPageDao.class;
        }

        @Override
        public String getPath() {
            return "dao.universalPage";
        }

        @Override
        public UniversalPageDao get(String name, Config config) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                return new UniversalPageSqlDao(
                        getConfigurator().get(
                                DataSource.class,
                                config.getString("dataSource")),
                        getConfigurator().get(
                                LocalPageDao.class,
                                config.getString("localPageDao"))
                );
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
