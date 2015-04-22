package org.wikibrain.pageview;

import gnu.trove.map.TIntIntMap;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.Dao;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.LocalId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Shilad Sen
 */
public interface PageViewDao extends Dao<PageView> {
    /**
     * Calculates and returns non-zero views for every page in the date range.
     * Return value is map from page id to pageview count.
     *
     * @param language
     * @param startDate
     * @param endDate
     * @return
     * @throws DaoException
     */
    public TIntIntMap getAllViews(Language language, DateTime startDate, DateTime endDate) throws DaoException;

    /**
     * Returns the total number of views for the requested page.
     *
     * @param pageId
     * @param startDate
     * @param numberOfHours
     * @return
     * @throws DaoException
     */
    public int getNumViews(LocalId pageId, DateTime startDate, int numberOfHours) throws DaoException;

    int getNumViews(Language lang, int pageId, DateTime startDate, int numberOfHours) throws DaoException;

    int getNumViews(Language lang, int pageId, DateTime startDate, DateTime endDate) throws DaoException;

    /**
     * Returns the total number of views for the requested page.
     *
     * @param pageId
     * @param startDate
     * @param endDate
     * @return
     * @throws DaoException
     */
    public int getNumViews(LocalId pageId, DateTime startDate, DateTime endDate) throws DaoException;

    /**
     * Returns the total number of views for the requested page.
     * @param lang
     * @param ids
     * @param startTime
     * @param endTime
     * @return
     * @throws ConfigurationException
     * @throws DaoException
     */
    public Map<Integer, Integer> getNumViews(Language lang, Iterable<Integer> ids, DateTime startTime, DateTime endTime) throws ConfigurationException, DaoException;

    /**
     * Returns the total number of views for the requested page.
     * @param lang
     * @param ids
     * @param dates
     * @return
     * @throws ConfigurationException
     * @throws DaoException
     */
    public Map<Integer, Integer> getNumViews(Language lang, Iterable<Integer> ids, ArrayList<DateTime[]> dates) throws ConfigurationException, DaoException;

    /**
     * Ensure the pageviews for the specified languages and time interval are loaded.
     * Download them and install them if necessary.
     *
     * @param start
     * @param end
     * @param langs
     * @throws DaoException
     */
    public void ensureLoaded(DateTime start, DateTime end, LanguageSet langs) throws DaoException;

    /**
     * Ensure the pageviews for the specified languages and time interval are loaded.
     * Download them and install them if necessary.
     *
     * @param intervals
     * @param langs
     * @throws DaoException
     */
    public void ensureLoaded(List<Interval> intervals, LanguageSet langs) throws DaoException;
}
