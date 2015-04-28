package org.wikibrain.pageview;

import com.typesafe.config.Config;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.jooq.*;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.dao.sql.AbstractSqlDao;
import org.wikibrain.core.dao.sql.JooqUtils;
import org.wikibrain.core.dao.sql.SimpleSqlDaoIterable;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.jooq.Tables;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;

import java.io.File;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Eventually this should implement a PageViewDao interface.
 *
 * @author Shilad Sen
 */
public class PageViewSqlDao extends AbstractSqlDao<PageView> implements PageViewDao {
    public static final String LOADED_CACHE_KEY = "pageviewhours";
    private final File downloadDir;
    private final LocalPageDao pageDao;
    private final MetaInfoDao metaDao;

    private static final TableField [] INSERT_FIELDS = new TableField[] {
            Tables.PAGEVIEW.LANG_ID,
            Tables.PAGEVIEW.PAGE_ID,
            Tables.PAGEVIEW.TSTAMP,
            Tables.PAGEVIEW.NUM_VIEWS,
    };

    /**
     * @param dataSource      Data source for jdbc connections
     * @throws org.wikibrain.core.dao.DaoException
     */
    public PageViewSqlDao(WpDataSource dataSource, MetaInfoDao metaDao, LocalPageDao pageDao, File downloadDir) throws DaoException {
        super(dataSource, INSERT_FIELDS, "/db/pageview");
        this.downloadDir = downloadDir;
        this.pageDao = pageDao;
        this.metaDao = metaDao;
    }

    @Override
    public void clear() throws DaoException {
        super.clear();
        cache.remove(LOADED_CACHE_KEY);
    }

    @Override
    public void save(PageView view) throws DaoException {
        insert(
                view.getPageId().getLanguage().getId(),
                view.getPageId().getId(),
                view.getHour(),
                view.getViews()
        );
    }

    @Override
    public TIntIntMap getAllViews(Language language, DateTime startDate, DateTime endDate) throws DaoException {
        DSLContext context = getJooq();
        Timestamp startTime = new Timestamp(startDate.getMillis());
        Timestamp endTime = new Timestamp(endDate.getMillis());
        try {
            Cursor<Record> result = context.select().
                    from(Tables.PAGEVIEW).
                    where(Tables.PAGEVIEW.LANG_ID.eq(language.getId())).
                    and(Tables.PAGEVIEW.TSTAMP.between(startTime, endTime)).
                    fetchLazy(getFetchSize());
            TIntIntMap views = new TIntIntHashMap(
                    gnu.trove.impl.Constants.DEFAULT_CAPACITY,
                    gnu.trove.impl.Constants.DEFAULT_LOAD_FACTOR,
                    -1, -1);
            for (Record record : result){
                views.adjustOrPutValue(
                        record.getValue(Tables.PAGEVIEW.PAGE_ID),
                        record.getValue(Tables.PAGEVIEW.NUM_VIEWS),
                        record.getValue(Tables.PAGEVIEW.NUM_VIEWS));
            }
            return views;
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public int getNumViews(LocalId pageId, DateTime startDate, int numberOfHours) throws DaoException {
        return getNumViews(pageId, startDate, startDate.plusHours(numberOfHours));
    }

    @Override
    public int getNumViews(Language lang, int pageId, DateTime startDate, int numberOfHours) throws DaoException {
        return getNumViews(new LocalId(lang, pageId), startDate, startDate.plusHours(numberOfHours));
    }

    @Override
    public int getNumViews(Language lang, int pageId, DateTime startDate, DateTime endDate) throws DaoException {
        return getNumViews(new LocalId(lang, pageId), startDate, endDate);
    }

    @Override
    public int getNumViews(LocalId pageId, DateTime startDate, DateTime endDate) throws DaoException {
        DSLContext context = getJooq();
        Timestamp startTime = new Timestamp(startDate.getMillis());
        Timestamp endTime = new Timestamp(endDate.getMillis());
        try {
            Cursor<Record> result = context.select().
                    from(Tables.PAGEVIEW).
                    where(Tables.PAGEVIEW.LANG_ID.eq(pageId.getLanguage().getId())).
                    and(Tables.PAGEVIEW.TSTAMP.between(startTime, endTime)).
                    and(Tables.PAGEVIEW.PAGE_ID.eq(pageId.getId())).
                    fetchLazy(getFetchSize());
            int numViews = 0;
            for (Record record : result){
                numViews += record.getValue(Tables.PAGEVIEW.NUM_VIEWS);
            }
            return numViews;
        } finally {
            freeJooq(context);
        }
    }

    @Override
    public Map<Integer, Integer> getNumViews(Language lang, Iterable<Integer> ids, DateTime startTime, DateTime endTime) throws ConfigurationException, DaoException{
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        for(Integer id: ids){
            result.put(id, getNumViews(new LocalId(lang, id), startTime, endTime));
        }
        return result;
    }
    @Override
    public Map<Integer, Integer> getNumViews(Language lang, Iterable<Integer> ids, ArrayList<DateTime[]> dates) throws ConfigurationException, DaoException{
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        DateTime startTime;
        DateTime endTime;
        int count = 0;
        for (DateTime[] date : dates){
            startTime = date[0];
            endTime = date[1];
            count++;
            for(Integer id : ids){
                if(!result.keySet().contains(id))
                {
                    result.put(id, getNumViews(new LocalId(lang, id), startTime, endTime));
                }
                else{
                    int totalViews = result.get(id) + getNumViews(new LocalId(lang, id), startTime, endTime);
                    result.put(id, totalViews);
                }
            }
            LOG.info(count + " dates loaded");
        }

        return result;
    }

    /**
     * Returns all pageviews that meet the filter criteria specified by an input PageViewDaoFilter
     *
     * @see org.wikibrain.pageview.PageViewSqlDao#get(org.wikibrain.core.dao.DaoFilter) for a typical example
     *
     * @param daoFilter a set of filters to limit the search
     *                  must be a PageViewDaoFilter or DaoException will be thrown
     * @return
     * @throws DaoException
     */
    @Override
    public Iterable<PageView> get(final DaoFilter daoFilter) throws DaoException {
        if (!(daoFilter instanceof PageViewDaoFilter)) {
            throw new DaoException("Need to input PageViewDaoFilter for PageViewSqlDao get method");
        }
        PageViewDaoFilter pDaoFilter = (PageViewDaoFilter) daoFilter;
        DSLContext context = getJooq();
        try {
            Collection<Condition> conditions = new ArrayList<Condition>();
            if (pDaoFilter.getLangIds() != null) {
                conditions.add(Tables.PAGEVIEW.LANG_ID.in(pDaoFilter.getLangIds()));
            }
            if (pDaoFilter.getPageIds() != null) {
                conditions.add(Tables.PAGEVIEW.PAGE_ID.in(pDaoFilter.getPageIds()));
            }
            if (pDaoFilter.getMinNumViews() != null) {
                conditions.add(Tables.PAGEVIEW.NUM_VIEWS.greaterOrEqual(pDaoFilter.getMinNumViews()));
            }
            if (pDaoFilter.getMaxNumViews() != null) {
                conditions.add(Tables.PAGEVIEW.NUM_VIEWS.lessOrEqual(pDaoFilter.getMaxNumViews()));
            }
            if (pDaoFilter.getStartDate() != null) {
                conditions.add(Tables.PAGEVIEW.TSTAMP.greaterOrEqual(new Timestamp(pDaoFilter.getStartDate().getMillis())));
            }
            if (pDaoFilter.getEndDate() != null) {
                conditions.add(Tables.PAGEVIEW.TSTAMP.lessOrEqual(new Timestamp(pDaoFilter.getEndDate().getMillis())));
            }
            Cursor<Record> result = context.select().
                    from(Tables.PAGEVIEW).
                    where(conditions).
                    limit(daoFilter.getLimitOrInfinity()).
                    fetchLazy(getFetchSize());
            return new SimpleSqlDaoIterable<PageView>(result, context) {
                @Override
                public PageView transform(Record r) {
                    try {
                        return buildPageView(r);
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

    /**
     * Shilad: I'm not sure this makes sense for this dao.
     * If implemented, it should return the number of rows (i.e. pages and hours)
     * that match the specified query.
     *
     * @param daoFilter a set of filters to limit the search
     * @return
     * @throws DaoException
     */
    @Override
    public int getCount(DaoFilter daoFilter) throws DaoException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void ensureLoaded(DateTime start, DateTime end, LanguageSet langs) throws DaoException {
        ensureLoaded(Arrays.asList(new Interval(start, end)), langs);
    }

    @Override
    public synchronized void ensureLoaded(List<Interval> intervals, final LanguageSet langs) throws DaoException {
        // FIXME: At the moment we totally ignore the language setting
        final Map<Language, SortedSet<DateTime>> loaded = getLoadedHours();

        SortedSet<DateTime> needed = new TreeSet<DateTime>();
        for (Interval i : intervals) {
            for (DateTime tstamp : PageViewUtils.timestampsInInterval(i.getStart(), i.getEnd())) {
                for (Language l : langs) {
                    if (!loaded.containsKey(l) || !loaded.get(l).contains(tstamp)) {
                        needed.add(tstamp);
                    }
                }
            }
        }

        if (needed.isEmpty()) {
            LOG.info("All requested page views are loaded.");
            return;
        }

        LOG.info(String.format("Loading pageviews for %d timestamps between %s and %s",
                    needed.size(), needed.first().toString(), needed.last().toString()));


        PageViewDownloader downloader = new PageViewDownloader(downloadDir);
        final TreeMap<DateTime, File> toLoad;
        try {
            toLoad = downloader.download(needed);
        } catch (WikiBrainException e) {
            throw new DaoException(e);
        }

        beginLoad();

        final AtomicInteger[] counters = new AtomicInteger[] { new AtomicInteger(), new AtomicInteger() };

        ParallelForEach.loop(toLoad.keySet(), new Procedure<DateTime>() {
            @Override
            public void call(DateTime tstamp) throws Exception {
                LOG.info("loading pageview file " + toLoad.get(tstamp));
                loadOneFile(tstamp, toLoad.get(tstamp), langs, counters);
                LOG.info("finished pageview file " + toLoad.get(tstamp));
            }
        });


        endLoad();

        LOG.info(String.format("Found %d pageviews for langs %s and resolved %d of them.",
                counters[0].get(), langs, counters[1].get()));

        // Make sure one second passes between the last view loaded and the save of the cached info
        // Otherwise we may incorrectly think the cache is stale.
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        for (Language lang : langs) {
            if (!loaded.containsKey(lang)) {
                loaded.put(lang, new TreeSet<DateTime>());
            }
            for (DateTime tstamp : needed) {
                loaded.get(lang).add(tstamp);
            }
        }
        cache.put(LOADED_CACHE_KEY, loaded);
    }

    private void loadOneFile(DateTime tstamp, File file, LanguageSet langs, AtomicInteger[] counters) {
        PageViewReader reader = new PageViewReader(file, langs);
        for (RawPageView view : reader) {
            try {
                counters[0].getAndIncrement();
                int id = pageDao.getIdByTitle(view.getTitle());
                if (id >= 0) {
                    counters[1].incrementAndGet();
                    PageView pv = new PageView(
                            new LocalId(view.getLanguage(), id),
                            tstamp.toDate(),
                            view.getViews());
                    save(pv);
                    metaDao.incrementRecords(PageView.class, pv.getPageId().getLanguage());
                }
            } catch (DaoException e) {
                metaDao.incrementErrorsQuietly(PageView.class);
                e.printStackTrace();
            }
        }
    }

    public synchronized  Map<Language, SortedSet<DateTime>> getLoadedHours() throws DaoException {
        DSLContext context = getJooq();
        try {
            if (!JooqUtils.tableExists(context, Tables.PAGEVIEW)) {
                return new HashMap<Language, SortedSet<DateTime>>();
            }
            Map<Language, SortedSet<DateTime>> loaded = (Map<Language, SortedSet<DateTime>>) cache.get(LOADED_CACHE_KEY, PageView.class);
            if (loaded != null) {
                return loaded;
            }

            LOG.info("creating loadedHours cache. This only happens once...");
            loaded = new HashMap<Language, SortedSet<DateTime>>();
            Result<Record2<Timestamp, Short>> times = context
                    .selectDistinct(Tables.PAGEVIEW.TSTAMP,Tables.PAGEVIEW.LANG_ID)
                    .from(Tables.PAGEVIEW)
                    .fetch();

            for (Record2<Timestamp, Short> record: times){
                Language lang = Language.getById(record.value2());
                DateTime date = new DateTime(record.value1());
                if (!loaded.containsKey(lang)) {
                    loaded.put(lang, new TreeSet<DateTime>());
                }
                loaded.get(lang).add(date);
            }
            cache.put(LOADED_CACHE_KEY, loaded);
            return loaded;
        } finally {
            freeJooq(context);
        }
    }


    protected PageView buildPageView(Record record) throws DaoException {
        if (record == null) {
            return null;
        }
        LocalId id = new LocalId(
                Language.getById(record.getValue(Tables.PAGEVIEW.LANG_ID)),
                record.getValue(Tables.PAGEVIEW.PAGE_ID)
        );
        return new PageView(
                id,
                record.getValue(Tables.PAGEVIEW.TSTAMP),
                record.getValue(Tables.PAGEVIEW.NUM_VIEWS)
        );
    }

    public static class Provider extends org.wikibrain.conf.Provider<PageViewDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return PageViewDao.class;
        }

        @Override
        public String getPath() {
            return "dao.pageView";
        }

        @Override
        public PageViewDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                PageViewSqlDao dao = new PageViewSqlDao(
                        getConfigurator().get(
                                WpDataSource.class,
                                config.getString("dataSource")),
                        getConfigurator().get(MetaInfoDao.class),
                        getConfigurator().get(LocalPageDao.class),
                        new File(config.getString("dir"))
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
