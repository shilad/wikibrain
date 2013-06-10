package org.wikapidia.core.dao;

import org.jooq.SQLDialect;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.PageType;
import org.wikapidia.core.model.Title;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class LocalPageDao<T extends LocalPage> {
    protected final SQLDialect dialect;
    protected DataSource ds;

    public LocalPageDao(DataSource dataSource) throws DaoException {
        ds = dataSource;
        Connection conn = null;
        try {
            conn = ds.getConnection();
            this.dialect = JooqUtils.dialect(conn);
        } catch (SQLException e) { throw new DaoException(e);
        } finally { quietlyCloseConn(conn);
        }
    }

    public abstract T getByTitle(Language language, Title title, PageType ns) throws DaoException;

    public abstract T getByPageId(Language language, int pageId) throws DaoException;

    public Map<Integer, T> getByIds(Language language, Collection<Integer> pageIds) throws DaoException {
        Map<Integer, T> map = new HashMap<Integer,T>();
        for (int id : pageIds){
            map.put(id, getByPageId(language,id));
        }
        return map;
    }

    public Map<Title, T> getByTitles(Language language, Collection<Title> titles, PageType ns) throws DaoException{
        Map<Title, T> map = new HashMap<Title, T>();
        for (Title title : titles){
            map.put(title, getByTitle(language, title, ns));
        }
        return map;
    }

    public static void quietlyCloseConn(Connection conn) throws DaoException {
        if (conn != null) {
            try { conn.close(); }
            catch (SQLException e) {
                throw new DaoException(e);
            }
        }
    }
}
