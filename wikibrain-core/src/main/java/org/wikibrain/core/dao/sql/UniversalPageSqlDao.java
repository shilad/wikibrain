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
public class UniversalPageSqlDao<T extends UniversalPage> extends AbstractSqlDao<T> implements UniversalPageDao<T> {

    private static final TableField [] INSERT_FIELDS = new TableField[] {
            Tables.UNIVERSAL_PAGE.LANG_ID,
            Tables.UNIVERSAL_PAGE.PAGE_ID,
            Tables.UNIVERSAL_PAGE.NAME_SPACE,
            Tables.UNIVERSAL_PAGE.UNIV_ID,
            Tables.UNIVERSAL_PAGE.ALGORITHM_ID
    };

    public UniversalPageSqlDao(WpDataSource dataSource) throws DaoException {
        super(dataSource, INSERT_FIELDS, "/db/universal-page");
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
    public Iterable<T> get(DaoFilter daoFilter) throws DaoException {
        DSLContext context = getJooq();
        try {
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
                    .limit(daoFilter.getLimitOrInfinity())
                    .fetchLazy(getFetchSize());
            Set<int[]> pages = new HashSet<int[]>();
            for (Record record : result) {
                pages.add(new int[]{
                        record.getValue(Tables.UNIVERSAL_PAGE.UNIV_ID),
                        record.getValue(Tables.UNIVERSAL_PAGE.ALGORITHM_ID)});
            }
            return new SqlDaoIterable<T, int[]>(result, pages.iterator(), context) {

                @Override
                public T transform(int[] item) throws DaoException {
                    return getById(item[0], item[1]);
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
                conditions.add(Tables.UNIVERSAL_PAGE.ALGORITHM_ID.in(daoFilter.getAlgorithmIds()));
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
    public T getById(int univId, int algorithmId) throws DaoException {
        DSLContext context = getJooq();
        try {
            Result<Record> result = context.select()
                    .from(Tables.UNIVERSAL_PAGE)
                    .where(Tables.UNIVERSAL_PAGE.UNIV_ID.eq(univId))
                    .and(Tables.UNIVERSAL_PAGE.ALGORITHM_ID.eq(algorithmId))
                    .fetch();
            return (T)buildUniversalPage(result);
        } finally {
            freeJooq(context);
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
    public UniversalPage getByLocalPage(LocalPage localPage, int algorithmId) throws DaoException {
        int conceptId = getUnivPageId(localPage, algorithmId);
        if (conceptId < 0) {
            return null;
        }
        return getById(conceptId, algorithmId);
    }

    @Override
    public int getUnivPageId(Language language, int localPageId, int algorithmId) throws DaoException {
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
    public int getUnivPageId(LocalPage localPage, int algorithmId) throws DaoException {
        return getUnivPageId(localPage.getLanguage(), localPage.getLocalId(), algorithmId);
    }

    @Override
    public Map<Language, TIntIntMap> getAllLocalToUnivIdsMap(int algorithmId, LanguageSet ls) throws DaoException {
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
                return new UniversalPageSqlDao(
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
