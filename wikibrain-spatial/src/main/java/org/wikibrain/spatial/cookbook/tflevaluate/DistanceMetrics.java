package org.wikibrain.spatial.cookbook.tflevaluate;

import com.vividsolutions.jts.geom.Geometry;
import org.geotools.referencing.GeodeticCalculator;

/**
 * Created by toby on 5/15/14.
 */
public class DistanceMetrics {

    public DistanceMetrics(){


    }

    public double getDistance(Geometry a, Geometry b){
        GeodeticCalculator geoCalc = new GeodeticCalculator();

        geoCalc.setStartingGeographicPoint(a.getCoordinate().x, a.getCoordinate().y);
        geoCalc.setDestinationGeographicPoint(b.getCoordinate().x, b.getCoordinate().y);

        return geoCalc.getOrthodromicDistance() / 1000;
    }




}
