package org.wikapidia.core.domain;

/**
 */
public class Link {
    private String myText;
    private int myID;
    private boolean mySubsec;

    public Link(String text, int id, boolean subsec) {
        myText = text;
        myID = id;
        mySubsec = subsec;
    }

    public String getMyText() {
        return myText;
    }

    public void setMyText(String myText) {
        this.myText = myText;
    }

    public int getMyID() {
        return myID;
    }

    public void setMyID(int myID) {
        this.myID = myID;
    }

    public boolean isMySubsec() {
        return mySubsec;
    }

    public void setMySubsec(boolean mySubsec) {
        this.mySubsec = mySubsec;
    }
}
