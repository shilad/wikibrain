package org.wikapidia.core.dao.sql;

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
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.RedirectDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.Redirect;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

/**
 */
public class RedirectSqlDao extends AbstractSqlDao<Redirect> implements RedirectDao {

    private static final TableField [] INSERT_FIELDS = new TableField[] {
            Tables.REDIRECT.LANG_ID,
            Tables.REDIRECT.SRC_PAGE_ID,
            Tables.REDIRECT.DEST_PAGE_ID,
    };

    public RedirectSqlDao(DataSource dataSource) throws DaoException {
        super(dataSource, INSERT_FIELDS, "/db/redirect");
    }

    @Override
    public void save(Redirect redirect) throws DaoException {
        insert(
                redirect.getLanguage().getId(),
                redirect.getSourceId(),
                redirect.getDestId()
        );
    }

    @Override
    public void save(Language lang, int src, int dest) throws DaoException {
        save(new Redirect(lang, src, dest));
    }

    /**
     * Generally this method should not be used.
     * @param daoFilter a set of filters to limit the search
     * @return
     * @throws DaoException
     */
    @Override
    public Iterable<Redirect> get(DaoFilter daoFilter) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Collection<Condition> conditions = new ArrayList<Condition>();
            if (daoFilter.getLangIds() != null) {
                conditions.add(Tables.REDIRECT.LANG_ID.in(daoFilter.getLangIds()));
//            } else {
//                return null;
            }
            Cursor<Record> result = context.select().
                    from(Tables.REDIRECT).
                    where(conditions).
                    fetchLazy(getFetchSize());
            return new SimpleSqlDaoIterable<Redirect>(result, conn) {
                @Override
                public Redirect transform(Record r) {
                    return buildRedirect(r);
                }
            };
        } catch (SQLException e) {
            quietlyCloseConn(conn);
            throw new DaoException(e);
        }
    }

    @Override
    public Integer resolveRedirect(Language lang, int id) throws DaoException {
        Connection conn=null;
        try{
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn,dialect);
            Record record = context.select().from(Tables.REDIRECT)
                    .where(Tables.REDIRECT.SRC_PAGE_ID.equal(id))
                    .and(Tables.REDIRECT.LANG_ID.equal(lang.getId()))
                    .fetchOne();
            if (record == null){
                return null;
            }
            return record.getValue(Tables.REDIRECT.DEST_PAGE_ID);
        } catch (SQLException e){
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public boolean isRedirect(Language lang, int id) throws DaoException {
        Connection conn=null;
        try{
        conn = ds.getConnection();
            DSLContext context = DSL.using(conn,dialect);
            Record record = context.select().from(Tables.REDIRECT)
                    .where(Tables.REDIRECT.SRC_PAGE_ID.equal(id))
                    .and(Tables.REDIRECT.LANG_ID.equal(lang.getId()))
                    .fetchOne();
            return (record != null);
        } catch (SQLException e){
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public TIntSet getRedirects(LocalPage localPage) throws DaoException {
        Connection conn=null;
        try{
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Result<Record> result = context.select().
                    from(Tables.REDIRECT).
                    where(Tables.REDIRECT.DEST_PAGE_ID.eq(localPage.getLocalId())).
                    and(Tables.REDIRECT.LANG_ID.eq(localPage.getLanguage().getId())).
                    fetch();
            TIntSet ids = new TIntHashSet();
            for (Record record : result){
                ids.add(record.getValue(Tables.REDIRECT.SRC_PAGE_ID));
            }
            return ids;
        } catch (SQLException e){
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public TIntIntMap getAllRedirectIdsToDestIds(Language lang) throws DaoException {
        Connection conn = null;
        try{
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn,dialect);
            Cursor<Record> cursor = context.select().
                    from(Tables.REDIRECT).
                    where(Tables.REDIRECT.LANG_ID.equal(lang.getId())).
                    fetchLazy();
            TIntIntMap ids = new TIntIntHashMap(
                    gnu.trove.impl.Constants.DEFAULT_CAPACITY,
                    gnu.trove.impl.Constants.DEFAULT_LOAD_FACTOR,
                    -1, -1);
            for (Record record : cursor){
                ids.put(record.getValue(Tables.REDIRECT.SRC_PAGE_ID),
                        record.getValue(Tables.REDIRECT.DEST_PAGE_ID));
            }
            return ids;
        } catch (SQLException e){
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    private Redirect buildRedirect(Record r) {
        if (r == null){
            return null;
        }
        return new Redirect(
                Language.getById(r.getValue(Tables.REDIRECT.LANG_ID)),
                r.getValue(Tables.REDIRECT.SRC_PAGE_ID),
                r.getValue(Tables.REDIRECT.DEST_PAGE_ID)
        );
    }

    public static class Provider extends org.wikapidia.conf.Provider<RedirectSqlDao>  {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return RedirectDao.class;
        }

        @Override
        public String getPath() {
            return "dao.redirect";
        }

        @Override
        public RedirectSqlDao get(String name, Config config) throws ConfigurationException {
            if (!config.getString("type").equals("sql")){
                return null;
            }
            try {
                return new RedirectSqlDao(
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
