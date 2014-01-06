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
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.sql.AbstractSqlDao;
import org.wikapidia.core.dao.sql.SimpleSqlDaoIterable;
import org.wikapidia.core.dao.sql.WpDataSource;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LocalId;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;

/**
 * Eventually this should implement a PageViewDao interface.
 *
 * @author Shilad Sen
 */
public class PageViewSqlDao extends AbstractSqlDao<PageView> {

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
        super(dataSource, INSERT_FIELDS, "db/pageview");
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
     * @param lang
     * @param startDate
     * @param endDate
     * @return
     * @throws org.wikapidia.core.WikapidiaException
     * @throws DaoException
     */
    public PageViewIterator getPageViewIterator(Language lang, DateTime startDate, DateTime endDate) throws WikapidiaException, DaoException {
        return new PageViewIterator(lang, startDate, endDate);
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
        }
    }

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
     * Returns all pageviews, for now.
     *
     * TODO: implement standard queries. This is tricky, because they are very pageview specific and don't fit into the existing structure.
     * @see org.wikapidia.pageview.PageViewSqlDao#get(org.wikapidia.core.dao.DaoFilter) for a typical example
     *
     * @param daoFilter a set of filters to limit the search
     * @return
     * @throws DaoException
     */
    @Override
    public Iterable<PageView> get(final DaoFilter daoFilter) throws DaoException {
        DSLContext context = getJooq();
        try {
            Cursor<Record> result = context.select().
                    from(Tables.LOCAL_PAGE).
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
            return LocalPageDao.class;
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
