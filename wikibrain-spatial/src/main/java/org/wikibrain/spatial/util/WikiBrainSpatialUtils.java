package org.wikibrain.spatial.util;

import com.google.gson.JsonObject;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import org.apache.commons.math3.util.FastMath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Created by bjhecht on 5/21/14.
*/
public class WikiBrainSpatialUtils {

    /**
     * Radius of earth, in meters.
     */
    public static final double EARTH_RADIUS = 6372800;
    public static final double EARTH_CIRCUMFERENCE = 2 * Math.PI * EARTH_RADIUS;
    private static final String EARTH_ITEM_ID = "Q2";

    private static final Logger LOG = LoggerFactory.getLogger(WikiBrainSpatialUtils.class);


    public static Geometry jsonToGeometry(JsonObject json){
        try {
            Double latitude = json.get("latitude").getAsDouble();
            Double longitude = json.get("longitude").getAsDouble();
            if (json.has("globe") && json.get("globe").isJsonPrimitive()) {
                String globe = json.get("globe").getAsString();
                if (!globe.endsWith(EARTH_ITEM_ID) && !globe.endsWith("earth")) {
                    return null; // check to make sure these refer to the Earth
                }
            }
            return getPoint(latitude, longitude);
        }catch(Exception e){
            LOG.warn("Parse error while reading Wikidata json value: " + json + " (" + e.getMessage() + ")");
            return null;
        }
    }

    /**
     * Returns the effective centroid of the geometry.
     * This is (currently) the centroid of the largest polygon.
     * @param g
     * @return
     */
    public static Point getCenter(Geometry g) {
        if (g instanceof Point) {
            return (Point)g;
        }
        Geometry largest = g;
        if (largest instanceof MultiPolygon) {
            double largestArea = -1;
            MultiPolygon mp = (MultiPolygon)g;
            for (int i = 0; i < mp.getNumGeometries(); i++) {
                Geometry g2 = mp.getGeometryN(i);
                double area = g2.getArea();
                if (area > largestArea) {
                    largestArea = area;
                    largest = g2;
                }
            }
        }
        return largest.getCentroid();
    }

    public static double[] get3DPoints(Point p) {
        double lng = FastMath.toRadians(p.getX());
        double lat = FastMath.toRadians(p.getY());
        return new double[] {
                FastMath.cos(lat) * FastMath.sin(-lng),
                FastMath.cos(lat) * FastMath.cos(-lng),
                FastMath.sin(-lat),
        };
    }

    public static double haversine(Point p1, Point p2) {
        return haversine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    /**
     * Approximation of the distance between two geographic points that treats the
     * earth as a sphere. Fast, but can have 0.5% error because the Earth is closer
     * to an ellipsoid.
     *
     * From http://rosettacode.org/wiki/Haversine_formula#Java
     *
     * The use of FastMath below cuts the time by more than 50%.
     *
     * @param lon1
     * @param lat1
     * @param lon2
     * @param lat2
     * @return
     */
    public static double haversine(double lon1, double lat1, double lon2, double lat2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = FastMath.sin(dLat / 2) * FastMath.sin(dLat / 2) + FastMath.sin(dLon / 2) * FastMath.sin(dLon / 2) * FastMath.cos(lat1) * FastMath.cos(lat2);
        double c = 2 * FastMath.asin(FastMath.sqrt(a));
        return EARTH_RADIUS * c;
    }

    public static Point getPoint(double lat, double lon) {
        Coordinate[] coords = new Coordinate[1];
        coords[0] = new Coordinate(lon, lat);
        CoordinateArraySequence coordArraySeq = new CoordinateArraySequence(coords);
        return new Point(coordArraySeq, new GeometryFactory(new PrecisionModel(), 4326));
    }
}
