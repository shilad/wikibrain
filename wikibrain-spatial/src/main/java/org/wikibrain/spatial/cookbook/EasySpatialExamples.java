package org.wikibrain.spatial.cookbook;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.lang.Language;
import org.wikibrain.spatial.SpatialContainerMetadata;
import org.wikibrain.spatial.constants.Layers;
import org.wikibrain.spatial.constants.Precision;
import org.wikibrain.spatial.constants.RefSys;
import org.wikibrain.spatial.dao.SpatialDataDao;

import java.util.Map;

/**
 * Created by bjhecht on 5/17/14.
 */
public class EasySpatialExamples {

    public static void main(String[] args){

//        useConvenienceFunctionsInSpatialDataDao(args);
//        printSpatialContainerMetadata(args);
//        printAllLoadedLayersInReferenceSystems(args);
//          printLatLonPrecisions(args);

        printNonEarthLayer(args);
    }

    /**
     *  This code prints the centroid of the geographic entity represented by article name.
     */
    public static void useConvenienceFunctionsInSpatialDataDao(String[] args){

        try {

            // do basic setup
            Env env = EnvBuilder.envFromArgs(args);
            Configurator c = env.getConfigurator();
            SpatialDataDao sdDao = c.get(SpatialDataDao.class);

            // set up parameters
            String articleName = "Minneapolis";
            Language lang = Language.SIMPLE;
            String layerName = Layers.WIKIDATA;

            // get geometries
            Geometry g = sdDao.getGeometry(articleName, lang, layerName);
            Point p = g.getCentroid();

            // print
            String outputStr = String.format("The centroid of %s is at (%.4f, %.4f)\n",
                    articleName, p.getCoordinate().x, p.getCoordinate().y);
            System.out.println(outputStr);

        }catch(Exception e){
            e.printStackTrace();
        }

    }

    public static void printSpatialContainerMetadata(String args[]){

        try {

            // do basic setup
            Env env = EnvBuilder.envFromArgs(args);
            Configurator c = env.getConfigurator();
            SpatialDataDao sdDao = c.get(SpatialDataDao.class);

            // get metadata
            SpatialContainerMetadata wikidataMetadata = sdDao.getLayerMetadata(Layers.WIKIDATA, RefSys.EARTH);
            SpatialContainerMetadata earth = sdDao.getReferenceSystemMetadata(RefSys.EARTH);


            // print out metadata
            System.out.println(wikidataMetadata.toString());
            System.out.println(earth.toString());


        }catch(Exception e){
            e.printStackTrace();
        }

    }

    public static void printAllLoadedLayersInReferenceSystems(String args[]){

        try {

            // do basic setup
            Env env = EnvBuilder.envFromArgs(args);
            Configurator c = env.getConfigurator();
            SpatialDataDao sdDao = c.get(SpatialDataDao.class);


            Iterable<String> refSyss = sdDao.getAllRefSysNames(); // get all reference systems
            for (String refSys : refSyss) {

                // print out layerNames
                System.out.printf("Loaded Layers in '%s':\n", refSys);
                for (String layerName : sdDao.getAllLayerNames(refSys)) { // get all layers in reference system
                    System.out.println(layerName);
                }

                System.out.println();

            }


        }catch(Exception e){
            e.printStackTrace();
        }


    }

    public static void printNonEarthLayer(String[] args){

        try {
            // do basic setup
            Env env = EnvBuilder.envFromArgs(args);
            Configurator c = env.getConfigurator();
            SpatialDataDao sdDao = c.get(SpatialDataDao.class);

            Map<Integer, Geometry> geoms = sdDao.getAllGeometriesInLayer(Layers.ELEMENTS, RefSys.PERIODIC_TABLE);
            for (Integer wdId : geoms.keySet()){
                System.out.println("Found wikidata id = " + wdId);
            }

        }catch(Exception e){
            e.printStackTrace();
        }

    }

    public static void printLatLonPrecisions(String args[]){

        try {

            // do basic setup
            Env env = EnvBuilder.envFromArgs(args);
            Configurator c = env.getConfigurator();
            SpatialDataDao sdDao = c.get(SpatialDataDao.class);

            String[] articleNames = new String[] {"Alaska","Minneapolis","California","Germany"};
            Language lang = Language.SIMPLE;
            String layerName = Layers.WIKIDATA;

            for (String articleName : articleNames){
                Geometry g = sdDao.getGeometry(articleName, lang, layerName, Precision.LatLonPrecision.HIGH);
                if (g != null){
                    System.out.printf(":-) Found high-precision geometry for '%s' (%s) in layer '%s': %s\n", articleName, lang.toString(), layerName, g.toString());
                }else{
                    System.out.printf(":-( Could not find high-precision geometry for '%s' (%s) in layer '%s'\n", articleName, lang.toString(), layerName);
                }
            }

        }catch(Exception e){
            e.printStackTrace();
        }


    }

}
