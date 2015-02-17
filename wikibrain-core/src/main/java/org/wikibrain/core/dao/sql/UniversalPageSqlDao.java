package org.wikibrain.core.dao.sql;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.typesafe.config.Config;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.jooq.*;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.jooq.Tables;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.UniversalPage;

import java.util.*;

/**
 *
 * A SQL database implementation of the UniversalPageDao.
 *
 * @author Ari Weiland
 * @author Shilad Sen
 *
 */
public class UniversalPageSqlDao extends AbstractSqlDao<UniversalPage> implements UniversalPageDao {

    private static final TableField [] INSERT_FIELDS = new TableField[] {
            Tables.UNIVERSAL_PAGE.LANG_ID,
            Tables.UNIVERSAL_PAGE.PAGE_ID,
            Tables.UNIVERSAL_PAGE.NAME_SPACE,
            Tables.UNIVERSAL_PAGE.UNIV_ID,
            Tables.UNIVERSAL_PAGE.ALGORITHM_ID
    };
    private final int algorithmId;

    public UniversalPageSqlDao(WpDataSource dataSource, int algorithmId) throws DaoException {
        super(dataSource, INSERT_FIELDS, "/db/universal-page");
        this.algorithmId = algorithmId;
    }

    @Override
    public void save(UniversalPage page) throws DaoException {
        NameSpace nameSpace = page.getNameSpace();
        for (Language language : page.getLanguageSet()) {
            for (LocalId localPage : page.getLocalEntities(language)) {
                insert(
                        language.getId(),
                        localPage.getId(),
                        nameSpace.getArbitraryId(),
                        page.getUnivId(),
                        page.getAlgorithmId()
                );
            }
        }
    }

    @Override
    public Iterable<UniversalPage> get(DaoFilter daoFilter) throws DaoException {
        DSLContext context = getJooq();
        try {
            Collection<Condition> conditions = new ArrayList<Condition>();
            if (daoFilter.getNameSpaceIds() != null) {
                conditions.add(Tables.UNIVERSAL_PAGE.NAME_SPACE.in(daoFilter.getNameSpaceIds()));
            }
            if (daoFilter.isRedirect() != null) {
                conditions.add(Tables.UNIVERSAL_PAGE.ALGORITHM_ID.eq(algorithmId));
            }
            Cursor<Record> result = context.select()
                    .from(Tables.UNIVERSAL_PAGE)
                    .where(conditions)
                    .limit(daoFilter.getLimitOrInfinity())
                    .fetchLazy(getFetchSize());
            Set<int[]> pages = new HashSet<int[]>();
            for (Record record : result) {
                pages.add(new int[]{
                        record.getValue(Tables.UNIVERSAL_PAGE.UNIV_ID),
                        record.getValue(Tables.UNIVERSAL_PAGE.ALGORITHM_ID)});
            }
            return new SqlDaoIterable<UniversalPage, int[]>(result, pages.iterator(), context) {

                @Override
                public UniversalPage transform(int[] item) throws DaoException {
                    return getById(item[0]);
                }
            };
        } catch (RuntimeException e) {
            freeJooq(context);
            throw e;
        }
    }

    @Override
    public int getCount(DaoFilter daoFilter) throws DaoException{
        DSLContext context = getJooq();
        try {
            Collection<Condition> conditions = new ArrayList<Condition>();
            if (daoFilter.getNameSpaceIds() != null) {
                conditions.add(Tables.UNIVERSAL_PAGE.NAME_SPACE.in(daoFilter.getNameSpaceIds()));
            }
            if (daoFilter.isRedirect() != null) {
                conditions.add(Tables.UNIVERSAL_PAGE.ALGORITHM_ID.eq(algorithmId));
            }
            return context.selectDistinct(Tables.UNIVERSAL_PAGE.UNIV_ID, Tables.UNIVERSAL_PAGE.ALGORITHM_ID)
                    .from(Tables.UNIVERSAL_PAGE)
                    .where(conditions)
                    .fetchCount();
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public UniversalPage getById(int univId) throws DaoException {
        DSLContext context = getJooq();
        try {
            Result<Record> result = context.select()
                    .from(Tables.UNIVERSAL_PAGE)
                    .where(Tables.UNIVERSAL_PAGE.UNIV_ID.eq(univId))
                    .and(Tables.UNIVERSAL_PAGE.ALGORITHM_ID.eq(algorithmId))
                    .fetch();
            return (UniversalPage)buildUniversalPage(result);
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public Map<Integer, UniversalPage> getByIds(Collection<Integer> univIds) throws DaoException {
        if (univIds == null || univIds.isEmpty()) {
            return null;
        }
        Map<Integer, UniversalPage> map = new HashMap<Integer, UniversalPage>();
        for (Integer univId : univIds){
            map.put(univId, getById(univId));
        }
        return map;
    }

    @Override
    public UniversalPage getByLocalPage(LocalPage localPage) throws DaoException {
        int conceptId = getUnivPageId(localPage);
        if (conceptId < 0) {
            return null;
        }
        return getById(conceptId);
    }

    @Override
    public int getUnivPageId(Language language, int localPageId) throws DaoException {
        DSLContext context = getJooq();
        try {
            Record record = context.select()
                    .from(Tables.UNIVERSAL_PAGE)
                    .where(Tables.UNIVERSAL_PAGE.LANG_ID.eq(language.getId()))
                    .and(Tables.UNIVERSAL_PAGE.PAGE_ID.eq(localPageId))
                    .and(Tables.UNIVERSAL_PAGE.ALGORITHM_ID.eq(algorithmId))
                    .limit(1)   // TODO: Remove
                    .fetchOne();
            if (record == null) {
                return -1;
            } else {
                return record.getValue(Tables.UNIVERSAL_PAGE.UNIV_ID);
            }
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public int getUnivPageId(LocalPage localPage) throws DaoException {
        return getUnivPageId(localPage.getLanguage(), localPage.getLocalId());
    }

    @Override
    public Map<Language, TIntIntMap> getAllLocalToUnivIdsMap(LanguageSet ls) throws DaoException {
        DSLContext context = getJooq();
        try {
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
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public Map<Language, TIntIntMap> getAllUnivToLocalIdsMap(LanguageSet ls) throws DaoException {
        DSLContext context = getJooq();
        try {
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
                    ids.put(record.getValue(Tables.UNIVERSAL_PAGE.UNIV_ID),
                            record.getValue(Tables.UNIVERSAL_PAGE.PAGE_ID));
                }
                map.put(l, ids);
            }
            return map;
        } finally {
            freeJooq(context);
        }
    }

    /**
     * Returns the local page ids for that language, value in map is -1 they do not exist
     * @param language
     * @param universalIds
     * @return
     * @throws DaoException
     */
    @Override
    public Map<Integer, Integer> getLocalIds(Language language, Collection<Integer> universalIds) throws DaoException {
        DSLContext context = getJooq();
        try {
            Object rows[][] = context
                    .select(Tables.UNIVERSAL_PAGE.UNIV_ID, Tables.UNIVERSAL_PAGE.PAGE_ID)
                    .from(Tables.UNIVERSAL_PAGE)
                    .where(Tables.UNIVERSAL_PAGE.LANG_ID.eq(language.getId()))
                    .and(Tables.UNIVERSAL_PAGE.UNIV_ID.in(universalIds))
                    .and(Tables.UNIVERSAL_PAGE.ALGORITHM_ID.eq(algorithmId))
                    .fetchArrays();

            Map<Integer, Integer> result = new HashMap<Integer, Integer>();
            if (rows == null) {
                return result;
            }
            for (Object [] row : rows) {
                result.put((Integer)row[0], (Integer)row[1]);
            }
            return result;
        } finally {
            freeJooq(context);
        }
    }

    /**
     * Returns the local page id for that language, or -1 if it does not exist
     * @param language
     * @param universalId
     * @return
     * @throws DaoException
     */
    @Override
    public int getLocalId(Language language, int universalId) throws DaoException {
        DSLContext context = getJooq();
        try {
            Record record = context.select()
                    .from(Tables.UNIVERSAL_PAGE)
                    .where(Tables.UNIVERSAL_PAGE.LANG_ID.eq(language.getId()))
                    .and(Tables.UNIVERSAL_PAGE.UNIV_ID.eq(universalId))
                    .and(Tables.UNIVERSAL_PAGE.ALGORITHM_ID.eq(algorithmId))
                    .limit(1)   // TODO: Remove
                    .fetchOne();
            if (record == null) {
                return -1;
            } else {
                return record.getValue(Tables.UNIVERSAL_PAGE.PAGE_ID);
            }
        } finally {
            freeJooq(context);
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
        Multimap<Language, LocalId> localPages = HashMultimap.create(result.size(), result.size());
        NameSpace nameSpace = NameSpace.getNameSpaceByArbitraryId(result.get(0).getValue(Tables.LOCAL_PAGE.NAME_SPACE));
        for(Record record : result) {
            Language language = Language.getById(record.getValue(Tables.UNIVERSAL_PAGE.LANG_ID));
            int pageId = record.getValue(Tables.UNIVERSAL_PAGE.PAGE_ID);
            localPages.put(language, new LocalId(language, pageId));
        }
        return new UniversalPage(
                result.get(0).getValue(Tables.UNIVERSAL_PAGE.UNIV_ID),
                result.get(0).getValue(Tables.UNIVERSAL_PAGE.ALGORITHM_ID),
                nameSpace,
                localPages
        );
    }

    public static class Provider extends org.wikibrain.conf.Provider<UniversalPageDao> {
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
        public UniversalPageDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                int algorithmId = getConfig().get().getInt("mapper." + config.getString("mapper") + ".algorithmId");
                return new UniversalPageSqlDao(
                        getConfigurator().get(
                                WpDataSource.class,
                                config.getString("dataSource")),
                        algorithmId
                );
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
