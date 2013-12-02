package org.wikapidia.pageview;

import org.joda.time.DateTime;
import org.wikapidia.core.lang.Language;

import java.io.File;
import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: derian
 * Date: 12/1/13
 * Time: 5:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class PageViewIterator implements Iterator {

    private DateTime currentDate;
    private DateTime endDate;
    private Language lang;
    private static String BASE_URL = "http://dumps.wikimedia.your.org/other/pagecounts-raw/";
    private PageViewDataStruct nextData;

    public PageViewIterator(Language lang, DateTime start, DateTime end) {
        this.lang = lang;
        this.currentDate = start;
        this.endDate = end;
        nextData = getPageViewData();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public PageViewDataStruct next() {
        PageViewDataStruct currentData = nextData;
        nextData = getPageViewData();
        return currentData;
    }

    public boolean hasNext() {
         return (nextData == null);
    }

    public PageViewDataStruct getPageViewData() {
        if (currentDate <)

    }

}
