package org.wikapidia.core.domain;

/**
 */
public class Link {
    private String text;
    private int pageID;
    private boolean subsec;

    public Link(String text, int pageID, boolean subsec) {
        this.text = text;
        this.pageID = pageID;
        this.subsec = subsec;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getPageID() {
        return pageID;
    }

    public void setPageID(int pageID) {
        this.pageID = pageID;
    }

    public boolean isSubsec() {
        return subsec;
    }

    public void setSubsec(boolean subsec) {
        this.subsec = subsec;
    }
}
