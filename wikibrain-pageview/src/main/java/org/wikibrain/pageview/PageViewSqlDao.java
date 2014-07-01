package org.wikibrain.pageview;

import com.typesafe.config.Config;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import org.joda.time.DateTime;
import org.jooq.*;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.sql.AbstractSqlDao;
import org.wikibrain.core.dao.sql.SimpleSqlDaoIterable;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.jooq.Tables;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.LocalId;

import java.io.File;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Eventually this should implement a PageViewDao interface.
 *
 * @author Shilad Sen
 */
public class PageViewSqlDao extends AbstractSqlDao<PageView> {

    Map<Integer, Set<Long>> loadedHours = new HashMap<Integer, Set<Long>>();

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
    public PageViewSqlDao(WpDataSource dataSource) throws DaoException {
        super(dataSource, INSERT_FIELDS, "/db/pageview");
//        setLoadedHours();
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

    /**
     * method to access a PageViewIterator via the DAO, can be used by clients to keep track of each of the PageViewDataStructs
     * retrieved by the iterator
     * @param langs
     * @param startDate
     * @param endDate
     * @return
     * @throws org.wikibrain.core.WikiBrainException
     * @throws DaoException
     */
    public PageViewIterator getPageViewIterator(LanguageSet langs, DateTime startDate, DateTime endDate, LocalPageDao lpDao) throws WikiBrainException, DaoException {
        return new PageViewIterator(langs, startDate, endDate, lpDao);
    }

    /**
     * adds all the page view entries in an input PageViewDataStruct to the SQL database
     * @param data
     * @throws DaoException
     */
    public void addData(PageViewDataStruct data) throws DaoException {
        TIntIntIterator it = data.stats.iterator();
        while (it.hasNext()) {
            it.advance();
            LocalId pageId = new LocalId(data.lang, it.key());
            Date hour = new Date(data.start.getMillis());
            PageView view = new PageView(pageId, hour, it.value());
            save(view);
            recordLoadedHours(view);
        }
    }

    protected void recordLoadedHours(PageView view) {
        int langId = view.getPageId().getLanguage().getId();
        Set<Long> hours = (loadedHours.get(langId) != null) ? loadedHours.get(langId) : new HashSet<Long>();
        hours.add(view.getHour().getTime());
        loadedHours.put(langId, hours);
    }

    public TIntIntMap getAllViews(Language language, DateTime startDate, DateTime endDate, LocalPageDao localPageDao) throws DaoException {
        checkLoaded(language, startDate, endDate, localPageDao);
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
                views.put(record.getValue(Tables.PAGEVIEW.PAGE_ID),
                        record.getValue(Tables.PAGEVIEW.NUM_VIEWS));
            }
            return views;
        } finally {
            freeJooq(context);
        }
    }

    public int getNumViews(Language language, int id, DateTime startDate, int numberOfHours, LocalPageDao localPageDao) throws DaoException {
        return getNumViews(language, id, startDate, startDate.plusHours(numberOfHours), localPageDao);
    }

    public int getNumViews(Language language, int id, DateTime startDate, DateTime endDate, LocalPageDao localPageDao) throws DaoException {
        checkLoaded(language, startDate, endDate, localPageDao);
        DSLContext context = getJooq();
        Timestamp startTime = new Timestamp(startDate.getMillis());
        Timestamp endTime = new Timestamp(endDate.getMillis());
        try {
            Cursor<Record> result = context.select().
                    from(Tables.PAGEVIEW).
                    where(Tables.PAGEVIEW.LANG_ID.eq(language.getId())).
                    and(Tables.PAGEVIEW.TSTAMP.between(startTime, endTime)).
                    and(Tables.PAGEVIEW.PAGE_ID.eq(id)).
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

    public Map<Integer, Integer> getNumViews(Language lang, Iterable<Integer> ids, DateTime startTime, DateTime endTime, LocalPageDao localPageDao) throws ConfigurationException, DaoException, WikiBrainException{
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        for(Integer id: ids){
            result.put(id, getNumViews(lang, id, startTime, endTime, localPageDao));
        }
        return result;
    }
    public Map<Integer, Integer> getNumViews(Language lang, Iterable<Integer> ids, ArrayList<DateTime[]> dates, LocalPageDao localPageDao) throws ConfigurationException, DaoException, WikiBrainException{
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        for(Integer id: ids){
            int sum=0;
            for(DateTime[] startEndTime: dates){
                sum+=getNumViews(lang,id,startEndTime[0],startEndTime[1],localPageDao);
            }
            result.put(id, sum);
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

    protected void checkLoaded(Language lang, DateTime startDate, DateTime endDate, LocalPageDao localPageDao) throws DaoException {
        setLoadedHours(); // Is this where this should go?
        List<DateTime> datesNotLoaded = new ArrayList<DateTime>();
        int langId = lang.getId();
        Set<Long> loadedHourSet = (loadedHours.containsKey(langId)) ? loadedHours.get(langId) : new HashSet<Long>();
        for (DateTime currentDate = startDate; currentDate.getMillis() < endDate.getMillis(); currentDate = currentDate.plusHours(1)) {
            if (!loadedHourSet.contains(currentDate.getMillis())) {
                datesNotLoaded.add(currentDate);
            }
        }
//       LOG.info("Number of timestamps not loaded " + datesNotLoaded.size());
        if(datesNotLoaded.size()!=0) {
            load(lang, datesNotLoaded, localPageDao);
        }
// LOG.info("Done downloading all timestamps needed");
    }

    private synchronized void setLoadedHours() throws DaoException{
        if (loadedHours.size() == 0) {
            loadedHours = new ConcurrentHashMap<Integer, Set<Long>>();
            LOG.info("creating loadedHours cache. This only happens once...");
            DSLContext context = getJooq();
            try {
                Result<Record2<Timestamp, Short>> times = context
                        .selectDistinct(Tables.PAGEVIEW.TSTAMP,Tables.PAGEVIEW.LANG_ID)
                        .from(Tables.PAGEVIEW)
                        .fetch();

                loadedHours= new HashMap<Integer, Set<Long>>();

                for (Record2<Timestamp, Short> record: times){
                    if(!loadedHours.containsKey(record.value2().intValue())){
                        loadedHours.put(record.value2().intValue(),new HashSet<Long>());
                    }
                    loadedHours.get(record.value2().intValue()).add(((Timestamp) record.value1()).getTime());

                }

            } finally {
                freeJooq(context);
            }
        }
    }

    protected void load(Language lang, List<DateTime> dates, LocalPageDao localPageDao) throws DaoException {
        beginLoad();
        PageViewLoader loader = new PageViewLoader(new LanguageSet(lang), this, localPageDao);
        int i = 0;
        while (i < dates.size()) {
            DateTime startDate = dates.get(i++);
            DateTime endDate = startDate.plusHours(1);
            while ((i < dates.size()) && (dates.get(i).equals(endDate))) {
                endDate = endDate.plusHours(1);
                i++;
            }
            try {
                loader.load(startDate, endDate);
            } catch (ConfigurationException cE) {
                System.out.println(cE.getMessage());
            } catch (WikiBrainException wE) {
                System.out.println(wE.getMessage());
            }
        }
        endLoad();
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

    public static class Provider extends org.wikibrain.conf.Provider<PageViewSqlDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return PageViewSqlDao.class;
        }

        @Override
        public String getPath() {
            return "dao.pageView";
        }

        @Override
        public PageViewSqlDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("sql")) {
                return null;
            }
            try {
                PageViewSqlDao dao = new PageViewSqlDao(
                        getConfigurator().get(
                                WpDataSource.class,
                                config.getString("dataSource"))
                );
//                String cachePath = getConfig().get().getString("dao.sqlCachePath");
//                File cacheDir = new File(cachePath);
//                if (!cacheDir.isDirectory()) {
//                    cacheDir.mkdirs();
//                }
//                dao.useCache(cacheDir);
                return dao;
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }

}
