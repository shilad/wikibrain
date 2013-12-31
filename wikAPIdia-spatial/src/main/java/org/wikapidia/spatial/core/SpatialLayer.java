package org.wikapidia.spatial.core;

import java.util.Set;

/**
 * Created by Brent Hecht on 12/29/13.
 * Represents a spatial layer, as is typically defined in the GIScience community
 */
public class SpatialLayer extends SpatialContainer{

    protected final String layerName;
    protected final String refSysName;

    protected SpatialLayer(SpatialContainerMetadata stats, String layerName, String refSysName){
        super(stats);
        this.layerName = layerName;
        this.refSysName = refSysName;
    }

    @Override
    public int hashCode(){
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object o){
        if (o instanceof SpatialLayer){
            return ((SpatialLayer)o).toString().equals(this.toString());
        }else{
            return false;
        }
    }

    @Override
    public String toString(){
        return layerName + "_" + refSysName;
    }

    public String getLayerName(){return layerName;}

    public String getRefSysName() {return refSysName;}



}
