package org.wikapidia.core.dao.sql;

import com.typesafe.config.Config;
import gnu.trove.impl.Constants;
import gnu.trove.map.hash.TLongIntHashMap;
import org.jooq.*;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 */
public class LocalPageSqlDao<T extends LocalPage> extends AbstractSqlDao<T> implements LocalPageDao<T> {
    private volatile TLongIntHashMap titlesToIds = null;
    private RedirectSqlDao redirectSqlDao;

    public LocalPageSqlDao(WpDataSource dataSource) throws DaoException {
        this(dataSource, true);
    }

    public LocalPageSqlDao(WpDataSource dataSource, boolean followRedirects) throws DaoException{
        super(dataSource, INSERT_FIELDS, "/db/local-page");
        if (followRedirects){
            redirectSqlDao = new RedirectSqlDao(wpDs);
        }
    }

    private static final TableField [] INSERT_FIELDS = new TableField[] {
            Tables.LOCAL_PAGE.LANG_ID,
            Tables.LOCAL_PAGE.PAGE_ID,
            Tables.LOCAL_PAGE.TITLE,
            Tables.LOCAL_PAGE.NAME_SPACE,
            Tables.LOCAL_PAGE.IS_REDIRECT,
            Tables.LOCAL_PAGE.IS_DISAMBIG
    };

    @Override
    public void save(LocalPage page) throws DaoException {
        insert(
                page.getLanguage().getId(),
                page.getLocalId(),
                page.getTitle().getCanonicalTitle(),
                page.getNameSpace().getArbitraryId(),
                page.isRedirect(),
                page.isDisambig()
        );
    }

    @Override
    public Iterable<T> get(final DaoFilter daoFilter) throws DaoException {
        DSLContext context = getJooq();
        try {
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
            Cursor<Record> result = context.select().
                    from(Tables.LOCAL_PAGE).
                    where(conditions).
                    fetchLazy(getFetchSize());
            return new SimpleSqlDaoIterable<T>(result, context) {
                @Override
                public T transform(Record r) {
                    try {
                        return (T)buildLocalPage(r, daoFilter);
                    } catch (DaoException e) {
                        LOG.log(Level.WARNING, e.getMessage(), e);
                        return null;
                    }
                }
            };
        } catch (RuntimeException e) {
            freeJooq(context);
            throw e;
        }
    }

    @Override
    public int getCount(DaoFilter daoFilter) throws DaoException{
        DSLContext context = getJooq();
        try {
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
            return context.selectCount().
                    from(Tables.LOCAL_PAGE).
                    where(conditions).
                    fetchOne().value1();
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public T getById(Language language, int pageId) throws DaoException {
        DSLContext context = getJooq();
        try {
            Record record = context.select().
                    from(Tables.LOCAL_PAGE).
                    where(Tables.LOCAL_PAGE.PAGE_ID.eq(pageId)).
                    and(Tables.LOCAL_PAGE.LANG_ID.eq(language.getId())).
                    fetchOne();
            LocalPage page = buildLocalPage(record);
            return (T)page;
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public void setFollowRedirects(boolean followRedirects) throws DaoException {
        if (followRedirects){
            redirectSqlDao = new RedirectSqlDao(wpDs);
        } else {
            redirectSqlDao = null;
        }
    }

    @Override
    public T getByTitle(Title title, NameSpace nameSpace) throws DaoException {
        DSLContext context = getJooq();
        try {
            Record record = context.select().
                    from(Tables.LOCAL_PAGE).
                    where(Tables.LOCAL_PAGE.TITLE.eq(title.getCanonicalTitle())).
                    and(Tables.LOCAL_PAGE.LANG_ID.eq(title.getLanguage().getId())).
                    and(Tables.LOCAL_PAGE.NAME_SPACE.eq(nameSpace.getArbitraryId())).
                    limit(1).
                    fetchOne();
            LocalPage page = buildLocalPage(record);
            return (T)page;
        } finally {
            freeJooq(context);
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
            map.put(title, getByTitle(title, nameSpace));
        }
        return map;
    }

    @Override
    public int getIdByTitle(String title, Language language, NameSpace nameSpace) throws DaoException {
        if (titlesToIds==null){
            buildTitlesToIds();
        }
        return titlesToIds.get(Title.longHashCode(language, title, nameSpace));
    }

    @Override
    public int getIdByTitle(Title title) throws DaoException {
        if (titlesToIds==null){
            buildTitlesToIds();
        }
        return titlesToIds.get(title.longHashCode());
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
        return buildLocalPage(record, new DaoFilter());
    }

    protected LocalPage buildLocalPage(Record record, DaoFilter daoFilter) throws DaoException {
        if (record == null) {
            return null;
        }
        Language lang = Language.getById(record.getValue(Tables.LOCAL_PAGE.LANG_ID));
        if (redirectSqlDao != null
                // either null or false
                // If true, we don't want to resolve redirects because they're all redirects
                && (daoFilter.isRedirect() == null || !daoFilter.isRedirect())
                && record.getValue(Tables.LOCAL_PAGE.IS_REDIRECT)) {
            LocalPage page = getById(lang, redirectSqlDao.resolveRedirect(
                    lang,
                    record.getValue(Tables.LOCAL_PAGE.PAGE_ID)));
            if (daoFilter.isValidLocalPage(page)) {
                return page;
            }
        }
        Title title = new Title(
                record.getValue(Tables.LOCAL_PAGE.TITLE), true,
                LanguageInfo.getByLanguage(lang));
        NameSpace nameSpace = NameSpace.getNameSpaceByArbitraryId(record.getValue(Tables.LOCAL_PAGE.NAME_SPACE));
        return new LocalPage(
                lang,
                record.getValue(Tables.LOCAL_PAGE.PAGE_ID),
                title,
                nameSpace,
                record.getValue(Tables.LOCAL_PAGE.IS_REDIRECT),
                record.getValue(Tables.LOCAL_PAGE.IS_DISAMBIG)
        );
    }

    protected synchronized void buildTitlesToIds() throws DaoException {
        if (titlesToIds != null) {
            return;
        }
        if (cache!=null) {
            TLongIntHashMap map = (TLongIntHashMap)cache.get("titlesToIds", LocalPage.class);
            if (map!=null){
                titlesToIds = map;
                return;
            }
        }
        LOG.info("Building title to id cache. This will only happen once!");
        int n = getCount(new DaoFilter());
        DSLContext context = getJooq();
        try {
            Cursor<Record> cursor = context.select().
                    from(Tables.LOCAL_PAGE).
                    fetchLazy(getFetchSize());
            TLongIntHashMap map = new TLongIntHashMap(
                    Constants.DEFAULT_CAPACITY,
                    Constants.DEFAULT_LOAD_FACTOR,
                    -1, -1);
            int numRedirects = 0;
            int numResolved = 0;
            for (Record record : cursor){
                long hash = Title.longHashCode(
                        record.getValue(Tables.LOCAL_PAGE.LANG_ID),
                        record.getValue(Tables.LOCAL_PAGE.TITLE),
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
                if (map.size() % 50000 == 0) {
                    LOG.info("built title cache entry " + map.size() + " of " + n);
                }
            }
            LOG.info("resolved " + numResolved + " of " + numRedirects + " redirects.");
            if (cache!=null){
                cache.put("titlesToIds", map);
            }
            titlesToIds = map;
        } finally {
            freeJooq(context);
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
                String cachePath = getConfig().get().getString("dao.sqlCachePath");
                LocalPageSqlDao dao = new LocalPageSqlDao(
                                    getConfigurator().get(
                                        WpDataSource.class,
                                        config.getString("dataSource"))
                                );
                File cacheDir = new File(cachePath);
                if (!cacheDir.isDirectory()) {
                    cacheDir.mkdirs();
                }
                dao.useCache(cacheDir);
                return dao;
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
