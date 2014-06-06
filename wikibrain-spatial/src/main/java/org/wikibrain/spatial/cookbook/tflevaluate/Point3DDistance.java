package org.wikibrain.spatial.cookbook.tflevaluate;

import com.vividsolutions.jts.geom.*;
import org.geotools.referencing.GeodeticCalculator;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;

/**
 * Created by toby on 6/2/14.
 */
public class Point3DDistance {
    /**
     *
     * Format for point: with coordinate(x,y,z). (x,y) in WGS84, z is height in meter.
     *
     * @param a
     * @param b
     * @param calc
     * @return
     */

    public double calculate3DDistance(Point a, Point b, GeodeticCalculator calc){



        calc.setStartingGeographicPoint(a.getX(), a.getY());
        calc.setDestinationGeographicPoint(b.getX(), b.getY());
        double distance = calc.getOrthodromicDistance();
        if (a.getCoordinate().z == Double.NaN || b.getCoordinate().z == Double.NaN)
            return distance;
        double heightDifference = a.getCoordinate().z - b.getCoordinate().z;
        return Math.sqrt(distance * distance + heightDifference * heightDifference );
    }

    public static void main(String[] args){

        try {

            //Env env = EnvBuilder.envFromArgs(args);
            //Configurator c = env.getConfigurator();

            Point3DDistance eval = new Point3DDistance();

            GeodeticCalculator calc = new GeodeticCalculator();
            Coordinate beijing = new Coordinate(116, 39.9, 0);
            Coordinate la = new Coordinate(-118.25, 34.05, 0);

            Point a = new Point(beijing, new PrecisionModel(), 4326);
            Point b = new Point(la, new PrecisionModel(), 4326);

            System.out.printf("%.2f km\n", eval.calculate3DDistance(a, b, calc)/1000);




        }catch(Exception e){
            e.printStackTrace();;
        }


    }



}
