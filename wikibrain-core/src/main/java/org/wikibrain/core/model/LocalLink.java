package org.wikibrain.core.model;

import org.apache.commons.lang3.ObjectUtils;
import org.wikibrain.core.lang.Language;

/**
 */
public class LocalLink implements Comparable<LocalLink> {

    private final Language language;
    private final String anchorText;
    private final int sourceId;
    private final int destId;
    private final boolean isOutlink;
    private final int location;

    //should allow null for Live DAO impl, which can't distinguish between parseable and unparseable
    private final Boolean isParseable;

    private final LocationType locType;

    @Override
    public int compareTo(LocalLink o) {
        int r = location - o.getLocation();
        if (r == 0) {
            r = language.compareTo(o.language);
        }
        if (r == 0) {
            r = sourceId - o.sourceId;
        }
        if (r == 0) {
            r = destId - o.destId;
        }
        if (r == 0) {
            r = ObjectUtils.compare(isOutlink, o.isOutlink);
        }
        if (r == 0) {
            r = ObjectUtils.compare(anchorText, anchorText);
        }
        return r;
    }

    public static enum LocationType {FIRST_PARA, FIRST_SEC, NONE}

    public LocalLink(Language language, String anchorText, int sourceId, int destId, boolean outlink, int location, Boolean parseable, LocationType locType) {
        this.language = language;
        this.anchorText = anchorText;
        this.sourceId = sourceId;
        this.destId = destId;
        isOutlink = outlink;
        this.location = location;
        isParseable = parseable;
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
     * The sourceId, destId, and isOutlink fields are all interrelated.
     * The ID fields give the local ID of the source and destination pages, respectivley,
     * while the isOutlink boolean determines whether the LocalLink was instantiated
     * as an outlink or an inlink.  This determines which parameter is returned by this
     * getLocalId method, which returns sourceId if isOutlink is true, and destId if not.
     *
     * @return sourceId or destId, depending on the field isOutlink
     */
    public int getLocalId() {
        return (isOutlink ? destId : sourceId);
    }

    /**
     * Returns whether or not this link is an outlink. Otherwise, it is an inlink.
     * @return
     */
    public boolean isOutlink() {
        return isOutlink;
    }

    /**
     *
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

    public int getSourceId(){
        return sourceId;
    }

    public int getDestId(){
        return destId;
    }

    public LocationType getLocType(){
        return locType;
    }

    public long longHashCode() {
        return ((long)sourceId << 32) | destId + 232421 * language.getId();
    }

    @Override
    public String toString() {
        return "LocalLink{" +
                "language=" + language +
                ", anchorText='" + anchorText + '\'' +
                ", sourceId=" + sourceId +
                ", destId=" + destId +
                ", isOutlink=" + isOutlink +
                ", location=" + location +
                ", isParseable=" + isParseable +
                ", locType=" + locType +
                '}';
    }
}
