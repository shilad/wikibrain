package org.wikapidia.core.model;

import org.wikapidia.core.lang.Language;

/**
 */
public class LocalLink {

    private final Language language;
    private final String anchorText;
    private final int localId;
    private final int location;
    private final boolean isParseable;
    private final LocationType locType;

    private static enum LocationType {FIRST_PARA, FIRST_SEC, NONE};

    public static boolean isRedId(int id){
        return (id < 0);
    }

    public LocalLink(Language language, String anchorText, int localId, int location, boolean isParseable, LocationType locType) {
        this.language = language;
        this.anchorText = anchorText;
        this.localId = localId;
        this.location = location;
        this.isParseable = isParseable;
        this.locType = locType;
    }

    /**
     * Returns the language of the link.
     * @return
     */
    public Language getLanguage() {
        return language;
    }

    /**
     * Returns the anchor text of the link.
     * @return
     */
    public String getAnchorText() {
        return anchorText;
    }

    /**
     * Returns the outlink or inlink IDs of the pivot page.
     * @return
     */
    public int getLocalId() {
        return localId;
    }

    /**
     * Returns the byte location of the beginning of the link.
     * @return
     */
    public int getLocation() {
        return location;
    }

    /**
     * Returns the parseability of the link.
     * @return
     */
    public boolean isParseable() {
        return isParseable;
    }
}
