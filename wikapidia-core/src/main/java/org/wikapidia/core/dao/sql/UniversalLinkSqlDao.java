package org.wikapidia.core.dao.sql;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.io.IOUtils;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.UniversalLinkDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.model.UniversalLink;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 */
public class UniversalLinkSqlDao extends AbstractSqlDao implements UniversalLinkDao {

    private final LocalLinkDao localLinkDao;
    
    public UniversalLinkSqlDao(DataSource dataSource, LocalLinkDao localLinkDao) throws DaoException {
        super(dataSource);
        this.localLinkDao = localLinkDao;
    }

    @Override
    public void beginLoad() throws DaoException {
        Connection conn=null;
        try {
            conn = ds.getConnection();
            conn.createStatement().execute(
                    IOUtils.toString(
                            UniversalLinkSqlDao.class.getResource("/db/universal-link-schema.sql")
                    ));
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (SQLException e){
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public void save(UniversalLink link) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            for (Language language : link.getLanguageSetOfExistsInLangs()) {
                for (LocalLink localLink : link.getLocalLinks(language)) {
                    context.insertInto(Tables.UNIVERSAL_LINK).values(
                            localLink.getLanguage().getId(),
                            localLink.getSourceId(),
                            localLink.getDestId(),
                            localLink.getLocation(),
                            link.getSourceUnivId(),
                            link.getDestUnivId(),
                            link.getAlgorithmId()
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
    public void save(LocalLink localLink, int sourceUnivId, int destUnivId, int algorithmId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            context.insertInto(Tables.UNIVERSAL_LINK).values(
                    localLink.getLanguage().getId(),
                    localLink.getSourceId(),
                    localLink.getDestId(),
                    sourceUnivId,
                    destUnivId,
                    algorithmId
            ).execute();
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public void endLoad() throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            conn.createStatement().execute(
                    IOUtils.toString(
                            UniversalLinkSqlDao.class.getResource("/db/universal-link-indexes.sql")
                    )
            );
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (SQLException e){
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
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
            if (conditions.isEmpty()) {
                return null;
            }
            Cursor<Record> result = context.select().
                    from(Tables.UNIVERSAL_LINK).
                    where(conditions).
                    fetchLazy();
            return buildUniversalLinksIterable(result);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public List<UniversalLink> getBySourceId(int sourceId, int algorithmId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Cursor<Record> result = context.select().
                    from(Tables.UNIVERSAL_LINK).
                    where(Tables.UNIVERSAL_LINK.SOURCE_ID.eq(sourceId)).
                    and(Tables.UNIVERSAL_LINK.ALGORITHM_ID.eq(algorithmId)).
                    fetchLazy(getFetchSize());
            return buildUniversalLinks(result, true);
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public List<UniversalLink> getByDestId(int destId, int algorithmId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Cursor<Record> result = context.select().
                    from(Tables.UNIVERSAL_LINK).
                    where(Tables.UNIVERSAL_LINK.DEST_ID.eq(destId)).
                    and(Tables.UNIVERSAL_LINK.ALGORITHM_ID.eq(algorithmId)).
                    fetchLazy(getFetchSize());
            return buildUniversalLinks(result, false);
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

    private List<UniversalLink> buildUniversalLinks(Cursor<Record> result, boolean source) throws DaoException {
        Multimap<Integer, Record> allRecords = HashMultimap.create();
        for (Record record : result) {
            allRecords.put(
                    record.getValue(source ?
                            Tables.UNIVERSAL_LINK.DEST_UNIV_ID :
                            Tables.UNIVERSAL_LINK.SOURCE_UNIV_ID),
                    record);
        }
        List<UniversalLink> links = new ArrayList<UniversalLink>();
        for (Integer integer : allRecords.keySet()) {
            links.add(buildUniversalLink(allRecords.get(integer)));
        }
        return links;
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
        return new Iterable<UniversalLink>() {
            @Override
            public Iterator<UniversalLink> iterator() {
                return new Iterator<UniversalLink>() {
                    @Override
                    public boolean hasNext() {
                        return univIds.entries().iterator().hasNext();
                    }

                    @Override
                    public UniversalLink next() {
                        Map.Entry entry = univIds.entries().iterator().next();
                        List<Record> records = new ArrayList<Record>();
                        for (Record record : result) {
                            if (    record.getValue(Tables.UNIVERSAL_LINK.SOURCE_UNIV_ID) == entry.getKey() &&
                                    record.getValue(Tables.UNIVERSAL_LINK.SOURCE_UNIV_ID) == entry.getValue())
                            {
                                records.add(record);
                            }
                        }
                        try {
                            return buildUniversalLink(records);
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
}