package org.wikapidia.spatial.core.dao;

import com.vividsolutions.jts.geom.Geometry;

import java.util.Collection;

/**
 * Created by Brent Hecht on 12/29/13.
 * Analogue to DaoFilter for spatial package
 *
 */
public class SpatialDaoFilter {

    private Collection<String> refSysNames;
    private Collection<String> layerNames;
    private Collection<Integer> geomIds;
    private Collection<Geometry> geometries;
    private SpatialContainmentDao.ContainmentOperationType containmentOperationType;

    public SpatialDaoFilter(){
        this.refSysNames = null;
        this.layerNames = null;
        this.geomIds = null;
        this.geometries = null;
        this.containmentOperationType = null;
    }

    public SpatialDaoFilter setRefSysNames(Collection<String> refSysNames){
        this.refSysNames = refSysNames;
        return this;
    }

    public SpatialDaoFilter setLayerNames(Collection<String> layerNames){
        this.layerNames = layerNames;
        return this;
    }

    public SpatialDaoFilter setGeomIds(Collection<Integer> geomIds){
        this.geomIds = geomIds;
        return this;
    }

    public SpatialDaoFilter setGeometries(Collection<Geometry> geometries){
        this.geometries = geometries;
        return this;
    }

    public SpatialDaoFilter setContainmentOperationType(SpatialContainmentDao.ContainmentOperationType opType){
        this.containmentOperationType = opType;
        return this;
    }


    public Collection<String> getRefSysNames() {
        return refSysNames;
    }

    public Collection<String> getLayerNames() {
        return layerNames;
    }

    public Collection<Integer> getGeomIds() {
        return geomIds;
    }

    public Collection<Geometry> getGeometries() {
        return geometries;
    }
}
