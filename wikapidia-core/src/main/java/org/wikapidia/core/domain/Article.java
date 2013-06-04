package org.wikapidia.core.domain;

/**
 * Created with IntelliJ IDEA.
 * User: shilad
 * Date: 6/3/13
 * Time: 3:10 PM
 * To change this template use File | Settings | File Templates.
 */

/**
 * This class represents a Wikipedia article and contains information about all of the components of that article.
 */
public class Article {

    private int pageID;
    private String title;
    private NameSpace pageNS;
    private PageType type;

    /**
     * This is the basic constructor for an Article that allows you to fill in all of the data manually.
     * @param id The numeric page ID for the Article in Wikipedia.
     * @param t The title of the Article as it appears in the Wikipedia page.
     * @param ns The Namespace that is a way that Wikipedia catagorizes its articles by type.
     * @param p The page type of the article (Diasambiguation, Redirect, or Normal).
     */
    public Article(int id, String t, NameSpace ns , PageType p)
    {
        pageID = id;
        title = t;
        pageNS = ns;
        type = p;
    }

    /**
     *
     * @return The page ID number of the Article.
     */
    public int getPageID() {
        return pageID;
    }

    /**
     *
     * @param pageID A valid integer as a page ID number for an Article.
     */
    public void setPageID(int pageID) {
        this.pageID = pageID;
    }

    /**
     *
     * @return The title of the Article.
     */
    public String getTitle() {
        return title;
    }

    /**
     *
     * @param title A valid String as the title for an Article.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     *
     * @return The Namespace of the Article, representing the type of Namespace of this Article.
     */
    public NameSpace getPageNS() {
        return pageNS;
    }

    /**
     *
     * @param pageNS A valid Namespace as defined by the NameSpace enum to represent the type of Namespace of this Article.
     */
    public void setPageNS(NameSpace pageNS) {
        this.pageNS = pageNS;
    }

    /**
     *
     * @return If the page is a Disambiguation, Redirect, or Normal page.
     */
    public PageType getType() {
        return type;
    }

    /**
     *
     * @param type A valid page type as defined by the PageType enum to represent the type of page of this Article.
     */
    public void setType(PageType type) {
        this.type = type;
    }

    /**
     * This enum represents the different types of Namespaces, used to catagorize pages, as defined by Wikipedia for the article.
     */
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

    /**
     * This enum represents the three different types of page an Article can be. It can be a disambiguation, a Redirect, or a normal page.
     */
    private enum PageType{
        DISAMB, REDIRECT, NORMAL
    }

}
