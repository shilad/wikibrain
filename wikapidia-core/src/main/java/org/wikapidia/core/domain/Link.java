package org.wikapidia.core.domain;

/**
 */
public class Link {
    private String text;
    private int pageID;
    private boolean subsec;

    public Link(String t, int id, boolean sub) {
        text = t;
        pageID = id;
        subsec = sub;
    }

}
