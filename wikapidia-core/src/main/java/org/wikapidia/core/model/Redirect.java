package org.wikapidia.core.model;

import org.wikapidia.core.lang.Language;

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
