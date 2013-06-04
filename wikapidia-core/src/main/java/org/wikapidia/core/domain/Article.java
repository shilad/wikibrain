package org.wikapidia.core.domain;

/**
 * Created with IntelliJ IdEA.
 * User: shilad
 * Date: 6/3/13
 * Time: 3:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class Article {

    private int articleId;
    private String title;
    private NameSpace pageNS;
    private PageType type;

    public Article(int articleId, String title, NameSpace pageNS , PageType type)
    {
        this.articleId = articleId;
        this.title = title;
        this.pageNS = pageNS;
        this.type = type;
    }

    public int getPageId() {
        return articleId;
    }

    public void setPageId(int articleId) {
        this.articleId = articleId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public NameSpace getPageNS() {
        return pageNS;
    }

    public void setPageNS(NameSpace pageNS) {
        this.pageNS = pageNS;
    }

    public PageType getType() {
        return type;
    }

    public void setType(PageType type) {
        this.type = type;
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

    private enum PageType{
        DISAMB, REDIRECT, STANDARD
    }

}
