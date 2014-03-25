package org.wikapidia.spatial.core;


import com.vividsolutions.jts.geom.Geometry;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;

import java.util.Collection;
import java.util.Set;

/**
 * Created by toby on 3/25/14.
 */

//TODO: need to implement this in a database later...just for testing now

public class SpatialGeomNameMapping {


    private TIntObjectHashMap<SpatialGeomName> nameMapping;
    private TObjectIntHashMap<SpatialGeomName> idMapping;

    public SpatialGeomNameMapping(){
        nameMapping = new TIntObjectHashMap<SpatialGeomName>();
        idMapping = new TObjectIntHashMap<SpatialGeomName>();
    }

    public TIntObjectHashMap<SpatialGeomName> getGeomNamesForGeomIds(Collection<Integer> geomIds){
        TIntObjectHashMap<SpatialGeomName> resultMapping = new TIntObjectHashMap<SpatialGeomName>();
        for(Integer geomId : geomIds){
            resultMapping.put(geomId, nameMapping.get(geomId));
        }
        return resultMapping;
    }

    public SpatialGeomName getGeoNameForGeomId(Integer geomId){
        return nameMapping.get(geomId);
    }

    public TObjectIntHashMap<SpatialGeomName> getGeomIdsForGeomNames(Collection<SpatialGeomName> geomNames){
        TObjectIntHashMap<SpatialGeomName> resultMapping = new TObjectIntHashMap<SpatialGeomName>();
        for(SpatialGeomName geomName : geomNames){
            resultMapping.put(geomName, idMapping.get(geomName));
        }
        return resultMapping;
    }

    public Integer getGeomIdForGeomName(SpatialGeomName geomName){
        return idMapping.get(geomName);
    }

    public TIntSet getAllGeomIds(){
        return nameMapping.keySet();
    }

    public Set<SpatialGeomName> getAllGeomNames(){
        return idMapping.keySet();
    }

    public void put(Integer geomId, SpatialGeomName geomName){
        nameMapping.put(geomId, geomName);
        idMapping.put(geomName, geomId);
    }

}


