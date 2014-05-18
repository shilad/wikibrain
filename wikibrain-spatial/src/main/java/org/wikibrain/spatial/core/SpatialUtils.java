package org.wikibrain.spatial.core;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import org.wikibrain.core.WikiBrainException;

import java.util.List;

/**
 * Spatial utilities class copied directly from thesis wikibrain.
 */
public class SpatialUtils {

    public static enum ShapeType {POINT, POLYLINE, POLYGON};
    public static enum LatLonPrecision {HIGH, MEDIUM, LOW};

    public static Geometry mergeGeometries(Geometry g1, Geometry g2) throws WikiBrainException{

        int t1 = getShapeType(g1).ordinal();
        int t2 = getShapeType(g2).ordinal();

        if (t1 > t2){
            return g1;
        }else if (t2 > t1){
            return g2;
        }else{
            if (g1.equals(g2)){
                return g1;
            }
            if (t1 == ShapeType.POINT.ordinal()){
                List<Point> points = Lists.newArrayList();
                points.addAll(getPointsFromPointType(g1));
                points.addAll(getPointsFromPointType(g2));
                Point[] pointsArr = new Point[points.size()];
                points.toArray(pointsArr);
                MultiPoint mp = new MultiPoint(pointsArr, new GeometryFactory(g1.getPrecisionModel(), g1.getSRID()));
                return mp;
            }else{
                throw new WikiBrainException("Cannot currently merge non-point-based geometries");
            }
        }

    }

    private static List<Point> getPointsFromPointType(Geometry g) throws WikiBrainException{

        List<Point> rVal = Lists.newArrayList();
        if (g instanceof Point){
            rVal.add((Point)g);
            return rVal;
        }else if(g instanceof MultiPoint){
            MultiPoint mp = (MultiPoint)g;
            for (int i = 0; i < mp.getNumGeometries(); i++){
                rVal.add((Point)mp.getGeometryN(i));
            }
        }else{
            throw new WikiBrainException("Cannot get points of geometry that is not Point or MultiPoint");
        }
        return rVal;

    }

    public static ShapeType getShapeType(Geometry g) throws WikiBrainException{

        if (g.getGeometryType().equals("Point") || g.getGeometryType().equals("MultiPoint")){
            return ShapeType.POINT;
        }else if (g.getGeometryType().equals("LineString")){
            return ShapeType.POLYLINE;
        }else if (g.getGeometryType().equals("Polygon") || g.getGeometryType().equals("MultiPolygon")){
            return ShapeType.POLYGON;
        }else{
            throw new WikiBrainException("Geometry of illegal shape type encountered");
        }

    }

}
