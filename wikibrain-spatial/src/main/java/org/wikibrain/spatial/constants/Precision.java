package org.wikibrain.spatial.constants;

import com.vividsolutions.jts.geom.Point;

/**
 * Created by bjhecht on 5/18/14.
 */
public class Precision {

    /**
     * Low = all wikidata points, High = anything without any significant digits after the decimal.
     * Not defined outside 'earth' reference system.
     *
     * This allows researchers and developers to implement solutions to the Geoweb Scale Problem (Hecht and Gergle 2010) like those in Lieberman et al. (2009).
     * Effectively, considering out LatLonPrecision.HIGH points will filter out *some* very large entities (e.g. Alaska) represented as points,
     * at the expense of ignoring high-precision points that happen to fall on lines of latitude AND longitude (very small set).
     *
     */
    public static enum LatLonPrecision {LOW, HIGH};

    public static LatLonPrecision getLatLonPrecision(Point p){

        if (hasSigDigitsAfterDecimal(p.getX()) || hasSigDigitsAfterDecimal(p.getY())){
            return LatLonPrecision.HIGH;
        }else{
            return LatLonPrecision.LOW;
        }

    }

    /**
     * Returns true if p1 greater than or equal to p2
     * @param p1
     * @param p2
     * @return
     */
    public static boolean isGreaterThanOrEqualTo(LatLonPrecision p1, LatLonPrecision p2){
        if (p1.equals(LatLonPrecision.HIGH)){
            return true;
        }else{
            if (p2.equals(LatLonPrecision.LOW)){
                return true;
            }else{
                return false;
            }
        }
    }

    private static boolean hasSigDigitsAfterDecimal(double d){

        Double dObj = new Double(d);
        Integer dInt = (int)Math.floor(d);

        return (dObj - dInt > 0);
    }
}
