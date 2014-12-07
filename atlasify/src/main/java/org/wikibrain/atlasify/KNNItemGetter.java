package org.wikibrain.atlasify;

import com.vividsolutions.jts.awt.PointShapeFactory;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import gnu.trove.set.TIntSet;
import org.apache.commons.lang3.StringUtils;
import org.geotools.referencing.GeodeticCalculator;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.jooq.tables.LocalPage;
import org.wikibrain.core.lang.Language;
import org.wikibrain.spatial.dao.SpatialContainmentDao;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.spatial.dao.SpatialNeighborDao;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResultList;

import java.util.*;

/**
 * Created by toby on 9/24/14
 */
public class KNNItemGetter {

    private SpatialNeighborDao snDao;
    private LocalPageDao lpDao;
    private UniversalPageDao upDao;
    private SpatialContainmentDao scDao;
    private SpatialDataDao sdDao;
    private Env env;
    private Configurator conf;

    public KNNItemGetter(Env env) throws ConfigurationException{
        this.env = env;
        this.conf = env.getConfigurator();
        this.snDao = conf.get(SpatialNeighborDao.class);
        this.lpDao = conf.get(LocalPageDao.class);
        this.scDao = conf.get(SpatialContainmentDao.class);
        this.sdDao = conf.get(SpatialDataDao.class);
    }

    public Map<Integer, Geometry> getKNNItem(Integer k, String layer, String refSys, Double lat, Double lon) throws DaoException{
        GeometryFactory geometryFactory = new GeometryFactory();
        Point currentLocation = geometryFactory.createPoint(new Coordinate(lon, lat));
        Map<Integer, Geometry> KNNResult = snDao.getKNNeighbors(currentLocation, k, layer, refSys, new HashSet<Integer>());
        return KNNResult;
    }

    public Map<Integer, Geometry> getContainingPolygon(String polygonLayer, String refSys, Double lat, Double lon) throws DaoException{
        GeometryFactory geometryFactory = new GeometryFactory();
        Point currentLocation = geometryFactory.createPoint(new Coordinate(lon, lat));
        Set<String> layerSet = new HashSet<String>();
        layerSet.add(polygonLayer);
        TIntSet containedSet = scDao.getContainedItemIds(currentLocation,  refSys, layerSet , SpatialContainmentDao.ContainmentOperationType.INTERSECTION);
        int[] resArray = new int[100];
        containedSet.toArray(resArray);
        Map<Integer, Geometry> resMap = new HashMap<Integer, Geometry>();
        for(int a : resArray){
            resMap.put(a, sdDao.getGeometry(a, polygonLayer));
        }
        return resMap;
    }


    public static void main(String[] args) throws Exception {
        Env env = EnvBuilder.envFromArgs(args);
        Configurator c = env.getConfigurator();
        LocalPageDao lpDao = c.get(LocalPageDao.class);
        UniversalPageDao upDao = c.get(UniversalPageDao.class);
        KNNItemGetter itemGetter = new KNNItemGetter(env);
        Double lat = 44.975248;
        Double lon = -93.234167;

        SRMetric sr = c.get(
                SRMetric.class, "ensemble",
                "language", "simple");

        Map<Integer, Geometry> containedResult = itemGetter.getContainingPolygon("states", "earth", lat, lon);
        for(Map.Entry<Integer, Geometry> e: containedResult.entrySet()){

            try{
                String phrase = upDao.getById(e.getKey()).getBestEnglishTitle(lpDao,true).getCanonicalTitle();
                System.out.println("Contained in " + phrase  );
                System.out.println("Most Related Articles to " + phrase);


                //Similarity between strings

                    SRResultList similar = sr.mostSimilar(phrase, 10);
                    List<String> pages = new ArrayList<String>();
                    for (int i = 0; i < similar.numDocs(); i++) {
                        org.wikibrain.core.model.LocalPage page = lpDao.getById(Language.SIMPLE, similar.getId(i));
                        pages.add((i+1) + ") " + page.getTitle());
                    }
                    System.out.println("'" + phrase + "' is similar to " + StringUtils.join(pages, ", "));

            }
            catch (Exception exception){
                //do nothing
            }
        }



        Map<Integer, Geometry> getterResult = itemGetter.getKNNItem(20, "wikidata", "earth", lat, lon);
        for(Map.Entry<Integer, Geometry> e: getterResult.entrySet()){

            try{
            GeodeticCalculator geoCalc = new GeodeticCalculator();
            geoCalc.setStartingGeographicPoint(lon, lat);
            geoCalc.setDestinationGeographicPoint(e.getValue().getCoordinate().x, e.getValue().getCoordinate().y);
            Double km = geoCalc.getOrthodromicDistance() / 1000;

            System.out.println(upDao.getById(e.getKey()).getBestEnglishTitle(lpDao,true).getCanonicalTitle() + " " +km + " km");
            }
            catch (Exception exception){
                //do nothing
            }
        }




    }


}
