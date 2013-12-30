package org.wikapidia.spatial.core;

/**
 * Created by bjhecht on 12/29/13.
 */
public class SpatialReferenceSystem extends SpatialContainer{

    private String refSysName;

    protected SpatialReferenceSystem(String refSysName, SpatialContainerMetadata metadata) {
        super(metadata);
        this.refSysName = refSysName;
    }

    @Override
    public int hashCode(){
        return refSysName.hashCode();
    }

    public String getReferenceSystemName(){
        return refSysName;
    }

    @Override
    public String toString(){
        return getReferenceSystemName();
    }

    @Override
    public boolean equals(Object o){
        if (o instanceof SpatialReferenceSystem){
            return ((SpatialReferenceSystem)o).toString().equals(this.toString());
        }else{
            return false;
        }
    }

}
