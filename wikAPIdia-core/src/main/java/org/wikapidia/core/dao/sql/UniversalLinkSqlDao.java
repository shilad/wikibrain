package org.wikapidia.core.dao.sql;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.typesafe.config.Config;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.wikapidia.conf.*;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.UniversalLinkDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.model.UniversalLink;
import org.wikapidia.core.model.UniversalLinkGroup;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
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
    
    public UniversalLinkSqlDao(DataSource dataSource, LocalLinkDao localLinkDao) throws DaoException {
        super(dataSource, INSERT_FIELDS, "/db/universal-link");
        this.localLinkDao = localLinkDao;
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
        for (Language language : link.getLanguageSetOfExistsInLangs()) {
            for (LocalLink localLink : link.getLocalLinks(language)) {
                save(
                        localLink,
                        link.getSourceUnivId(),
                        link.getDestUnivId(),
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
        throw new UnsupportedOperationException("Temporarily out of order");
//        Connection conn = null;
//        try {
//            conn = ds.getConnection();
//            DSLContext context = DSL.using(conn, dialect);
//            Collection<Condition> conditions = new ArrayList<Condition>();
//            if (daoFilter.getNameSpaceIds() != null) {
//                conditions.add(Tables.UNIVERSAL_LINK.UNIV_SOURCE_ID.in(daoFilter.getSourceIds()));
//            }
//            if (daoFilter.getNameSpaceIds() != null) {
//                conditions.add(Tables.UNIVERSAL_LINK.UNIV_DEST_ID.in(daoFilter.getDestIds()));
//            }
//            if (daoFilter.isRedirect() != null) {
//                conditions.add(Tables.UNIVERSAL_LINK.ALGORITHM_ID.in(daoFilter.getAlgorithmIds()));
//            }
//            Cursor<Record> result = context.select().
//                    from(Tables.UNIVERSAL_LINK).
//                    where(conditions).
//                    fetchLazy(getFetchSize());
//            return buildUniversalLinksIterable(result, conn);
//        } catch (SQLException e) {
//            quietlyCloseConn(conn);
//            throw new DaoException(e);
//        }
    }

    @Override
    public UniversalLinkGroup getOutlinks(int sourceId, int algorithmId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Cursor<Record> result = context.select().
                    from(Tables.UNIVERSAL_LINK).
                    where(Tables.UNIVERSAL_LINK.UNIV_SOURCE_ID.eq(sourceId)).
                    and(Tables.UNIVERSAL_LINK.ALGORITHM_ID.eq(algorithmId)).
                    fetchLazy(getFetchSize());
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
            Cursor<Record> result = context.select().
                    from(Tables.UNIVERSAL_LINK).
                    where(Tables.UNIVERSAL_LINK.UNIV_DEST_ID.eq(destId)).
                    and(Tables.UNIVERSAL_LINK.ALGORITHM_ID.eq(algorithmId)).
                    fetchLazy(getFetchSize());
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
            Result<Record> result = context.select().
                    from(Tables.UNIVERSAL_LINK).
                    where(Tables.UNIVERSAL_LINK.UNIV_SOURCE_ID.eq(sourceId)).
                    and(Tables.UNIVERSAL_LINK.UNIV_DEST_ID.eq(destId)).
                    and(Tables.UNIVERSAL_LINK.ALGORITHM_ID.eq(algorithmId)).
                    fetch();
            return buildUniversalLink(result, true);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
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
            map.put(integer, buildUniversalLink(allRecords.get(integer), outlinks));
        }
        return new UniversalLinkGroup(
                map,
                outlinks,
                commonId,
                algorithmId,
                new LanguageSet(languages)
        );
    }

    private UniversalLink buildUniversalLink(Collection<Record> records, boolean outlinks) throws DaoException {
        if (records == null || records.isEmpty()) {
            return null;
        }
        Multimap<Language, LocalLink> map = HashMultimap.create(records.size(), records.size());
        for (Record record : records) {
            Language language = Language.getById(record.getValue(Tables.UNIVERSAL_LINK.LANG_ID));
            LocalLink temp = new LocalLink(
                    language,
                    null,
                    record.getValue(Tables.UNIVERSAL_LINK.LOCAL_SOURCE_ID),
                    record.getValue(Tables.UNIVERSAL_LINK.LOCAL_DEST_ID),
                    outlinks,
                    -1,
                    true,
                    null
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
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                return new UniversalLinkSqlDao(
                        getConfigurator().get(
                                DataSource.class,
                                config.getString("dataSource")),
                        getConfigurator().get(
                                LocalLinkDao.class,
                                config.getString("localLinkDao"))
                );
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }

}