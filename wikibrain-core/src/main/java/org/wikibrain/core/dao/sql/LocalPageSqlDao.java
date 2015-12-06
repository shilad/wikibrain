package org.wikibrain.core.dao.sql;

import com.typesafe.config.Config;
import gnu.trove.impl.Constants;
import gnu.trove.map.hash.TLongIntHashMap;
import org.jooq.*;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.jooq.Tables;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.Title;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpThreadUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class LocalPageSqlDao extends AbstractSqlDao<LocalPage> implements LocalPageDao {
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
    public Iterable<LocalPage> get(final DaoFilter daoFilter) throws DaoException {
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
                    limit(daoFilter.getLimitOrInfinity()).
                    fetchLazy(getFetchSize());
            return new SimpleSqlDaoIterable<LocalPage>(result, context) {
                @Override
                public LocalPage transform(Record r) {
                    try {
                        return buildLocalPage(r, daoFilter);
                    } catch (DaoException e) {
                        LOG.warn(e.getMessage(), e);
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
    public Set<LocalId> getIds(final DaoFilter daoFilter) throws DaoException {
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
            Cursor<Record2<Short, Long>> result = context.select(Tables.LOCAL_PAGE.LANG_ID, Tables.LOCAL_PAGE.ID).
                    from(Tables.LOCAL_PAGE).
                    where(conditions).
                    limit(daoFilter.getLimitOrInfinity()).
                    fetchLazy(getFetchSize());
            Set<LocalId> ids = new HashSet<LocalId>();
            for (Record2<Short, Long> record : result) {
                ids.add(
                        new LocalId(Language.getById(record.value1()),
                        record.value2().intValue()));
            }
            return ids;
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
    public LocalPage getById(Language language, int pageId) throws DaoException {
        DSLContext context = getJooq();
        try {
            Record record = context.select().
                    from(Tables.LOCAL_PAGE).
                    where(Tables.LOCAL_PAGE.PAGE_ID.eq(pageId)).
                    and(Tables.LOCAL_PAGE.LANG_ID.eq(language.getId())).
                    limit(1).
                    fetchOne();
            return buildLocalPage(record);
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public LocalPage getById(LocalId localId) throws DaoException {
        return getById(localId.getLanguage(), localId.getId());
    }

    @Override
    public void setFollowRedirects(boolean followRedirects) throws DaoException {
        if (followRedirects){
            redirectSqlDao = new RedirectSqlDao(wpDs);
        } else {
            redirectSqlDao = null;
        }
        titlesToIds = null;
    }

    @Override
    public LocalPage getByTitle(Title title, NameSpace nameSpace) throws DaoException {
        DSLContext context = getJooq();
        try {
            Record record = context.select().
                    from(Tables.LOCAL_PAGE).
                    where(Tables.LOCAL_PAGE.TITLE.eq(title.getCanonicalTitle())).
                    and(Tables.LOCAL_PAGE.LANG_ID.eq(title.getLanguage().getId())).
                    and(Tables.LOCAL_PAGE.NAME_SPACE.eq(nameSpace.getArbitraryId())).
                    limit(1).
                    fetchOne();
            return buildLocalPage(record);
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public LocalPage getByTitle(Language language, NameSpace ns, String title) throws DaoException {
        return getByTitle(new Title(title, language), ns);
    }

    @Override
    public Map<Integer, LocalPage> getByIds(Language language, Collection<Integer> pageIds) throws DaoException {
        if (pageIds == null || pageIds.isEmpty()) {
            return null;
        }
        Map<Integer, LocalPage> map = new HashMap<Integer, LocalPage>();
        for (Integer pageId : pageIds){
            map.put(pageId, getById(language, pageId));
        }
        return map;
    }

    @Override
    public Map<Title, LocalPage> getByTitles(Language language, Collection<Title> titles, NameSpace nameSpace) throws DaoException {
        if (titles == null || titles.isEmpty()) {
            return null;
        }
        Map<Title, LocalPage> map = new HashMap<Title, LocalPage>();
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
        String key = "titlesToIds";
        if (redirectSqlDao == null) {
            key += ".noRedirect";
        }
        if (cache!=null) {
            TLongIntHashMap map = (TLongIntHashMap)cache.get(key, LocalPage.class);
            if (map!=null){
                titlesToIds = map;
                return;
            }
        }
        LOG.info("Building title to id cache. This will only happen once!");
        final int n = getCount(new DaoFilter());
        DSLContext context = getJooq();
        try {
            Cursor<Record> cursor = context.select().
                    from(Tables.LOCAL_PAGE).
                    fetchLazy(getFetchSize());
            final TLongIntHashMap map = new TLongIntHashMap(
                    Constants.DEFAULT_CAPACITY,
                    Constants.DEFAULT_LOAD_FACTOR,
                    -1, -1);
            final AtomicInteger numRedirects = new AtomicInteger();
            final AtomicInteger numResolved = new AtomicInteger();
            ParallelForEach.iterate(
                    cursor.iterator(), WpThreadUtils.getMaxThreads(), 10000,
                    new Procedure<Record>() {
                        @Override
                        public void call(Record record) throws Exception {
                            long hash = Title.longHashCode(
                                record.getValue(Tables.LOCAL_PAGE.LANG_ID),
                                record.getValue(Tables.LOCAL_PAGE.TITLE),
                                record.getValue(Tables.LOCAL_PAGE.NAME_SPACE));
                            if (redirectSqlDao != null && record.getValue(Tables.LOCAL_PAGE.IS_REDIRECT)){
                                numRedirects.incrementAndGet();
                                Integer dest = redirectSqlDao.resolveRedirect(
                                        Language.getById(record.getValue(Tables.LOCAL_PAGE.LANG_ID)),
                                        record.getValue(Tables.LOCAL_PAGE.PAGE_ID));
                                if (dest != null) {
                                    numResolved.incrementAndGet();
                                    synchronized (map) {
                                        map.put(hash, dest);
                                    }
                                }
                            } else {
                                synchronized (map) {
                                    map.put(hash, record.getValue(Tables.LOCAL_PAGE.PAGE_ID));
                                }
                            }
                            if (map.size() % 500000 == 0) {
                                LOG.info("built title cache entry " + map.size() + " of " + n);
                            }

                        }
                    }, Integer.MAX_VALUE);
            LOG.info("resolved " + numResolved + " of " + numRedirects + " redirects.");
            if (cache!=null){
                cache.put(key, map);
            }
            titlesToIds = map;
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public LocalPage getByTitle(Language lang, String title) throws DaoException {
        return getByTitle(lang, NameSpace.ARTICLE, title);
    }


    public static class Provider extends org.wikibrain.conf.Provider<LocalPageDao> {
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
        public LocalPageDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                LocalPageSqlDao dao = new LocalPageSqlDao(
                                    getConfigurator().get(
                                        WpDataSource.class,
                                        config.getString("dataSource"))
                                );
                String cachePath = getConfig().get().getString("dao.sqlCachePath");
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
