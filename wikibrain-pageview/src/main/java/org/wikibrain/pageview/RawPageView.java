package org.wikibrain.pageview;

import org.joda.time.DateTime;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.Title;

/**
* @author Shilad Sen
*/
public class RawPageView {
    private final DateTime tstamp;
    private final Title title;
    private final int views;

    public RawPageView(DateTime tstamp, Title title, int views) {
        this.tstamp = tstamp;
        this.title = title;
        this.views = views;
    }

    public DateTime getTstamp() {
        return tstamp;
    }

    public Title getTitle() {
        return title;
    }

    public int getViews() {
        return views;
    }

    public Language getLanguage() {
        return title.getLanguage();
    }
}
