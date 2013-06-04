package org.wikapidia.core.domain;

import java.util.ArrayList;

/**
 */
public class Concept {
    private int id;
    private int[] articleIds;

    public Concept(int id, int[] articleIds) {
        this.id = id;
        this.articleIds = articleIds;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int[] getArticleIds() {
        return articleIds;
    }

    public void setArticleIds(int[] articleIds) {
        this.articleIds = articleIds;
    }
}
