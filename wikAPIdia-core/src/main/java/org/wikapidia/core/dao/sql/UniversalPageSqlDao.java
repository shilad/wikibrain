package org.wikapidia.core.dao.sql;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.typesafe.config.Config;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
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
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 *
 * A SQL database implementation of the UniversalPageDao.
 *
 * @author Ari Weiland
 * @author Shilad Sen
 *
 */
public class UniversalPageSqlDao<T extends UniversalPage> extends AbstractSqlDao<T> implements UniversalPageDao<T> {

    private static final TableField [] INSERT_FIELDS = new TableField[] {
            Tables.UNIVERSAL_PAGE.LANG_ID,
            Tables.UNIVERSAL_PAGE.PAGE_ID,
            Tables.UNIVERSAL_PAGE.NAME_SPACE,
            Tables.UNIVERSAL_PAGE.UNIV_ID,
            Tables.UNIVERSAL_PAGE.ALGORITHM_ID
    };

    public UniversalPageSqlDao(DataSource dataSource) throws DaoException {
        super(dataSource, INSERT_FIELDS, "/db/universal-page");
    }

    @Override
    public void save(UniversalPage page) throws DaoException {
        NameSpace nameSpace = page.getNameSpace();
        for (Language language : page.getLanguageSetOfExistsInLangs()) {
            for (Object localPage : page.getLocalPages(language)) {
                insert(
                        language.getId(),
                        ((LocalPage) localPage).getLocalId(),
                        nameSpace.getArbitraryId(),
                        page.getUnivId(),
                        page.getAlgorithmId()
                );
            }
        }
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
            Cursor<Record> result = context.select()
                    .from(Tables.UNIVERSAL_PAGE)
                    .where(conditions)
                    .fetchLazy(getFetchSize());
            Set<int[]> pages = new HashSet<int[]>();
            for (Record record : result) {
                pages.add(new int[]{
                        record.getValue(Tables.UNIVERSAL_PAGE.UNIV_ID),
                        record.getValue(Tables.UNIVERSAL_PAGE.ALGORITHM_ID)});
            }
            return new SqlDaoIterable<T, int[]>(result, pages.iterator(), conn) {

                @Override
                public T transform(int[] item) throws DaoException {
                    return getById(item[0], item[1]);
                }
            };
        } catch (SQLException e) {
            quietlyCloseConn(conn);
            throw new DaoException(e);
        }
    }

    @Override
    public int getNumItems(DaoFilter daoFilter) throws DaoException {
        int i=0;
        for (T page : get(daoFilter)) {
            i++;
        }
        return i;
    }

    @Override
    public T getById(int univId, int algorithmId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Result<Record> result = context.select()
                    .from(Tables.UNIVERSAL_PAGE)
                    .where(Tables.UNIVERSAL_PAGE.UNIV_ID.eq(univId))
                    .and(Tables.UNIVERSAL_PAGE.ALGORITHM_ID.eq(algorithmId))
                    .fetch();
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
            Record record = context.select()
                    .from(Tables.UNIVERSAL_PAGE)
                    .where(Tables.UNIVERSAL_PAGE.LANG_ID.eq(language.getId()))
                    .and(Tables.UNIVERSAL_PAGE.PAGE_ID.eq(localPageId))
                    .and(Tables.UNIVERSAL_PAGE.ALGORITHM_ID.eq(algorithmId))
                    .fetchOne();
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
    public int getNumUniversalPages(int algorithmId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Cursor<Record> result = context.select().
                    from(Tables.UNIVERSAL_PAGE).
                    where(Tables.UNIVERSAL_PAGE.ALGORITHM_ID.eq(algorithmId)).
                    fetchLazy();
            TIntSet ids = new TIntHashSet();
            for (Record record : result){
                ids.add(record.getValue(Tables.UNIVERSAL_PAGE.UNIV_ID));
            }
            return ids.size();
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public Map<Language, TIntIntMap> getAllLocalToUnivIdsMap(int algorithmId, LanguageSet ls) throws DaoException {
        Connection conn = null;
        try{
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn,dialect);
            Map<Language, TIntIntMap> map = new HashMap<Language, TIntIntMap>();
            for (Language l : ls) {
                Cursor<Record> cursor = context.select()
                        .from(Tables.UNIVERSAL_PAGE)
                        .where(Tables.UNIVERSAL_PAGE.ALGORITHM_ID.eq(algorithmId))
                        .and(Tables.UNIVERSAL_PAGE.LANG_ID.eq(l.getId()))
                        .fetchLazy(getFetchSize());
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
        if (result == null || result.isEmpty()) {
            return null;
        }
        Multimap<Language, LocalPage> localPages = HashMultimap.create(result.size(), result.size());
        NameSpace nameSpace = NameSpace.getNameSpaceByArbitraryId(result.get(0).getValue(Tables.LOCAL_PAGE.NAME_SPACE));
        for(Record record : result) {
            Language language = Language.getById(record.getValue(Tables.UNIVERSAL_PAGE.LANG_ID));
            int pageId = record.getValue(Tables.UNIVERSAL_PAGE.PAGE_ID);
            localPages.put(language, new LocalPage(language, pageId, null, nameSpace));
        }
        return new UniversalPage<LocalPage>(
                result.get(0).getValue(Tables.UNIVERSAL_PAGE.UNIV_ID),
                result.get(0).getValue(Tables.UNIVERSAL_PAGE.ALGORITHM_ID),
                nameSpace,
                localPages
        );
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
                                config.getString("dataSource"))
                );
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
