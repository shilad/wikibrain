package org.wikapidia.pageview;

import com.typesafe.config.Config;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.joda.time.DateTime;
import org.jooq.*;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.sql.AbstractSqlDao;
import org.wikapidia.core.dao.sql.SimpleSqlDaoIterable;
import org.wikapidia.core.dao.sql.WpDataSource;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.lang.LocalId;

import java.sql.Timestamp;
import java.util.*;
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
     * @throws org.wikapidia.core.dao.DaoException
     */
    public PageViewSqlDao(WpDataSource dataSource) throws DaoException {
        super(dataSource, INSERT_FIELDS, "/db/pageview");
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
     * @throws org.wikapidia.core.WikapidiaException
     * @throws DaoException
     */
    public PageViewIterator getPageViewIterator(LanguageSet langs, DateTime startDate, DateTime endDate) throws WikapidiaException, DaoException {
        return new PageViewIterator(langs, startDate, endDate);
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

    public TIntIntMap getAllViews(Language language, DateTime startDate, DateTime endDate) throws DaoException {
        checkLoaded(language, startDate, endDate);
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

    public int getNumViews(Language language, int id, DateTime startDate) throws DaoException {
        return getNumViews(language, id, startDate, startDate.plusHours(1));
    }

    public int getNumViews(Language language, int id, DateTime startDate, DateTime endDate) throws DaoException {
        checkLoaded(language, startDate, endDate);
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

    /**
     * Returns all pageviews that meet the filter criteria specified by an input PageViewDaoFilter
     *
     * @see org.wikapidia.pageview.PageViewSqlDao#get(org.wikapidia.core.dao.DaoFilter) for a typical example
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

    protected void checkLoaded(Language lang, DateTime startDate, DateTime endDate) {
        List<DateTime> datesNotLoaded = new ArrayList<DateTime>();
        Set<Long> loadedHourSet = (loadedHours.get(lang.getId()) != null) ? loadedHours.get(lang.getId()) : new HashSet<Long>();
        for (DateTime currentDate = startDate; currentDate.getMillis() < endDate.getMillis(); currentDate = currentDate.plusHours(1)) {
            if (!loadedHourSet.contains(currentDate.getMillis())) {
                datesNotLoaded.add(currentDate);
            }
        }
        load(lang, datesNotLoaded);
    }

    protected void load(Language lang, List<DateTime> dates) {
        PageViewLoader loader = new PageViewLoader(new LanguageSet(lang), this);
        int i = 0;
        while (i < dates.size()) {
            DateTime startDate = dates.get(i++);
            DateTime endDate = startDate.plusHours(1);
            while (dates.get(i).equals(endDate)) {
                endDate = endDate.plusHours(1);
                i++;
            }
            try {
                loader.load(startDate, endDate);
            } catch (ConfigurationException cE) {
                System.out.println(cE.getMessage());
            } catch (WikapidiaException wE) {
                System.out.println(wE.getMessage());
            }
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


    public static class Provider extends org.wikapidia.conf.Provider<PageViewSqlDao> {
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
                return dao;
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }

}
