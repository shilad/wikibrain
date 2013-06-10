package org.wikapidia.core.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.List;

/**
 */
public class LocalLink {

    private final Integer localId;
    private final Integer location;
    private final Boolean isParseable;
    private final LocationType locType;

    private static enum LocationType {FIRST_PARA, FIRST_SEC, NONE};

    public boolean isRedId(int id){
        return (id < 0);
    }

    public LocalLink(Integer localID, Integer location, Boolean parseable, LocationType locType) {
        this.localId = localID;
        this.location = location;
        isParseable = parseable;
        this.locType = locType;
    }

    /**
     * Returns the outlink or inlink IDs of the pivot page.
     * @return
     */
    public Integer getLocalId() {
        return localId;
    }

    /**
     * Returns the byte location of the beginning of the lin
     * @return
     */
    public Integer getLocation() {
        return location;
    }

    /**
     * Returns the parseability of the link.
     * @return
     */
    public Boolean getIsParseable() {
        return isParseable;
    }


}
