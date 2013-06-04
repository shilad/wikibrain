package org.wikapidia.core.domain;

/**
 */
public class Link {
    private String text;
    private int articleId;
    private boolean subsec;

    public Link(String text, int articleId, boolean subsec) {
        this.text = text;
        this.articleId = articleId;
        this.subsec = subsec;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getPageId() {
        return articleId;
    }

    public void setPageId(int articleId) {
        this.articleId = articleId;
    }

    public boolean isSubsec() {
        return subsec;
    }

    public void setSubsec(boolean subsec) {
        this.subsec = subsec;
    }
}
