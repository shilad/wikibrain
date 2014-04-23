package org.wikibrain.spatial.core;

/**
 * Stores metadata about spatial containers (e.g. layers, reference systems)
 * Created by Brent Hecht on 12/29/13.
 */
public class SpatialContainerMetadata {

    public final int geomCount;
    public final int maxShapeType;


    public SpatialContainerMetadata(int geomCount, int maxShapeType) {
        this.geomCount = geomCount;
        this.maxShapeType = maxShapeType;
    }

    public SpatialContainerMetadata merge(SpatialContainerMetadata input){
        int geomCount = this.geomCount + input.geomCount;
        int maxShapeType = Math.max(this.maxShapeType, input.maxShapeType);
        return new SpatialContainerMetadata(geomCount, maxShapeType);
    }

}
