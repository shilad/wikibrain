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
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class LocalPageDao<T extends LocalPage> {
    public static final Logger LOG = Logger.getLogger(LocalPageDao.class.getName());

    protected final SQLDialect dialect;
    protected DataSource ds;

    /**
     *
     * @param dataSource
     * @throws DaoException
     */
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

    /**
     * Get a single page by its title
     * @param language the page's language
     * @param title the page's title
     * @param ns the page's namespace
     * @return the requested LocalPage
     * @throws DaoException if there was an error retrieving the page
     */
    public abstract T getByTitle(Language language, Title title, PageType ns) throws DaoException;

    /**
     * Get a single page by its title
     * @param language the page's language
     * @param pageId the page's id
     * @return the requested LocalPage
     * @throws DaoException if there was an error retrieving the page
     */
    public abstract T getByPageId(Language language, int pageId) throws DaoException;

    /**
     * Get a set of pages by their ids
     * @param language the language of the pages
     * @param pageIds a Collection of page ids
     * @return a map of ids to pages
     * @throws DaoException if there was an error retrieving the pages
     */
    public Map<Integer, T> getByIds(Language language, Collection<Integer> pageIds) throws DaoException {
        Map<Integer, T> map = new HashMap<Integer,T>();
        for (int id : pageIds){
            map.put(id, getByPageId(language,id));
        }
        return map;
    }

    /**
     * Get a set of pages by their titles
     * @param language the language of the pages
     * @param titles a Collection of page titles
     * @param ns the namespace of the pages
     * @return a map of titles to pages
     * @throws DaoException if there was an error retrieving the pages
     */
    public Map<Title, T> getByTitles(Language language, Collection<Title> titles, PageType ns) throws DaoException{
        Map<Title, T> map = new HashMap<Title, T>();
        for (Title title : titles){
            map.put(title, getByTitle(language, title, ns));
        }
        return map;
    }

    /**
     * Close a connection without generating an exception if it fails.
     * @param conn
     */
    public static void quietlyCloseConn(Connection conn) {
        if (conn != null) {
            try { conn.close(); }
            catch (SQLException e) {
                LOG.log(Level.WARNING, "Failed to close connection: ", e);
            }
        }
    }
}
