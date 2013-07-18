package org.wikapidia.core.dao.sql;

import com.typesafe.config.Config;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.UniversalLinkDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.UniversalLink;
import org.wikapidia.core.model.UniversalLinkGroup;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * An alternate implementation of a UniversalLinkSqlDao.
 * It ignores the underlying local link map of a universal link, and only
 * maintains the language set in the database via a massive array of booleans.
 *
 * @author Ari Weiland
 */
public class UniversalLinkSkeletalSqlDao extends AbstractSqlDao<UniversalLink> implements UniversalLinkDao {

    public UniversalLinkSkeletalSqlDao(DataSource dataSource) throws DaoException {
        super(dataSource, INSERT_FIELDS, "/db/universal-skeletal-link");
    }

    private static final TableField [] INSERT_FIELDS = new TableField[] {
            Tables.UNIVERSAL_SKELETAL_LINK.SOURCE_ID,
            Tables.UNIVERSAL_SKELETAL_LINK.DEST_ID,
            Tables.UNIVERSAL_SKELETAL_LINK.ALGORITHM_ID,
            Tables.UNIVERSAL_SKELETAL_LINK.LANGS
    };

    @Override
    public void save(UniversalLink item) throws DaoException {
        insert(
                item.getSourceUnivId(),
                item.getDestUnivId(),
                item.getAlgorithmId(),
                item.getLanguageSetOfExistsInLangs().toTruncatedArray()
        );
    }

    @Override
    public Iterable<UniversalLink> get(DaoFilter daoFilter) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Collection<Condition> conditions = new ArrayList<Condition>();
            if (daoFilter.getNameSpaceIds() != null) {
                conditions.add(Tables.UNIVERSAL_SKELETAL_LINK.SOURCE_ID.in(daoFilter.getSourceIds()));
            }
            if (daoFilter.getNameSpaceIds() != null) {
                conditions.add(Tables.UNIVERSAL_SKELETAL_LINK.DEST_ID.in(daoFilter.getDestIds()));
            }
            if (daoFilter.isRedirect() != null) {
                conditions.add(Tables.UNIVERSAL_SKELETAL_LINK.ALGORITHM_ID.in(daoFilter.getAlgorithmIds()));
            }
            Cursor<Record> result = context.select()
                    .from(Tables.UNIVERSAL_SKELETAL_LINK)
                    .where(conditions)
                    .fetchLazy(getFetchSize());
            return new SimpleSqlDaoIterable<UniversalLink>(result, conn) {

                @Override
                public UniversalLink transform(Record item) throws DaoException {
                    return buildUniversalLink(item);
                }
            };
        } catch (SQLException e) {
            quietlyCloseConn(conn);
            throw new DaoException(e);
        }
    }

    @Override
    public UniversalLinkGroup getOutlinks(int sourceId, int algorithmId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Result<Record> result = context.select()
                    .from(Tables.UNIVERSAL_SKELETAL_LINK)
                    .where(Tables.UNIVERSAL_SKELETAL_LINK.SOURCE_ID.eq(sourceId))
                    .and(Tables.UNIVERSAL_SKELETAL_LINK.ALGORITHM_ID.eq(algorithmId))
                    .fetch();
            return buildUniversalLinkGroup(result, true);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public UniversalLinkGroup getInlinks(int destId, int algorithmId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Result<Record> result = context.select()
                    .from(Tables.UNIVERSAL_SKELETAL_LINK)
                    .where(Tables.UNIVERSAL_SKELETAL_LINK.DEST_ID.eq(destId))
                    .and(Tables.UNIVERSAL_SKELETAL_LINK.ALGORITHM_ID.eq(algorithmId))
                    .fetch();
            return buildUniversalLinkGroup(result, false);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public TIntSet getOutlinkIds(int sourceId, int algorithmId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
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
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public TIntSet getInlinkIds(int destId, int algorithmId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
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
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public UniversalLink getUniversalLink(int sourceId, int destId, int algorithmId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Record record = context.select()
                    .from(Tables.UNIVERSAL_SKELETAL_LINK)
                    .where(Tables.UNIVERSAL_SKELETAL_LINK.SOURCE_ID.eq(sourceId))
                    .and(Tables.UNIVERSAL_SKELETAL_LINK.DEST_ID.eq(destId))
                    .and(Tables.UNIVERSAL_SKELETAL_LINK.ALGORITHM_ID.eq(algorithmId))
                    .fetchOne();
            return buildUniversalLink(record);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
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
            for (Language language : link.getLanguageSetOfExistsInLangs()) {
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
                record.getValue(Tables.UNIVERSAL_LINK.UNIV_SOURCE_ID),
                record.getValue(Tables.UNIVERSAL_LINK.UNIV_DEST_ID),
                record.getValue(Tables.UNIVERSAL_LINK.ALGORITHM_ID),
                LanguageSet.getLanguageSet(record.getValue(Tables.UNIVERSAL_SKELETAL_LINK.LANGS))
        );
    }

    public static class Provider extends org.wikapidia.conf.Provider<UniversalLinkDao> {
        public Provider(Configurator configurator, org.wikapidia.conf.Configuration config) throws ConfigurationException {
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
        public UniversalLinkDao get(String name, Config config) throws ConfigurationException {
            if (!config.getString("type").equals("skeletal-sql")) {
                return null;
            }
            try {
                return new UniversalLinkSkeletalSqlDao(
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
