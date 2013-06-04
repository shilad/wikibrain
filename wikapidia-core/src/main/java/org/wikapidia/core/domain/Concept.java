package org.wikapidia.core.domain;

import java.util.ArrayList;

/**
 */
public class Concept {
    private int conceptId;
    private int[] articleIds;

    public Concept(int conceptId, int[] articleIds) {
        this.conceptId = conceptId;
        this.articleIds = articleIds;
    }

    public int getConceptId() {
        return conceptId;
    }

    public void setConceptId(int conceptId) {
        this.conceptId = conceptId;
    }

    public int[] getArticleIds() {
        return articleIds;
    }

    public void setArticles(int[] articleIds) {
        this.articleIds = articleIds;
    }
}
