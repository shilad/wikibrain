package org.wikapidia.spatial.core;

import java.util.Set;

/**
 * Created by bjhecht on 12/29/13.
 */
public abstract class SpatialContainer {

    private SpatialContainerMetadata metadata;

    protected SpatialContainer(SpatialContainerMetadata metadata){
        this.metadata = metadata;
    }

    public SpatialContainerMetadata getMetadata(){
        return metadata;
    }
}
