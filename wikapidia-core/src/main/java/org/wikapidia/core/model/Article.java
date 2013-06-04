package org.wikapidia.core.model;

/**
 */
public class Article {

    private int id;
    private String title;
    private NameSpace ns;
    private PageType type;

    public Article(int id, String title, NameSpace ns , PageType type)
    {
        this.id = id;
        this.title = title;
        this.ns = ns;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public NameSpace getNs() {
        return ns;
    }

    public void setNs(NameSpace ns) {
        this.ns = ns;
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
