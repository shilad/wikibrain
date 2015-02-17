package org.wikibrain.core.dao.sql;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.typesafe.config.Config;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.jooq.*;
import org.wikibrain.conf.*;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalLinkDao;
import org.wikibrain.core.dao.UniversalLinkDao;
import org.wikibrain.core.jooq.Tables;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.model.UniversalLink;
import org.wikibrain.core.model.UniversalLinkGroup;

import java.util.*;

/**
 *
 * A SQL database implementation of the UniversalLinkDao.
 *
 * @author Ari Weiland
 *
 */
public class UniversalLinkSqlDao extends AbstractSqlDao<UniversalLink> implements UniversalLinkDao {

    private final LocalLinkDao localLinkDao;
    private final int algorithmId;

    public UniversalLinkSqlDao(WpDataSource dataSource, LocalLinkDao localLinkDao, int algorithmId) throws DaoException {
        super(dataSource, INSERT_FIELDS, "/db/universal-link");
        this.localLinkDao = localLinkDao;
        this.algorithmId = algorithmId;
    }

    private static final TableField [] INSERT_FIELDS = new TableField[] {
            Tables.UNIVERSAL_LINK.LANG_ID,
            Tables.UNIVERSAL_LINK.LOCAL_SOURCE_ID,
            Tables.UNIVERSAL_LINK.LOCAL_DEST_ID,
            Tables.UNIVERSAL_LINK.UNIV_SOURCE_ID,
            Tables.UNIVERSAL_LINK.UNIV_DEST_ID,
            Tables.UNIVERSAL_LINK.ALGORITHM_ID,
    };

    @Override
    public void save(UniversalLink link) throws DaoException {
        for (Language language : link.getLanguageSet()) {
            for (LocalLink localLink : link.getLocalLinks(language)) {
                save(
                        localLink,
                        link.getSourceId(),
                        link.getDestId(),
                        link.getAlgorithmId()
                );
            }
        }
    }

    public void save(LocalLink localLink, int sourceUnivId, int destUnivId, int algorithmId) throws DaoException {
        insert(
                localLink.getLanguage().getId(),
                localLink.getSourceId(),
                localLink.getDestId(),
                sourceUnivId,
                destUnivId,
                algorithmId
        );
    }

    @Override
    public Iterable<UniversalLink> get(DaoFilter daoFilter) throws DaoException {
        DSLContext context = getJooq();
        try {
            Collection<Condition> conditions = new ArrayList<Condition>();
            if (daoFilter.getSourceIds() != null) {
                conditions.add(Tables.UNIVERSAL_LINK.UNIV_SOURCE_ID.in(daoFilter.getSourceIds()));
            }
            if (daoFilter.getDestIds() != null) {
                conditions.add(Tables.UNIVERSAL_LINK.UNIV_DEST_ID.in(daoFilter.getDestIds()));
            }
            if (daoFilter.isRedirect() != null) {
                conditions.add(Tables.UNIVERSAL_LINK.ALGORITHM_ID.eq(algorithmId));
            }
            Cursor<Record> result = context.select().
                    from(Tables.UNIVERSAL_LINK).
                    where(conditions).
                    limit(daoFilter.getLimitOrInfinity()).
                    fetchLazy(getFetchSize());
            return buildUniversalLinksIterable(result, context);
        } catch (RuntimeException e) {
            freeJooq(context);
            throw e;
        }
    }

    @Override
    public int getCount(DaoFilter daoFilter) throws DaoException {
        DSLContext context = getJooq();
        try {
            Collection<Condition> conditions = new ArrayList<Condition>();
            if (daoFilter.getSourceIds() != null) {
                conditions.add(Tables.UNIVERSAL_LINK.UNIV_SOURCE_ID.in(daoFilter.getSourceIds()));
            }
            if (daoFilter.getDestIds() != null) {
                conditions.add(Tables.UNIVERSAL_LINK.UNIV_DEST_ID.in(daoFilter.getDestIds()));
            }
            if (daoFilter.isRedirect() != null) {
                conditions.add(Tables.UNIVERSAL_LINK.ALGORITHM_ID.eq(algorithmId));
            }
            return context.selectDistinct(Tables.UNIVERSAL_LINK.UNIV_SOURCE_ID, Tables.UNIVERSAL_LINK.UNIV_DEST_ID, Tables.UNIVERSAL_LINK.ALGORITHM_ID)
                    .from(Tables.UNIVERSAL_LINK)
                    .where(conditions)
                    .fetchCount();
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public UniversalLinkGroup getOutlinks(int sourceId) throws DaoException {
        DSLContext context = getJooq();
        try {
            Cursor<Record> result = context.select().
                    from(Tables.UNIVERSAL_LINK).
                    where(Tables.UNIVERSAL_LINK.UNIV_SOURCE_ID.eq(sourceId)).
                    and(Tables.UNIVERSAL_LINK.ALGORITHM_ID.eq(algorithmId)).
                    fetchLazy(getFetchSize());
            return buildUniversalLinkGroup(result, true);
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public TIntSet getOutlinkIds(int sourceId) throws DaoException{
        DSLContext context = getJooq();
        try {
            Cursor<Record> result = context.select().
                    from(Tables.UNIVERSAL_LINK).
                    where(Tables.UNIVERSAL_LINK.UNIV_SOURCE_ID.eq(sourceId)).
                    and(Tables.UNIVERSAL_LINK.ALGORITHM_ID.eq(algorithmId)).
                    fetchLazy(getFetchSize());
            TIntSet ids = new TIntHashSet();
            for (Record record : result){
                ids.add(record.getValue(Tables.UNIVERSAL_LINK.UNIV_DEST_ID));
            }
            return ids;
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public UniversalLinkGroup getInlinks(int destId) throws DaoException {
        DSLContext context = getJooq();
        try {
            Cursor<Record> result = context.select().
                    from(Tables.UNIVERSAL_LINK).
                    where(Tables.UNIVERSAL_LINK.UNIV_DEST_ID.eq(destId)).
                    and(Tables.UNIVERSAL_LINK.ALGORITHM_ID.eq(algorithmId)).
                    fetchLazy(getFetchSize());
            return buildUniversalLinkGroup(result, false);
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public TIntSet getInlinkIds(int destId) throws DaoException{
        DSLContext context = getJooq();
        try {
            Cursor<Record> result = context.select().
                    from(Tables.UNIVERSAL_LINK).
                    where(Tables.UNIVERSAL_LINK.UNIV_DEST_ID.eq(destId)).
                    and(Tables.UNIVERSAL_LINK.ALGORITHM_ID.eq(algorithmId)).
                    fetchLazy(getFetchSize());
            TIntSet ids = new TIntHashSet();
            for (Record record : result){
                ids.add(record.getValue(Tables.UNIVERSAL_LINK.UNIV_SOURCE_ID));
            }
            return ids;
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public UniversalLink getUniversalLink(int sourceId, int destId) throws DaoException {
        DSLContext context = getJooq();
        try {
            Result<Record> result = context.select().
                    from(Tables.UNIVERSAL_LINK).
                    where(Tables.UNIVERSAL_LINK.UNIV_SOURCE_ID.eq(sourceId)).
                    and(Tables.UNIVERSAL_LINK.UNIV_DEST_ID.eq(destId)).
                    and(Tables.UNIVERSAL_LINK.ALGORITHM_ID.eq(algorithmId)).
                    fetch();
            return buildUniversalLink(result);
        } finally {
            freeJooq(context);
        }
    }

    private UniversalLinkGroup buildUniversalLinkGroup(Cursor<Record> result, boolean outlinks) throws DaoException {
        if (!result.hasNext()) {
            return null;
        }
        Multimap<Integer, Record> allRecords = HashMultimap.create();
        Set<Language> languages = new HashSet<Language>();
        int commonId = -1;
        int algorithmId = -1;
        for (Record record : result) {
            allRecords.put(
                    record.getValue(outlinks ?                      // Gets the unique ID of the links
                            Tables.UNIVERSAL_LINK.UNIV_DEST_ID :    // If links are outlinks, dest ID is unique
                            Tables.UNIVERSAL_LINK.UNIV_SOURCE_ID),  // If links are inlinks, source ID is unique
                    record);
            languages.add(Language.getById(record.getValue(Tables.UNIVERSAL_LINK.LANG_ID)));
            if (commonId == -1) {
                commonId = record.getValue(outlinks ?               // Gets the common ID of the links
                        Tables.UNIVERSAL_LINK.UNIV_SOURCE_ID :      // If links are outlinks, source ID is common
                        Tables.UNIVERSAL_LINK.UNIV_DEST_ID);        // If links are inlinks, dest ID is common;
                algorithmId = record.getValue(Tables.UNIVERSAL_LINK.ALGORITHM_ID);
            }
        }
        Map<Integer, UniversalLink> map = new HashMap<Integer, UniversalLink>();
        for (Integer integer : allRecords.keySet()) {
            map.put(integer, buildUniversalLink(allRecords.get(integer)));
        }
        return new UniversalLinkGroup(
                map,
                outlinks,
                commonId,
                algorithmId,
                new LanguageSet(languages)
        );
    }

    private Iterable<UniversalLink> buildUniversalLinksIterable(Cursor<Record> result, DSLContext context) throws DaoException {
        Set<Integer[]> links = new HashSet<Integer[]>();
        for (Record record : result) {
            links.add(new Integer[]{
                    record.getValue(Tables.UNIVERSAL_LINK.UNIV_SOURCE_ID),
                    record.getValue(Tables.UNIVERSAL_LINK.UNIV_DEST_ID),
                    record.getValue(Tables.UNIVERSAL_LINK.ALGORITHM_ID)});
        }
        return new SqlDaoIterable<UniversalLink, Integer[]>(result, links.iterator(), context) {

            @Override
            public UniversalLink transform(Integer[] item) throws DaoException {
                return getUniversalLink(item[0], item[1]);
            }
        };
    }

    private UniversalLink buildUniversalLink(Collection<Record> records) throws DaoException {
        if (records == null || records.isEmpty()) {
            return null;
        }
        Multimap<Language, LocalLink> map = HashMultimap.create(records.size(), records.size());
        for (Record record : records) {
            Language language = Language.getById(record.getValue(Tables.UNIVERSAL_LINK.LANG_ID));
            LocalLink temp = localLinkDao.getLink(
                    language,
                    record.getValue(Tables.UNIVERSAL_LINK.LOCAL_SOURCE_ID),
                    record.getValue(Tables.UNIVERSAL_LINK.LOCAL_DEST_ID)
            );
            map.put(language, temp);
        }
        Record temp = records.iterator().next();
        return new UniversalLink(
                temp.getValue(Tables.UNIVERSAL_LINK.UNIV_SOURCE_ID),
                temp.getValue(Tables.UNIVERSAL_LINK.UNIV_DEST_ID),
                temp.getValue(Tables.UNIVERSAL_LINK.ALGORITHM_ID),
                map
        );
    }

    public static class Provider extends org.wikibrain.conf.Provider<UniversalLinkDao> {
        public Provider(Configurator configurator, org.wikibrain.conf.Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return UniversalLinkDao.class;
        }

        @Override
        public String getPath() {
            return "dao.universalLink";
        }

        @Override
        public UniversalLinkDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {

                int algorithmId = getConfig().get().getInt("mapper." + config.getString("mapper") + ".algorithmId");
                return new UniversalLinkSqlDao(
                        getConfigurator().get(
                                WpDataSource.class,
                                config.getString("dataSource")),
                        getConfigurator().get(
                                LocalLinkDao.class,
                                config.getString("localLinkDao")),
                        algorithmId
                );
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }

}
