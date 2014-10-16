package org.wikibrain.spatial.dao.postgis;

import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import com.vividsolutions.jts.geom.Geometry;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.referencing.GeodeticCalculator;
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.dao.SpatialNeighborDao;

import java.io.IOException;
import java.util.*;

/**
 * Created by toby on 4/17/14.
 */
public class PostGISSpatialNeighborDao implements SpatialNeighborDao{

    private final PostGISDB db;

    public PostGISSpatialNeighborDao (PostGISDB db){
        this.db = db;

    }

    @Override
    public TIntSet getNeighboringItemIds(Integer itemId, String layerName, String refSysName, Set<String> subLayers, double minDist, double maxDist) throws DaoException{
        Geometry g = db.getGeometry(itemId, layerName, refSysName);
        if (g == null){
            throw new DaoException(String.format("Could not find item %d in layer %s (%s)", itemId, layerName, refSysName));
        }

        return getNeighboringItemIds(g, refSysName, subLayers, minDist, maxDist);
    }

    @Override
    public TIntSet getNeighboringItemIds(Geometry g, String refSysName, Set<String> subLayers, double minDist, double maxDist) throws DaoException{

        if (subLayers.size() == 0) throw new DaoException("Cannot get containment without any layers");

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        PropertyName geomProperty = ff.property(db.getGeometryAttributeName());


        //build ref sys clause
        PropertyName refSysProperty = ff.property(db.getRefSysAttributeName());
        Filter refSysFilter = ff.equals(refSysProperty, ff.literal(refSysName));

        // build layer-related clause
        PropertyName layerProperty = ff.property(db.getLayerAttributeName());
        List<Filter> layerFilters = Lists.newArrayList();
        for (String subLayer : subLayers){
            Filter curLayerFilter = ff.equals(layerProperty, ff.literal(subLayer));
            layerFilters.add(curLayerFilter);
        }

        Filter layerFilter = ff.and(layerFilters);

        Filter withinFilter = ff.dwithin(geomProperty, ff.literal(g), maxDist , "4396");
        Filter beyondFilter = ff.beyond(geomProperty, ff.literal(g), minDist , "4396");


        List<Filter> filters = Lists.newArrayList();
        filters.add(refSysFilter);
        filters.add(layerFilter);
        filters.add(withinFilter);
        filters.add(beyondFilter);

        Filter finalFilter = ff.and(filters);

        try {

            FeatureSource featureSource = db.getFeatureSource();
            FeatureCollection containedFeatures = featureSource.getFeatures(finalFilter);
            FeatureIterator featureIterator = containedFeatures.features();

            TIntSet rVal = new TIntHashSet();

            while (featureIterator.hasNext()){

                Feature f = featureIterator.next();
                Integer itemId = (Integer)f.getProperty(db.getItemIdAttributeName()).getValue();
                rVal.add(itemId);

            }
            featureIterator.close();

            return rVal;

        }catch(IOException e){
            throw new DaoException(e);
        }



    }

    @Override
    public Map<Integer, Geometry> getKNNeighbors (Geometry g, int k, String layerName, String refSysName, Set<Integer> excludeSet) throws DaoException{


        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        PropertyName geomProperty = ff.property(db.getGeometryAttributeName());

        //build ref sys clause
        PropertyName refSysProperty = ff.property(db.getRefSysAttributeName());
        Filter refSysFilter = ff.equals(refSysProperty, ff.literal(refSysName));

        // build layer-related clause
        PropertyName layerProperty = ff.property(db.getLayerAttributeName());
        Filter layerFilter = ff.equals(layerProperty, ff.literal(layerName));

        double guess = 0.01;

        Filter withinFilter = ff.dwithin(geomProperty, ff.literal(g), guess * k , "4396");




        List<Filter> filters = Lists.newArrayList();
        filters.add(refSysFilter);
        filters.add(layerFilter);
        filters.add(withinFilter);


        for(Integer i : excludeSet){
            Filter nonEqualFilter = ff.notEqual(ff.property(db.getItemIdAttributeName()), ff.literal(i));
            filters.add(nonEqualFilter);
        }


        Filter finalFilter = ff.and(filters);



        try {

            FeatureSource featureSource = db.getFeatureSource();
            FeatureCollection containedFeatures = featureSource.getFeatures(finalFilter);

            while(containedFeatures.size() < k && guess * k < 180){
                if(containedFeatures.size() == 0)
                    guess = guess * 2;
                else
                    guess = guess * (k / containedFeatures .size() > 2 ? 1.3 * Math.sqrt((k / containedFeatures.size())) : 2);
                withinFilter = ff.dwithin(geomProperty, ff.literal(g), guess * k , "4396");
                List<Filter> newFilters = Lists.newArrayList();
                newFilters.add(refSysFilter);
                newFilters.add(layerFilter);
                newFilters.add(withinFilter);
                for(Integer i : excludeSet){
                    Filter nonEqualFilter = ff.notEqual(ff.property(db.getItemIdAttributeName()), ff.literal(i));
                    newFilters.add(nonEqualFilter);
                }
                finalFilter = ff.and(newFilters);
                containedFeatures = featureSource.getFeatures(finalFilter);
            }

            FeatureIterator featureIterator = containedFeatures.features();


            Map<Integer, Geometry> rVal = new HashMap<Integer, Geometry>();
            Map<Integer, Geometry> finalRVal = new LinkedHashMap<Integer, Geometry>();
            final Map<Integer, Double> distMap = new HashMap<Integer, Double>();
            List<Integer> order = new LinkedList<Integer>();

            GeodeticCalculator geoCalc = new GeodeticCalculator();


            while (featureIterator.hasNext()){
                try{

                Feature f = featureIterator.next();
                Integer itemId = (Integer)f.getProperty(db.getItemIdAttributeName()).getValue();
                Geometry geometry = (Geometry) f.getDefaultGeometryProperty().getValue();
                rVal.put(itemId, geometry);
                order.add(itemId);
                geoCalc.setStartingGeographicPoint(geometry.getCoordinate().x, geometry.getCoordinate().y);
                geoCalc.setDestinationGeographicPoint(g.getCoordinate().x, g.getCoordinate().y);
                distMap.put(itemId, geoCalc.getOrthodromicDistance());
                }
                catch (Exception e){
                    //do nothing
                }
            }
            featureIterator.close();

            Collections.sort(order, new Comparator<Integer>() {
                @Override
                public int compare(Integer integer, Integer integer2){
                    try {

                        double dist1 = distMap.get(integer);

                        double dist2 = distMap.get(integer2);

                        if(dist1 < dist2) return 0;
                        else
                            return 1;
                    }
                    catch (Exception e){

                    }
                    return 0;
                }
            });
            if(k > order.size())
                k = order.size();

            for(Integer i: order.subList(0, k)){
                finalRVal.put(i, rVal.get(i));
            }
            featureIterator.close();




            return finalRVal;

        }catch(IOException e){
            throw new DaoException(e);
        }



    }
    @Override
    public Map<Integer, Geometry> getNeighbors(Integer itemId, String layerName, String refSysName, Set<Integer> excludeSet) throws DaoException{
        return getNeighbors(db.getGeometry(itemId, layerName, refSysName), layerName, refSysName, excludeSet);
    }

    @Override
    public  Map<Integer, Geometry> getNeighbors(Geometry g, String layerName, String refSysName, Set<Integer> excludeSet) throws DaoException{
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        PropertyName geomProperty = ff.property(db.getGeometryAttributeName());

        //build ref sys clause
        PropertyName refSysProperty = ff.property(db.getRefSysAttributeName());
        Filter refSysFilter = ff.equals(refSysProperty, ff.literal(refSysName));

        // build layer-related clause
        PropertyName layerProperty = ff.property(db.getLayerAttributeName());
        Filter layerFilter = ff.equals(layerProperty, ff.literal(layerName));

        /*

        Filter touchFilter = ff.touches(geomProperty, ff.literal(g));
        Filter intersectFilter = ff.intersects(geomProperty, ff.literal(g));

        List<Filter> orFilters = Lists.newArrayList();
        orFilters.add(touchFilter);
        orFilters.add(intersectFilter);

        Filter touchOrIntersectFilter = ff.or(orFilters);

        */

        Filter intersectFilter = ff.intersects(geomProperty, ff.literal(g));
        List<Filter> filters = Lists.newArrayList();
        filters.add(refSysFilter);
        filters.add(layerFilter);
        filters.add(intersectFilter);


        for(Integer i : excludeSet){
            Filter nonEqualFilter = ff.notEqual(ff.property(db.getItemIdAttributeName()), ff.literal(i));
            filters.add(nonEqualFilter);
        }


        Filter finalFilter = ff.and(filters);

        try {

            FeatureSource featureSource = db.getFeatureSource();
            FeatureCollection containedFeatures = featureSource.getFeatures(finalFilter);
            FeatureIterator featureIterator = containedFeatures.features();

            Map<Integer, Geometry> result = new HashMap<Integer, Geometry>();

            while (featureIterator.hasNext()){

                Feature f = featureIterator.next();
                result.put((Integer)f.getProperty(db.getItemIdAttributeName()).getValue(), (Geometry) f.getDefaultGeometryProperty().getValue());
            }

            featureIterator.close();

            return result;

        }catch(IOException e){
            throw new DaoException(e);
        }

    }

    public TIntSet getMaxDistanceKmItemIds(Integer itemId, String layerName, String refSysName, Set<String> subLayers, double maxDist) throws DaoException{

        return getNeighboringItemIds(itemId, layerName, refSysName, subLayers, 0, maxDist / 112);
    }

    public TIntSet getMaxDistanceKmItemIds(Geometry g, String refSysName, Set<String> subLayers, double maxDist) throws DaoException{

        return getNeighboringItemIds(g, refSysName, subLayers, 0, maxDist / 112);
    }

    @Override
    public Map<Integer, Geometry> getKNNeighbors(Integer itemId, int k, String layerName, String refSysName, Set<Integer> excludeSet) throws DaoException{

        Geometry stPoint = db.getGeometry(itemId, layerName, refSysName);
        return getKNNeighbors(stPoint, k, layerName,refSysName, excludeSet);

    }

    public static class Provider extends org.wikibrain.conf.Provider<PostGISSpatialNeighborDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return SpatialNeighborDao.class;
        }

        @Override
        public String getPath() {
            return "spatial.dao.spatialNeighbor";
        }

        @Override
        public PostGISSpatialNeighborDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {

            return new PostGISSpatialNeighborDao( getConfigurator().get(PostGISDB.class, config.getString("dataSource")));

        }
    }

}
