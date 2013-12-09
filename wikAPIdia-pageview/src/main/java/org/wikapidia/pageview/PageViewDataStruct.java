package org.wikapidia.pageview;

import gnu.trove.map.TIntIntMap;
import org.joda.time.DateTime;
import org.wikapidia.core.lang.Language;

/**
 * Created with IntelliJ IDEA.
 * User: derian
 * Date: 12/1/13
 * Time: 5:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class PageViewDataStruct {

    private final Language lang;
    private final DateTime start;
    private final DateTime end;

    // represents page views for the time period specified by start and end
    // maps page IDs to the number of views that page has had in the specified time
    // will represent one hour for the first iteration
    private final TIntIntMap stats;

    public PageViewDataStruct(Language lang, DateTime start, DateTime end, TIntIntMap stats) {
        this.lang = lang;
        this.start = start;
        this.end = end;
        this.stats = stats;
    }

    public Language getLang() {
        return lang;
    }

    public DateTime getStartDate() {
        return start;
    }

    public DateTime getEndDate() {
        return end;
    }

    public TIntIntMap getPageViewStats() {
        return stats;
    }
}
