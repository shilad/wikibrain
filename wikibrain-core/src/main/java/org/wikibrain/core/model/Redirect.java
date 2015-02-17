package org.wikibrain.core.model;

import org.wikibrain.core.lang.Language;

/**
 */
public class Redirect {

    private final Language language;
    private final int sourceId;
    private final int destId;

    public Redirect(Language language, int sourceId, int destId) {
        this.language = language;
        this.sourceId = sourceId;
        this.destId = destId;
    }

    public Language getLanguage() {
        return language;
    }

    public int getSourceId() {
        return sourceId;
    }

    public int getDestId() {
        return destId;
    }
}
