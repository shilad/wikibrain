package org.wikibrain.spatial.cookbook.tflevaluate;

import com.vividsolutions.jts.geom.Geometry;
import org.geotools.referencing.GeodeticCalculator;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.dao.SpatialNeighborDao;

import java.util.*;

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


    public Map<Integer,Integer> getGraphDistance(Integer itemIdA, Map<Integer,Geometry> compareTo, int k, int maxTopoDistance, float[][] distanceMatrix) throws DaoException{

        Geometry a = compareTo.get(itemIdA);
        // topologies in current level
        Map<Integer, Geometry> currentLevel = new HashMap<Integer, Geometry>();
        // topologies found so far
        Set<Integer> discoveredPoint = new HashSet<Integer>();
        // result map
        Map<Integer,Integer> idToDistance = new HashMap<Integer, Integer>();
        currentLevel.put(itemIdA, a);
        discoveredPoint.add(itemIdA);
        idToDistance.put(itemIdA, 0);
        // ids in order
        List<Integer> order = new ArrayList<Integer>();
        order.addAll(compareTo.keySet());

        for (int curTopoDistance=1; curTopoDistance<=maxTopoDistance; curTopoDistance++){
            // if no points in current level, leave loop
            if (currentLevel.isEmpty()){
                System.out.println("Too high: "+curTopoDistance);
                break;
            }
            // newly discovered neighbors
            Map<Integer, Geometry> neighbors = new HashMap<Integer, Geometry>();
            // find all current level geometries' neighbors
            for(Integer i : currentLevel.keySet()){
//                Map<Integer, Geometry> singleNeighbors = snDao.getKNNeighbors(compareTo.get(i), k, layerName,refSysName, discoveredPoint);
                // add new neighbors to discoveredPoint and neighbors
                Map<Integer, Geometry> singleNeighbors = getKNNeighbors(k, compareTo, distanceMatrix[order.indexOf(i)]);

                for(Integer m : singleNeighbors.keySet()){
                    if(!discoveredPoint.contains(m)) {

                        discoveredPoint.add(m);
                        neighbors.put(m, singleNeighbors.get(m));
                    }
                }
            }

//            System.out.println(curTopoDistance+" "+neighbors.keySet().size());

            // new currentLevel
            currentLevel = neighbors;
            // loop over it to find geometries with this topo distance
            for (Integer i: currentLevel.keySet()){
                if (compareTo.keySet().contains(i)){
                    idToDistance.put(i,curTopoDistance);
                }
            }
        }

        System.out.println("id = "+itemIdA);
        System.out.println("total "+idToDistance.size());

        return idToDistance;

    }

    public Set<Integer> getGraphDistance(Map<Integer,Geometry> compareTo, int k, int maxTopoDistance, float[][] significantDistanceMatrix, float[] cityDistanceMatrix) throws DaoException{


        // topologies in current level
        Map<Integer,Geometry> currentLevel = getKNNeighbors(k,compareTo,cityDistanceMatrix);
        // topologies found so far
        Set<Integer> discoveredPoints = new HashSet<Integer>();

        // ids in order
        List<Integer> order = new ArrayList<Integer>();
        order.addAll(compareTo.keySet());

        // loop over possible graph distances
        for (int curTopoDistance=2; curTopoDistance<=maxTopoDistance; curTopoDistance++){
            // if no points in current level, leave loop
            if (currentLevel.isEmpty()){
                System.out.println("Too high: "+curTopoDistance);
                break;
            }
            // newly discovered neighbors
            Map<Integer, Geometry> neighbors = new HashMap<Integer, Geometry>();

            // find all current level geometries' neighbors
            for(Integer i : currentLevel.keySet()){
                // add new neighbors to discoveredPoint and neighbors
                Map<Integer, Geometry> singleNeighbors = getKNNeighbors( k, compareTo, significantDistanceMatrix[order.indexOf(i)]);

                for(Integer m : singleNeighbors.keySet()){
                    if(!discoveredPoints.contains(m)) {

                        discoveredPoints.add(m);
                        neighbors.put(m, singleNeighbors.get(m));
                    }
                }
            }

            // new currentLevel
            currentLevel = neighbors;
        }

        return discoveredPoints;

    }

    public Map<Integer, Geometry> getKNNeighbors( int k, Map<Integer, Geometry> geometries, final float[] distanceMatrix ) throws DaoException {
        final List<Integer> order = new ArrayList<Integer>();
        order.addAll(geometries.keySet());
        Map<Integer, Geometry> result = new HashMap<Integer, Geometry>();
        Collections.sort(order, new Comparator<Integer>() {
            @Override
            public int compare(Integer integer, Integer integer2) {


                double dist1 = distanceMatrix[order.indexOf(integer)];

                double dist2 = distanceMatrix[order.indexOf(integer2)];


                return Double.compare(dist1, dist2);
            }
        });

        for (int i =1; i<=k; i++){
            result.put(order.get(i),geometries.get(order.get(i)));
        }
        return result;
    }

}
