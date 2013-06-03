package org.wikapidia.core.domain;

/**
 * Created with IntelliJ IDEA.
 * User: shilad
 * Date: 6/3/13
 * Time: 3:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class Article {

    private int myID;
    private String myTitle;
    private NameSpace myNS;
    private boolean myDisamb;
    private boolean myRedirect;

    public Article(int pageID, String title, NameSpace ns , boolean disamb, boolean redirect)
    {
        myID = pageID;
        myTitle = title;
        myNS = ns;
        myDisamb = disamb;
        myRedirect = redirect;
    }

    public int getMyID() {
        return myID;
    }

    public void setMyID(int myID) {
        this.myID = myID;
    }

    public String getMyTitle() {
        return myTitle;
    }

    public void setMyTitle(String myTitle) {
        this.myTitle = myTitle;
    }

    public NameSpace getMyNS() {
        return myNS;
    }

    public void setMyNS(NameSpace myNS) {
        this.myNS = myNS;
    }

    public boolean isMyDisamb() {
        return myDisamb;
    }

    public void setMyDisamb(boolean myDisamb) {
        this.myDisamb = myDisamb;
    }

    public boolean isMyRedirect() {
        return myRedirect;
    }

    public void setMyRedirect(boolean myRedirect) {
        this.myRedirect = myRedirect;
    }

    private enum NameSpace {
        MAIN, TALK,
        USER, USER_TALK,
        WIKIPEDIA, WIKIPEDIA_TALK,
        FILE, FILE_TALK,
        MEDIA_WIKI, MEDIA_WIKI_TALK,
        TEMPLATE, TEMPLATE_TALK,
        HELP, HELP_TALK,
        CATEGORY, CATEGORY_TALK,
        PORTAL, PORTAL_TALK,
        BOOK, BOOK_TALK,
        EDUCATION_PROGRAM, EDUCATION_PROGRAM_TALK,
        TIMED_TEXT, TIMED_TEXT_TALK,
        MODULE, MODULE_TALK
    }

}
