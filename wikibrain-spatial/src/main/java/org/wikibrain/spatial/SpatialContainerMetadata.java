package org.wikibrain.spatial;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import org.wikibrain.core.WikiBrainException;

/**
 * Stores metadata about spatial containers (e.g. layers, reference systems)
 * Number of geometries (features), shapetype of those features, etc.
 * Created by Brent Hecht on 12/29/13.
 */
public class SpatialContainerMetadata {

    public static enum ShapeType {POINT, POLYLINE, POLYGON, OTHER, MIXED};

    public static ShapeType getShapeTypeFromGeometry(Geometry g){
        if (g instanceof Point){
            return ShapeType.POINT;
        }else if (g instanceof LineString){
            return ShapeType.POLYLINE;
        }else if (g instanceof Polygon){
            return ShapeType.POLYGON;
        }else{
            return ShapeType.OTHER;
        }
    }

    public final int geomCount;
    public final ShapeType shapeType;
    public final String layerName;
    public final String refSysName;

    private boolean isReferenceSystem;

    public SpatialContainerMetadata(String layerName, String refSysName, int geomCount, ShapeType shapeType) {

        this.geomCount = geomCount;
        this.shapeType = shapeType;
        this.layerName = layerName;
        this.refSysName = refSysName;
        this.isReferenceSystem = false;

    }

    public SpatialContainerMetadata merge(SpatialContainerMetadata input) throws WikiBrainException{

        // reference system
        if (!this.refSysName.equals(input.refSysName)){
            throw new WikiBrainException("Cannot merge two spatial container metadata objects describing data from two different reference systems");
        }

        // geom count
        int geomCount = this.geomCount + input.geomCount;

        // layer name
        String layerName = null;
        if (this.layerName.equals(input.layerName)){
            layerName = this.layerName;
        }

        ShapeType shapeType;
        if (input.shapeType.equals(this.shapeType)) {
            shapeType = input.shapeType;
        }else {
            shapeType = ShapeType.MIXED;
        }

        return new SpatialContainerMetadata(layerName, refSysName, geomCount, shapeType);


    }

    public void toReferenceSystem(){
        this.isReferenceSystem = true;
    }

    @Override
    public String toString(){

        if (!isReferenceSystem) {
            return String.format("%s (%s): # geometries = %d, shapetype = %s", layerName, refSysName, geomCount, shapeType.toString());
        }else{
            return String.format("reference system '%s': # geometries = %d, shapetype = %s", refSysName, geomCount, shapeType.toString());
        }

    }

}
