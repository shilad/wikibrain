package org.wikibrain.core.dao.sql;

import com.google.common.collect.Sets;
import com.typesafe.config.Config;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.*;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.UniversalLinkDao;
import org.wikibrain.core.jooq.Tables;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.UniversalLink;
import org.wikibrain.core.model.UniversalLinkGroup;
import org.wikibrain.utils.ObjectDb;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * An alternate implementation of a UniversalLinkSqlDao.
 * It ignores the underlying local link map of a universal link, and only
 * maintains the language set in the database via a byte array where each
 * element represents a single ID. IDs over 255 are represented by 2 bytes.
 *
 * @author Ari Weiland
 */
public class UniversalLinkSkeletalSqlDao extends AbstractSqlDao<UniversalLink> implements UniversalLinkDao {

    private final int algorithmId;
    private File path;
    private ObjectDb<byte[]> objectDb;

    public UniversalLinkSkeletalSqlDao(WpDataSource dataSource, int algorithmId) throws DaoException {
        super(dataSource, INSERT_FIELDS, "/db/universal-skeletal-link");
        this.algorithmId = algorithmId;
    }

    private static final TableField [] INSERT_FIELDS = new TableField[] {
            Tables.UNIVERSAL_SKELETAL_LINK.SOURCE_ID,
            Tables.UNIVERSAL_SKELETAL_LINK.DEST_ID,
            Tables.UNIVERSAL_SKELETAL_LINK.ALGORITHM_ID,
            Tables.UNIVERSAL_SKELETAL_LINK.LANGS
    };

    @Override
    public void beginLoad() throws DaoException {
        super.beginLoad();
        try {
            path = File.createTempFile("univ-links", "odb");
            if (path.isFile()) { path.delete(); }
            path.mkdirs();
            objectDb = new ObjectDb<byte[]>(path, true);
        } catch (IOException e) {
            throw new DaoException(e);
        }
    }

    @Override
    public void save(UniversalLink item) throws DaoException {
        try {
            int sourceId = item.getSourceId();
            int destId = item.getDestId();
            LanguageSet languages = item.getLanguageSet();
            String key = sourceId + "_" + destId + "_" + item.getAlgorithmId();
            byte[] temp = objectDb.get(key);
            if (temp != null) {
                languages = new LanguageSet(Sets.union(LanguageSet.getLanguageSet(temp).getLanguages(), languages.getLanguages()));
            }
            objectDb.put(key, languages.toByteArray());
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (ClassNotFoundException e) {
            throw new DaoException(e);
        }
    }

    @Override
    public void endLoad() throws DaoException {
        for (Pair<String, byte[]> pair : objectDb) {
            String[] ids = pair.getKey().split("_");
            insert(
                    Integer.valueOf(ids[0]),
                    Integer.valueOf(ids[1]),
                    Integer.valueOf(ids[2]),
                    pair.getValue()
            );
        }
        objectDb.close();
        path.delete();
        super.endLoad();
    }

    @Override
    public Iterable<UniversalLink> get(DaoFilter daoFilter) throws DaoException {
        DSLContext context = getJooq();
        try {
            Collection<Condition> conditions = new ArrayList<Condition>();
            if (daoFilter.getNameSpaceIds() != null) {
                conditions.add(Tables.UNIVERSAL_SKELETAL_LINK.SOURCE_ID.in(daoFilter.getSourceIds()));
            }
            if (daoFilter.getNameSpaceIds() != null) {
                conditions.add(Tables.UNIVERSAL_SKELETAL_LINK.DEST_ID.in(daoFilter.getDestIds()));
            }
            if (daoFilter.isRedirect() != null) {
                conditions.add(Tables.UNIVERSAL_SKELETAL_LINK.ALGORITHM_ID.eq(algorithmId));
            }
            Cursor<Record> result = context.select()
                    .from(Tables.UNIVERSAL_SKELETAL_LINK)
                    .where(conditions)
                    .limit(daoFilter.getLimitOrInfinity())
                    .fetchLazy(getFetchSize());
            return new SimpleSqlDaoIterable<UniversalLink>(result, context) {

                @Override
                public UniversalLink transform(Record item) throws DaoException {
                    return buildUniversalLink(item);
                }
            };
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
            if (daoFilter.getNameSpaceIds() != null) {
                conditions.add(Tables.UNIVERSAL_SKELETAL_LINK.SOURCE_ID.in(daoFilter.getSourceIds()));
            }
            if (daoFilter.getNameSpaceIds() != null) {
                conditions.add(Tables.UNIVERSAL_SKELETAL_LINK.DEST_ID.in(daoFilter.getDestIds()));
            }
            if (daoFilter.isRedirect() != null) {
                conditions.add(Tables.UNIVERSAL_SKELETAL_LINK.ALGORITHM_ID.eq(algorithmId));
            }
            return context.select()
                    .from(Tables.UNIVERSAL_SKELETAL_LINK)
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
            Result<Record> result = context.select()
                    .from(Tables.UNIVERSAL_SKELETAL_LINK)
                    .where(Tables.UNIVERSAL_SKELETAL_LINK.SOURCE_ID.eq(sourceId))
                    .and(Tables.UNIVERSAL_SKELETAL_LINK.ALGORITHM_ID.eq(algorithmId))
                    .fetch();
            return buildUniversalLinkGroup(result, true);
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public UniversalLinkGroup getInlinks(int destId) throws DaoException {
        DSLContext context = getJooq();
        try {
            Result<Record> result = context.select()
                    .from(Tables.UNIVERSAL_SKELETAL_LINK)
                    .where(Tables.UNIVERSAL_SKELETAL_LINK.DEST_ID.eq(destId))
                    .and(Tables.UNIVERSAL_SKELETAL_LINK.ALGORITHM_ID.eq(algorithmId))
                    .fetch();
            return buildUniversalLinkGroup(result, false);
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public TIntSet getOutlinkIds(int sourceId) throws DaoException {
        DSLContext context = getJooq();
        try {
            Cursor<Record> result = context.select()
                    .from(Tables.UNIVERSAL_SKELETAL_LINK)
                    .where(Tables.UNIVERSAL_SKELETAL_LINK.SOURCE_ID.eq(sourceId))
                    .and(Tables.UNIVERSAL_SKELETAL_LINK.ALGORITHM_ID.eq(algorithmId))
                    .fetchLazy(getFetchSize());
            TIntSet ids = new TIntHashSet();
            for (Record record : result){
                ids.add(record.getValue(Tables.UNIVERSAL_SKELETAL_LINK.DEST_ID));
            }
            return ids;
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public TIntSet getInlinkIds(int destId) throws DaoException {
        DSLContext context = getJooq();
        try {
            Cursor<Record> result = context.select()
                    .from(Tables.UNIVERSAL_SKELETAL_LINK)
                    .where(Tables.UNIVERSAL_SKELETAL_LINK.DEST_ID.eq(destId))
                    .and(Tables.UNIVERSAL_SKELETAL_LINK.ALGORITHM_ID.eq(algorithmId))
                    .fetchLazy(getFetchSize());
            TIntSet ids = new TIntHashSet();
            for (Record record : result){
                ids.add(record.getValue(Tables.UNIVERSAL_SKELETAL_LINK.SOURCE_ID));
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
            Record record = context.select()
                    .from(Tables.UNIVERSAL_SKELETAL_LINK)
                    .where(Tables.UNIVERSAL_SKELETAL_LINK.SOURCE_ID.eq(sourceId))
                    .and(Tables.UNIVERSAL_SKELETAL_LINK.DEST_ID.eq(destId))
                    .and(Tables.UNIVERSAL_SKELETAL_LINK.ALGORITHM_ID.eq(algorithmId))
                    .fetchOne();
            return buildUniversalLink(record);
        } finally {
            freeJooq(context);
        }
    }
    private UniversalLinkGroup buildUniversalLinkGroup(Result<Record> result, boolean outlinks) throws DaoException {
        if (result == null || result.isEmpty()) {
            return null;
        }
        Map<Integer, UniversalLink> map = new HashMap<Integer, UniversalLink>();
        int commonId = -1;
        int algorithmId = -1;
        for (Record record : result) {
            map.put(
                    record.getValue(outlinks ?                          // Gets the unique ID of the links
                            Tables.UNIVERSAL_SKELETAL_LINK.DEST_ID :    // If links are outlinks, dest ID is unique
                            Tables.UNIVERSAL_SKELETAL_LINK.SOURCE_ID),  // If links are inlinks, source ID is unique
                    buildUniversalLink(record));
            if (commonId == -1) {
                commonId = record.getValue(outlinks ?                   // Gets the common ID of the links
                        Tables.UNIVERSAL_SKELETAL_LINK.SOURCE_ID :      // If links are outlinks, source ID is common
                        Tables.UNIVERSAL_SKELETAL_LINK.DEST_ID);        // If links are inlinks, dest ID is common;
                algorithmId = record.getValue(Tables.UNIVERSAL_LINK.ALGORITHM_ID);
            }
        }
        Set<Language> languages = new HashSet<Language>();
        for (UniversalLink link : map.values()) {
            for (Language language : link.getLanguageSet()) {
                languages.add(language);
            }
        }
        return new UniversalLinkGroup(
                map,
                outlinks,
                commonId,
                algorithmId,
                new LanguageSet(languages)
        );
    }

    private UniversalLink buildUniversalLink(Record record) throws DaoException {
        if (record == null) {
            return null;
        }
        return new UniversalLink(
                record.getValue(Tables.UNIVERSAL_SKELETAL_LINK.SOURCE_ID),
                record.getValue(Tables.UNIVERSAL_SKELETAL_LINK.DEST_ID),
                record.getValue(Tables.UNIVERSAL_SKELETAL_LINK.ALGORITHM_ID),
                LanguageSet.getLanguageSet(record.getValue(Tables.UNIVERSAL_SKELETAL_LINK.LANGS))
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
            if (!config.getString("type").equals("skeletal-sql")) {
                return null;
            }
            try {
                int algorithmId = getConfig().get().getInt("mapper." + config.getString("mapper") + ".algorithmId");
                return new UniversalLinkSkeletalSqlDao(
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
