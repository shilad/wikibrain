package org.wikibrain.pageview;

import org.wikibrain.core.lang.LocalId;

import java.util.Date;

/**
 * @author Shilad Sen
 */
public class PageView {
    private final LocalId pageId;
    private final Date hour;          // timestamp to hour precision
    private final int views;

    public PageView(LocalId pageId, Date hour, int views) {
        this.pageId = pageId;
        this.hour = hour;
        this.views = views;
    }

    public LocalId getPageId() {
        return pageId;
    }

    public Date getHour() {
        return hour;
    }

    public int getViews() {
        return views;
    }
}
