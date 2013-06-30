package org.wikapidia.core.dao.sql;

import com.typesafe.config.Config;
import gnu.trove.impl.Constants;
import gnu.trove.map.hash.TLongIntHashMap;
import org.apache.commons.io.IOUtils;
import org.jooq.Condition;
import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 */
public class LocalPageSqlDao<T extends LocalPage> extends AbstractSqlDao implements LocalPageDao<T> {
    private volatile TLongIntHashMap titlesToIds = null;
    private RedirectSqlDao redirectSqlDao;

    public LocalPageSqlDao(DataSource dataSource) throws DaoException {
        this(dataSource, true);
    }

    public LocalPageSqlDao(DataSource dataSource, boolean followRedirects) throws DaoException{
        super(dataSource);
        if (followRedirects){
            redirectSqlDao = new RedirectSqlDao(ds);
        }
    }

    @Override
    public void beginLoad() throws DaoException {
        Connection conn=null;
        try {
            conn = ds.getConnection();
            conn.createStatement().execute(
                    IOUtils.toString(
                            LocalPageSqlDao.class.getResource("/db/local-page-schema.sql")
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
    public void save(LocalPage page) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            context.insertInto(Tables.LOCAL_PAGE).values(
                    null,
                    page.getLanguage().getId(),
                    page.getLocalId(),
                    page.getTitle().getCanonicalTitle(),
                    page.getNameSpace().getArbitraryId(),
                    page.isRedirect(),
                    page.isDisambig()
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
                            LocalPageSqlDao.class.getResource("/db/local-page-indexes.sql")
                    ));
            if (cache!=null){
                cache.updateTableLastModified(Tables.LOCAL_PAGE.getName());
            }
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (SQLException e){
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    // This iterable can contain null entries, which originate from the cursor.
    // TODO: Investigate this issue
    @Override
    public Iterable<T> get(DaoFilter daoFilter) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Collection<Condition> conditions = new ArrayList<Condition>();
            if (daoFilter.getLangIds() != null) {
                conditions.add(Tables.LOCAL_PAGE.LANG_ID.in(daoFilter.getLangIds()));
            }
            if (daoFilter.getNameSpaceIds() != null) {
                conditions.add(Tables.LOCAL_PAGE.NAME_SPACE.in(daoFilter.getNameSpaceIds()));
            }
            if (daoFilter.isRedirect() != null) {
                conditions.add(Tables.LOCAL_PAGE.IS_REDIRECT.in(daoFilter.isRedirect()));
            }
            if (daoFilter.isDisambig() != null) {
                conditions.add(Tables.LOCAL_PAGE.IS_DISAMBIG.in(daoFilter.isDisambig()));
            }
            if (conditions.isEmpty()) {
                return null;
            }
            Cursor<Record> result = context.select().
                    from(Tables.LOCAL_PAGE).
                    where(conditions).
                    fetchLazy(getFetchSize());
            return new SqlDaoIterable<T>(result) {
                @Override
                public T transform(Record r) {
                    try {
                        return (T)buildLocalPage(r);
                    } catch (DaoException e) {
                        LOG.log(Level.WARNING, e.getMessage(), e);
                        return null;
                    }
                }
            };
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public T getById(Language language, int pageId) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Record record = context.select().
                    from(Tables.LOCAL_PAGE).
                    where(Tables.LOCAL_PAGE.PAGE_ID.eq(pageId)).
                    and(Tables.LOCAL_PAGE.LANG_ID.eq(language.getId())).
                    fetchOne();
            LocalPage page = buildLocalPage(record);
            return (T)page;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public void setFollowRedirects(boolean followRedirects) throws DaoException {
        redirectSqlDao = new RedirectSqlDao(ds);
    }

    @Override
    public T getByTitle(Language language, Title title, NameSpace nameSpace) throws DaoException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Record record = context.select().
                    from(Tables.LOCAL_PAGE).
                    where(Tables.LOCAL_PAGE.TITLE.eq(title.getCanonicalTitle())).
                    and(Tables.LOCAL_PAGE.LANG_ID.eq(title.getLanguage().getId())).
                    and(Tables.LOCAL_PAGE.NAME_SPACE.eq(nameSpace.getArbitraryId())).
                    fetchOne();
            LocalPage page = buildLocalPage(record);
            return (T)page;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    @Override
    public Map<Integer, T> getByIds(Language language, Collection<Integer> pageIds) throws DaoException {
        if (pageIds == null || pageIds.isEmpty()) {
            return null;
        }
        Map<Integer, T> map = new HashMap<Integer, T>();
        for (Integer pageId : pageIds){
            map.put(pageId, getById(language, pageId));
        }
        return map;
    }

    @Override
    public Map<Title, T> getByTitles(Language language, Collection<Title> titles, NameSpace nameSpace) throws DaoException {
        if (titles == null || titles.isEmpty()) {
            return null;
        }
        Map<Title, T> map = new HashMap<Title, T>();
        for (Title title : titles){
            map.put(title, getByTitle(language, title, nameSpace));
        }
        return map;
    }

    public int getIdByTitle(String title, Language language, NameSpace nameSpace) throws DaoException {
        if (titlesToIds==null){
            buildTitlesToIds();
        }
        return titlesToIds.get(hashTitle(title,language.getId(),nameSpace.ordinal()));
    }

    /**
     * Build a LocalPage from a database record representation.
     * Classes that extend class this should override this method.
     *
     * @param record a database record
     * @return a LocalPage representation of the given database record
     * @throws DaoException if the record is not a Page
     */
    protected LocalPage buildLocalPage(Record record) throws DaoException {
        if (record == null) {
            return null;
        }
        Language lang = Language.getById(record.getValue(Tables.LOCAL_PAGE.LANG_ID));
        if (record.getValue(Tables.LOCAL_PAGE.IS_REDIRECT)&&redirectSqlDao!=null){
            return getById(lang,
                    redirectSqlDao.resolveRedirect(lang,record.getValue(Tables.LOCAL_PAGE.PAGE_ID)));
        }
        Title title = new Title(
                record.getValue(Tables.LOCAL_PAGE.TITLE), true,
                LanguageInfo.getByLanguage(lang));
        NameSpace nameSpace = NameSpace.getNameSpaceById(record.getValue(Tables.LOCAL_PAGE.NAME_SPACE));
        return new LocalPage(
                lang,
                record.getValue(Tables.LOCAL_PAGE.PAGE_ID),
                title,
                nameSpace,
                record.getValue(Tables.LOCAL_PAGE.IS_REDIRECT),
                record.getValue(Tables.LOCAL_PAGE.IS_DISAMBIG)
        );
    }

    /**
     *
     * @param s The canonical title of the page.
     * @param lang_id
     * @param page_type_id
     * @return
     */
    protected long hashTitle(String s, int lang_id, int page_type_id){
        s = s+lang_id+page_type_id;
        long h = 1125899906842597L; //prime
        int len = s.length();
        for (int i = 0; i < len; i++) {
            h = 31*h + s.charAt(i);
        }
        return h;
    }

    protected long hashTitle(Title title, Language language, NameSpace nameSpace){
        return hashTitle(title.getCanonicalTitle(),language.getId(),nameSpace.ordinal());
    }

    protected synchronized void buildTitlesToIds() throws DaoException {
        if (titlesToIds != null) {
            return;
        }
        Connection conn = null;
        try {
            if (cache!=null) {
                String [] dependsOn = (redirectSqlDao == null)
                        ? new String[] { Tables.LOCAL_PAGE.getName() }
                        : new String[] { Tables.LOCAL_PAGE.getName(), Tables.REDIRECT.getName() };
                TLongIntHashMap map = (TLongIntHashMap)cache.get("titlesToIds", dependsOn);
                if (map!=null){
                    titlesToIds = map;
                    return;
                }
            }
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Cursor<Record> cursor = context.select().
                    from(Tables.LOCAL_PAGE).
                    fetchLazy();
            TLongIntHashMap map = new TLongIntHashMap(
                    Constants.DEFAULT_CAPACITY,
                    Constants.DEFAULT_LOAD_FACTOR,
                    -1, -1);
            int numRedirects = 0;
            int numResolved = 0;
            for (Record record : cursor){
                long hash = hashTitle(record.getValue(Tables.LOCAL_PAGE.TITLE),
                        record.getValue(Tables.LOCAL_PAGE.LANG_ID),
                        record.getValue(Tables.LOCAL_PAGE.NAME_SPACE));
                if (redirectSqlDao != null && record.getValue(Tables.LOCAL_PAGE.IS_REDIRECT)){
                    numRedirects++;
                    Integer dest = redirectSqlDao.resolveRedirect(
                            Language.getById(record.getValue(Tables.LOCAL_PAGE.LANG_ID)),
                            record.getValue(Tables.LOCAL_PAGE.PAGE_ID));
                    if (dest != null) {
                        numResolved++;
                        map.put(hash, dest);
                    }
                }
                else{
                    map.put(hash, record.getValue(Tables.LOCAL_PAGE.PAGE_ID));
                }
            }
            LOG.info("resolved " + numResolved + " of " + numRedirects + " redirects.");
            if (cache!=null){
                cache.saveToCache("titlesToIds", map);
            }
            titlesToIds = map;
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            quietlyCloseConn(conn);
        }
    }

    public static class Provider extends org.wikapidia.conf.Provider<LocalPageDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return LocalPageDao.class;
        }

        @Override
        public String getPath() {
            return "dao.localPage";
        }

        @Override
        public LocalPageDao get(String name, Config config) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                return new LocalPageSqlDao(
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
