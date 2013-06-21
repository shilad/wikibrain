package org.wikapidia.core.dao.sql;

import com.typesafe.config.Config;
import gnu.trove.impl.Constants;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.IOUtils;
import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.RedirectDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 */
public class RedirectSqlDao extends AbstractSqlDao implements RedirectDao{

    public RedirectSqlDao(DataSource dataSource) throws DaoException {
        super(dataSource);
    }

    public void beginLoad() throws DaoException{
        Connection conn=null;
        try {
            conn = ds.getConnection();
            conn.createStatement().execute(
                    IOUtils.toString(
                            RedirectSqlDao.class.getResource("/db/redirect-schema.sql")
                    )
            );
        } catch (IOException e){
            throw new DaoException(e);
        } catch (SQLException e){
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    public void endLoad() throws DaoException{
        Connection conn = null;
        try {
            conn = ds.getConnection();
            conn.createStatement().execute(
                    IOUtils.toString(
                            LocalPageSqlDao.class.getResource("/db/redirect-indexes.sql")
                    ));
            if (cache!=null){
                cache.updateTableLastModified(Tables.REDIRECT.getName());
            }
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (SQLException e){
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
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
            DSLContext context = DSL.using(conn,dialect);
            Result<Record> result = context.select().from(Tables.REDIRECT)
                    .where(Tables.REDIRECT.DEST_PAGE_ID.equal(localPage.getLocalId()))
                    .and(Tables.REDIRECT.LANG_ID.equal(localPage.getLanguage().getId()))
                    .fetch();
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
        Connection conn=null;
        try{
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn,dialect);
            Cursor<Record> cursor = context.select().from(Tables.REDIRECT)
                    .where(Tables.REDIRECT.LANG_ID.equal(lang.getId()))
                    .fetchLazy();
            TIntIntMap ids = new TIntIntHashMap(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1, -1);
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

    public void save(Language lang, int src, int dest) throws DaoException {
        Connection conn=null;
        try{
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn,dialect);
            context.insertInto(Tables.REDIRECT).values(
                    lang.getId(),
                    src,
                    dest
            ).execute();

        } catch (SQLException e){
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
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
