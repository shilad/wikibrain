package org.wikapidia.core.dao.sql;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.RedirectDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 */
public class RedirectSqlDao extends AbstractSqlDao implements RedirectDao{

    public RedirectSqlDao(DataSource dataSource) throws DaoException {
        super(dataSource);
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
            TIntIntMap ids = new TIntIntHashMap();
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
            );
        } catch (SQLException e){
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public void update(Language lang, int src, int newDest) throws DaoException {
        Connection conn = null;
        try{
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            int n = context.update(Tables.REDIRECT)
                    .set(Tables.REDIRECT.DEST_PAGE_ID, newDest)
                    .where(Tables.REDIRECT.SRC_PAGE_ID.equal(src))
                    .and(Tables.REDIRECT.LANG_ID.equal(lang.getId()))
                    .execute();
            if (n == 0) {
                n = context.insertInto(Tables.REDIRECT, Tables.REDIRECT.LANG_ID, Tables.REDIRECT.SRC_PAGE_ID,
                        Tables.REDIRECT.DEST_PAGE_ID)
                        .values(lang.getId(), src, newDest)
                        .execute();
            }
        }catch (SQLException e){
            throw new DaoException(e);
        }
    }

}
