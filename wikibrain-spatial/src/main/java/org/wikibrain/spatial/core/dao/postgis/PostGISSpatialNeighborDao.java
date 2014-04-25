package org.wikibrain.spatial.core.dao.postgis;

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
import org.wikibrain.spatial.core.dao.SpatialContainmentDao;
import org.wikibrain.spatial.core.dao.SpatialNeighborDao;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

            return rVal;

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
