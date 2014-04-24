package org.wikibrain.pageview;

import gnu.trove.map.TIntIntMap;
import gnu.trove.procedure.TIntIntProcedure;
import org.joda.time.DateTime;
import org.wikibrain.core.lang.Language;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: derian
 * Date: 12/1/13
 * Time: 5:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class PageViewDataStruct {

    Language lang;
    DateTime start;
    DateTime end;


    // represents page views for the time period specified by start and end
    // maps page IDs to the number of views that page has had in the specified time
    // will represent one hour for the first iteration

    //a int-int map, key is page id, value is number of page views
    TIntIntMap stats;

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
