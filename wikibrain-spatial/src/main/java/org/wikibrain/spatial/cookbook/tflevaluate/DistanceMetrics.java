package org.wikibrain.spatial.cookbook.tflevaluate;

import com.vividsolutions.jts.geom.Geometry;
import org.geotools.referencing.GeodeticCalculator;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.dao.SpatialNeighborDao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by toby on 5/15/14.
 */
public class DistanceMetrics {
    Env env;
    Configurator c;
    SpatialNeighborDao snDao;

    public DistanceMetrics() throws ConfigurationException{
        env = new EnvBuilder().build();
        c = env.getConfigurator();
        snDao = c.get(SpatialNeighborDao.class);

    }

    public DistanceMetrics(Env env, Configurator c, SpatialNeighborDao snDao){
        this.env = env;
        this.c = c;
        this.snDao = snDao;
    }

    public double getDistance(Geometry a, Geometry b){
        GeodeticCalculator geoCalc = new GeodeticCalculator();

        geoCalc.setStartingGeographicPoint(a.getCoordinate().x, a.getCoordinate().y);
        geoCalc.setDestinationGeographicPoint(b.getCoordinate().x, b.getCoordinate().y);

        return geoCalc.getOrthodromicDistance() / 1000;
    }

    public int getTopologicalDistance(Geometry a, Integer itemIdA, Geometry b, Integer itemIdB, int k, String layerName, String refSysName) throws DaoException{

        int counter = 0;

        Map<Integer, Geometry> currentLevel = new HashMap<Integer, Geometry>();
        Set<Integer> discoveredPoint = new HashSet<Integer>();
        currentLevel.put(itemIdA, a);
        discoveredPoint.add(itemIdA);
        while(!currentLevel.isEmpty()){
            counter ++;
            Map<Integer, Geometry> neighbors = new HashMap<Integer, Geometry>();
            for(Integer i : currentLevel.keySet()){

                Map<Integer, Geometry> singleNeighbors = snDao.getKNNeighbors(a, k, layerName,refSysName, discoveredPoint);
                if (singleNeighbors.keySet().contains(itemIdB))
                    return counter;
                for(Integer m : singleNeighbors.keySet()){
                    if(discoveredPoint.contains(m))
                        continue;
                    discoveredPoint.add(m);
                    neighbors.put(m, singleNeighbors.get(m));
                }
            }
            currentLevel = neighbors;
        }



        return -1;

    }




}
