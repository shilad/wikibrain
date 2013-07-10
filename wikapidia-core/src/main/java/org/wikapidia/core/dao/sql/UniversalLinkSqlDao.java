package org.wikapidia.core.dao.sql;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.typesafe.config.Config;
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
 * @author Ari Weiland
 *
 * A SQL database implementation of the UniversalLinkDao.
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
            Tables.UNIVERSAL_LINK.SOURCE_ID,
            Tables.UNIVERSAL_LINK.DEST_ID,
            Tables.UNIVERSAL_LINK.SOURCE_UNIV_ID,
            Tables.UNIVERSAL_LINK.DEST_UNIV_ID,
            Tables.UNIVERSAL_LINK.ALGORITHM_ID,
    };

    @Override
    public void save(UniversalLink link) throws DaoException {
        for (Language language : link.getLanguageSetOfExistsInLangs()) {
            for (LocalLink localLink : link.getLocalLinks(language)) {
                insert(
                    localLink.getLanguage().getId(),
                    localLink.getSourceId(),
                    localLink.getDestId(),
                    link.getSourceUnivId(),
                    link.getDestUnivId(),
                    link.getAlgorithmId()
                );
            }
        }
    }

    @Override
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
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Collection<Condition> conditions = new ArrayList<Condition>();
            if (daoFilter.getNameSpaceIds() != null) {
                conditions.add(Tables.UNIVERSAL_LINK.SOURCE_UNIV_ID.in(daoFilter.getSourceIds()));
            }
            if (daoFilter.getNameSpaceIds() != null) {
                conditions.add(Tables.UNIVERSAL_LINK.DEST_UNIV_ID.in(daoFilter.getDestIds()));
            }
            if (daoFilter.isRedirect() != null) {
                conditions.add(Tables.UNIVERSAL_LINK.ALGORITHM_ID.in(daoFilter.getAlgorithmIds()));
            }
//            if (conditions.isEmpty()) {
//                return null;
//            }
            Cursor<Record> result = context.select().
                    from(Tables.UNIVERSAL_LINK).
                    where(conditions).
                    fetchLazy(getFetchSize());
            return buildUniversalLinksIterable(result);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public UniversalLinkGroup getOutlinks(int sourceId, int algorithmId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Cursor<Record> result = context.select().
                    from(Tables.UNIVERSAL_LINK).
                    where(Tables.UNIVERSAL_LINK.SOURCE_ID.eq(sourceId)).
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
                    where(Tables.UNIVERSAL_LINK.DEST_ID.eq(destId)).
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
    public UniversalLink getUniversalLink(int sourceId, int destId, int algorithmId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Result<Record> result = context.select().
                    from(Tables.UNIVERSAL_LINK).
                    where(Tables.UNIVERSAL_LINK.SOURCE_ID.eq(sourceId)).
                    and(Tables.UNIVERSAL_LINK.DEST_ID.eq(destId)).
                    and(Tables.UNIVERSAL_LINK.ALGORITHM_ID.eq(algorithmId)).
                    fetch();
            return buildUniversalLink(result);
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
        Record temp = null;
        for (Record record : result) {
            allRecords.put(
                    record.getValue(outlinks ?                      // Gets the unique ID of the links
                            Tables.UNIVERSAL_LINK.DEST_UNIV_ID :    // If links are outlinks, dest ID is unique
                            Tables.UNIVERSAL_LINK.SOURCE_UNIV_ID),  // If links are inlinks, source ID is unique
                    record);
            languages.add(Language.getById(record.getValue(Tables.UNIVERSAL_LINK.LANG_ID)));
            temp = record;
        }
        List<UniversalLink> links = new ArrayList<UniversalLink>();
        Map<Integer, UniversalLink> map = new HashMap<Integer, UniversalLink>(links.size());
        for (Integer integer : allRecords.keySet()) {
            map.put(integer, buildUniversalLink(allRecords.get(integer)));
        }
        return new UniversalLinkGroup(
                map,
                outlinks,
                temp.getValue(outlinks ?           // Gets the common ID of the links
                        Tables.UNIVERSAL_LINK.SOURCE_UNIV_ID :  // If links are outlinks, source ID is common
                        Tables.UNIVERSAL_LINK.DEST_UNIV_ID),    // If links are inlinks, dest ID is common
                temp.getValue(Tables.UNIVERSAL_LINK.ALGORITHM_ID),
                new LanguageSet(languages)
        );
    }

    /*
    This method does the same thing as the method above it, except that it
    should use much less memory because it doesn't load the entire cursor
    into memory. Instead, it should run entirely on iterables. However, it
    must instead iterate over the cursor multiple times, rather than just
    once, which increases the time constraint by at least a factor of n.
    TODO: decide which of these methods to use
     */
    private Iterable<UniversalLink> buildUniversalLinksIterable(final Cursor<Record> result) throws DaoException {
        final Multimap<Integer, Integer> univIds = HashMultimap.create();
        for (Record record : result) {
            univIds.put(
                    record.getValue(Tables.UNIVERSAL_LINK.SOURCE_UNIV_ID),
                    record.getValue(Tables.UNIVERSAL_LINK.DEST_UNIV_ID));
        }
        return new SqlDaoIterable<UniversalLink, Map.Entry<Integer, Integer>>(result, univIds.entries().iterator()) {

            @Override
            public UniversalLink transform(Map.Entry<Integer, Integer> item) throws DaoException {
                List<Record> records = new ArrayList<Record>();
                for (Record record : result) {
                    if (    record.getValue(Tables.UNIVERSAL_LINK.SOURCE_UNIV_ID) == item.getKey() &&
                            record.getValue(Tables.UNIVERSAL_LINK.SOURCE_UNIV_ID) == item.getValue()) {
                        records.add(record);
                    }
                }
                return buildUniversalLink(records);
            }
        };
//        return new Iterable<UniversalLink>() {
//
//            private boolean closed = false;
//
//            @Override
//            public Iterator<UniversalLink> iterator() {
//                if (closed) {
//                    throw new IllegalStateException("Iterable can only be iterated over once.");
//                }
//                closed = true;
//                return new Iterator<UniversalLink>() {
//                    @Override
//                    public boolean hasNext() {
//                        return univIds.entries().iterator().hasNext();
//                    }
//
//                    @Override
//                    public UniversalLink next() {
//                        Map.Entry entry = univIds.entries().iterator().next();
//                        List<Record> records = new ArrayList<Record>();
//                        for (Record record : result) {
//                            if (    record.getValue(Tables.UNIVERSAL_LINK.SOURCE_UNIV_ID) == entry.getKey() &&
//                                    record.getValue(Tables.UNIVERSAL_LINK.SOURCE_UNIV_ID) == entry.getValue())
//                            {
//                                records.add(record);
//                            }
//                        }
//                        try {
//                            return buildUniversalLink(records);
//                        } catch (DaoException e) {
//                            LOG.log(Level.WARNING, "Failed to build links: ", e);
//                        }
//                        return null;
//                    }
//
//                    @Override
//                    public void remove() {
//                        throw new UnsupportedOperationException();
//                    }
//                };
//            }
//        };
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
                    record.getValue(Tables.UNIVERSAL_LINK.SOURCE_ID),
                    record.getValue(Tables.UNIVERSAL_LINK.DEST_ID)
            );
            map.put(language, temp);
        }
        Record temp = records.iterator().next();
        return new UniversalLink(
                temp.getValue(Tables.UNIVERSAL_LINK.SOURCE_UNIV_ID),
                temp.getValue(Tables.UNIVERSAL_LINK.DEST_UNIV_ID),
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