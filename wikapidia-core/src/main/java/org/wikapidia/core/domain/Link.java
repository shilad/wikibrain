package org.wikapidia.core.domain;

/**
 */
public class Link {
    private String text;
    private int id;
    private boolean subsec;

    public Link(String text, int id, boolean subsec) {
        this.text = text;
        this.id = id;
        this.subsec = subsec;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isSubsec() {
        return subsec;
    }

    public void setSubsec(boolean subsec) {
        this.subsec = subsec;
    }
}
