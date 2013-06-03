package org.wikapidia.core.domain;

import java.util.ArrayList;

/**
 */
public class Concept {
    private int pageID;
    private Article[] articles;

    public Concept(int id, Article[] arts) {
        pageID = id;
        articles = arts;
    }

    public int getPageID() {
        return pageID;
    }

    public void setPageID(int pageID) {
        this.pageID = pageID;
    }

    public Article[] getArticles() {
        return articles;
    }

    public void setArticles(Article[] articles) {
        this.articles = articles;
    }
}
