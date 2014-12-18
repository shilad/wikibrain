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
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.spatial.dao.SpatialContainmentDao;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by bjhecht on 4/7/14.
 */
public class PostGISSpatialContainmentDao implements SpatialContainmentDao {


    private final PostGISDB db;

    public PostGISSpatialContainmentDao(PostGISDB db){
        this.db = db;
    }

    @Override
    public TIntSet getContainedItemIds(Integer itemId, String layerName, String refSysName, Set<String> subLayers, ContainmentOperationType opType) throws DaoException {

        Geometry g = db.getGeometry(itemId, layerName, refSysName);
        if (g == null){
            throw new DaoException(String.format("Could not find item %d in layer %s (%s)", itemId, layerName, refSysName));
        }

        return getContainedItemIds(g, refSysName, subLayers, opType);

    }

    @Override
    public TIntSet getContainedItemIds(Geometry g, String refSysName, Set<String> subLayers, ContainmentOperationType opType) throws DaoException {

        if (subLayers.size() == 0) throw new DaoException("Cannot get containment without any layers");


        // *** BUILD SOMEWHAT COMPLEX QUERY ***

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

        Filter geomFilter = null;
        switch(opType){
            case CONTAINMENT:
                geomFilter = ff.contains(ff.literal(g), geomProperty);
                break;
            case INTERSECTION:
                geomFilter = ff.intersects(ff.literal(g), geomProperty);
                break;
            default:
                throw new DaoException("Illegal containment operation type (not supported): " + opType);
        }

        List<Filter> filters = Lists.newArrayList();
        filters.add(refSysFilter);
        filters.add(layerFilter);
        filters.add(geomFilter);

        Filter finalFilter = ff.and(filters);

        // *** EXECUTE QUERY ***
        FeatureSource featureSource;
        FeatureCollection containedFeatures;
        try {

            featureSource = db.getFeatureSource();
            containedFeatures = featureSource.getFeatures(finalFilter);
        }
        catch (IOException e){
            throw new DaoException();
        }
        FeatureIterator featureIterator = containedFeatures.features();

        TIntSet rVal = new TIntHashSet();

        while (featureIterator.hasNext()){

            Feature f = featureIterator.next();
            Integer itemId = (Integer)f.getProperty(db.getItemIdAttributeName()).getValue();
            rVal.add(itemId);

        }
        featureIterator.close();



        return rVal;



    }



    public static class Provider extends org.wikibrain.conf.Provider<PostGISSpatialContainmentDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return SpatialContainmentDao.class;
        }

        @Override
        public String getPath() {
            return "spatial.dao.spatialContainment";
        }

        @Override
        public PostGISSpatialContainmentDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {

            return new PostGISSpatialContainmentDao( getConfigurator().get(PostGISDB.class, config.getString("dataSource")));

        }
    }
}
