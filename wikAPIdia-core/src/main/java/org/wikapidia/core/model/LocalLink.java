package org.wikapidia.core.model;

import com.google.common.base.Strings;
import com.google.common.primitives.Bytes;
import org.apache.commons.lang.StringUtils;
import org.wikapidia.core.lang.Language;

/**
 */
public class LocalLink {

    private final Language language;
    private final String anchorText;
    private final int sourceId;
    private final int destId;
    private final boolean isOutlink;
    private final int location;
    private final boolean isParseable;
    private final LocationType locType;

    public static enum LocationType {FIRST_PARA, FIRST_SEC, NONE}

    public LocalLink(Language language, String anchorText, int sourceId, int destId, boolean outlink, int location, boolean parseable, LocationType locType) {
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
        return (isOutlink ? sourceId : destId);
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

    public String getKey() {
        return language.getId() + "_" + sourceId + "_" + destId;
    }

    public byte[] getBytes() {
        Object[] fields = new Object[5];
        fields[0] = anchorText;
        fields[1] = isOutlink;
        fields[2] = location;
        fields[3] = isParseable;
        fields[4] = locType.toString();
        String temp = StringUtils.join(fields, "__");
        return temp.getBytes();
    }

    public static LocalLink getLocalLink(String key, byte[] bytes) {
        String[] keys = key.split("_");
        String[] fields = new String(bytes).split("__");
        return new LocalLink(
                Language.getById(Integer.valueOf(keys[0])),
                fields[0],
                Integer.valueOf(keys[1]),
                Integer.valueOf(keys[2]),
                Boolean.valueOf(fields[1]),
                Integer.valueOf(fields[2]),
                Boolean.valueOf(fields[3]),
                LocationType.valueOf(fields[4])
        );
    }
}
