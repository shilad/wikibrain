package org.wikibrain.pageview;

import org.joda.time.DateTime;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: derian
 * Date: 1/13/14
 * Time: 11:54 AM
 * To change this template use File | Settings | File Templates.
 */
public class PageViewDaoFilter extends DaoFilter {

    private Collection<Integer> pageIds;
    private Integer minNumViews;
    private Integer maxNumViews;
    private DateTime startDate;
    private DateTime endDate;

    public PageViewDaoFilter() {
        super();
        pageIds = null;
        minNumViews = null;
        maxNumViews = null;
        startDate = null;
        endDate = null;
    }

    public Collection<Integer> getPageIds() {
        return pageIds;
    }

    public Integer getMinNumViews() {
        return minNumViews;
    }

    public Integer getMaxNumViews() {
        return maxNumViews;
    }

    public DateTime getStartDate() {
        return startDate;
    }
    
    public DateTime getEndDate() {
        return endDate;
    }

    public PageViewDaoFilter setPageIds(Collection<Integer> pageIds) {
        this.pageIds = pageIds;
        return this;
    }

    public PageViewDaoFilter setPageIds(int pageId) {
        this.pageIds = Arrays.asList(pageId);
        return this;
    }

    public PageViewDaoFilter setMinNumViews(int minNumViews) {
        this.minNumViews = minNumViews;
        return this;
    }

    public PageViewDaoFilter setMaxNumViews(int maxNumViews) {
        this.maxNumViews = maxNumViews;
        return this;
    }

    public PageViewDaoFilter setStartDate(DateTime startDate) {
        this.startDate = startDate;
        return this;
    }

    public PageViewDaoFilter setEndDate(DateTime endDate) {
        this.endDate = endDate;
        return this;
    }

    public PageViewDaoFilter setStartDate(Date startDate) {
        this.startDate = new DateTime(startDate);
        return this;
    }

    public PageViewDaoFilter setEndDate(Date endDate) {
        this.endDate = new DateTime(endDate);
        return this;
    }
}
