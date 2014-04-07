package org.wikapidia.spatial.core.dao.postgis;

import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import com.vividsolutions.jts.geom.Geometry;
import org.geotools.data.FeatureSource;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.text.cql2.CQL;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.spatial.core.SpatialContainerMetadata;
import org.wikapidia.spatial.core.dao.SpatialDataDao;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by bjhecht on 4/7/14.
 */
public class PostGISSpatialDataDao implements SpatialDataDao {

    private final PostGISDB db;

    // for writing data
    private List<SimpleFeature> curFeaturesToStore = null;
    private SimpleFeatureBuilder simpleFeatureBuilder = null;
    private static final Integer BUFFER_SIZE = 10;


    public PostGISSpatialDataDao(PostGISDB postGisDb){
        this.db = postGisDb;
    }

    @Override
    public Geometry getGeometry(int itemId, String layerName, String refSysName) throws DaoException {

        try {

            FeatureSource contents = db.getFeatureSource();
            String cqlQuery = String.format("item_id = %d AND layer_name = '%s' AND ref_sys_name = '%s'", itemId, layerName, refSysName);
            Filter f = CQL.toFilter(cqlQuery);
            FeatureCollection collection = contents.getFeatures(f);

            if (collection.size() == 0) return null;

            return ((Geometry)collection.features().next().getProperty(db.getGeometryAttributeName()));


        }catch(Exception e){
            throw new DaoException(e);
        }


    }

    @Override
    public Iterable<Geometry> getGeometries(int itemId) throws DaoException {
        return null;
    }

    @Override
    public Iterable<Integer> getAllItemsInLayer(String layerName, String refSysName) throws DaoException {
        return null;
    }

    @Override
    public Iterable<String> getAllRefSysNames() throws DaoException {
        return null;
    }

    @Override
    public Iterable<String> getAllLayerNames(String refSysName) throws DaoException {
        return null;
    }

    @Override
    public SpatialContainerMetadata getReferenceSystemMetadata(String refSysName) throws DaoException {
        return null;
    }

    @Override
    public SpatialContainerMetadata getLayerMetadata(String layerName, String refSysName) throws DaoException {
        return null;
    }

    @Override
    public void beginSaveGeometries() throws DaoException {
        try {
            simpleFeatureBuilder = new SimpleFeatureBuilder(db.getSchema());
        }catch(Exception e){
            throw new DaoException(e);
        }
    }

    @Override
    public void endSaveGeometries() throws DaoException {
        flushFeatureBuffer();
    }

    private void flushFeatureBuffer() throws DaoException{

        try {
            SimpleFeatureCollection featuresToStore = new ListFeatureCollection(db.getSchema(), curFeaturesToStore);
            ((SimpleFeatureStore) db.getFeatureSource()).addFeatures(featuresToStore); // GeoTools can be so weird sometimes
            curFeaturesToStore.clear();
        }catch(IOException e){
            throw new DaoException(e);
        }
    }

    @Override
    public void saveGeometry(int itemId, String layerName, String refSysName, Geometry g) throws DaoException {

        try {

            if (curFeaturesToStore == null) {
                curFeaturesToStore = Lists.newArrayList();
            }

            SimpleFeature curFeature = simpleFeatureBuilder.buildFeature("n/a", new Object[]{new Integer(itemId), layerName, refSysName, g});
            curFeaturesToStore.add(curFeature);

            if (curFeaturesToStore.size() % BUFFER_SIZE == 0){
                flushFeatureBuffer();
            }


        }catch(Exception e){
            throw new DaoException(e);
        }

    }


    public static class Provider extends org.wikapidia.conf.Provider<PostGISSpatialDataDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return SpatialDataDao.class;
        }

        @Override
        public String getPath() {
            return "spatial.dao.spatialData";
        }

        @Override
        public PostGISSpatialDataDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {

            return new PostGISSpatialDataDao( getConfigurator().get(PostGISDB.class, config.getString("dataSource")));
        }
    }

}
