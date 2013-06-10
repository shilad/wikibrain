package org.wikapidia.core.dao;

import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.PageType;
import org.wikapidia.core.model.Title;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class LocalPageDao<T extends LocalArticle> {

<<<<<<< HEAD
    public abstract T getByPageId(Language language, int pageId);
=======
    public LocalPageDao(DataSource dataSource) throws SQLException {
        ds = dataSource;
        Connection conn = ds.getConnection();
        try {
            this.dialect = JooqUtils.dialect(conn);
        } finally {
            conn.close();
        }
    }

    public LocalPage get(Language lang, int localId) throws SQLException {
        Connection conn = ds.getConnection();
        try {
            DSLContext context = DSL.using(conn, dialect);
            Record record = context.select().
                    from(Tables.LOCAL_PAGE).
                    where(Tables.LOCAL_PAGE.PAGE_ID.equal(localId)).
                    and(Tables.LOCAL_PAGE.LANG_ID.equal((short) lang.getId())).
                    fetchOne();
            return buildPage(record);
        } finally {
            conn.close();
        }
    }

    public LocalPage get(Title title, PageType pageType) throws SQLException {
        return get(title, pageType.getNamespace());
    }

    /**
     * @param title
     * @param ns
     * @return
     */
    public LocalPage get(Title title, PageType.NameSpace ns) throws SQLException {
        Connection conn = ds.getConnection();
        try {
            DSLContext context = DSL.using(conn, dialect);
            Record record = context.select().
                    from(Tables.LOCAL_PAGE).
                    where(Tables.LOCAL_PAGE.TITLE.equal(title.getCanonicalTitle())).
                    and(Tables.LOCAL_PAGE.LANG_ID.equal((short) title.getLanguage().getId())).
                    and(Tables.LOCAL_PAGE.NS.equal((short)ns.getValue())).
                    fetchOne();
            return buildPage(record);
        } finally {
            conn.close();
        }
    }
>>>>>>> ed66aceb33b9265c95eb9ab41c86e29baaf376aa

    public abstract T getByTitle(Language language, Title title, PageType ns);

    public Map<Integer, T> getByIds(Language language, Collection<Integer> pageIds) {
        Map<Integer, T> map = new HashMap<Integer,T>();
        for (int id : pageIds){
            map.put(id,getByPageId(language,id));
        }
        return map;
    }

    public Map<Title, T> getbyTitles(Language language, Collection<Title> titles, PageType ns){
        Map<Title, T> map = new HashMap<Title, T>();
        for (Title title : titles){
            map.put(title, getByTitle(language, title, ns));
        }
        return map;
    }
}
