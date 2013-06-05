package org.wikapidia.core.model;

/**
 */
public class Concept {
    private long id;
    private int[] articleIds;

    public Concept(long id, int[] articleIds) {
        this.id = id;
        this.articleIds = articleIds;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int[] getArticleIds() {
        return articleIds;
    }

    public void setArticleIds(int[] articleIds) {
        this.articleIds = articleIds;
    }
}
