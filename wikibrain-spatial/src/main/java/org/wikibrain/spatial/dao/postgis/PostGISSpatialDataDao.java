package org.wikibrain.spatial.dao.postgis;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.typesafe.config.Config;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.Title;
import org.wikibrain.spatial.SpatialContainerMetadata;
import org.wikibrain.spatial.constants.Precision;
import org.wikibrain.spatial.constants.RefSys;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.wikidata.WikidataDao;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by bjhecht on 4/7/14.
 */
public class PostGISSpatialDataDao implements SpatialDataDao {

    private final PostGISDB db;
    private final WikidataDao wikidataDao;
    private final LocalPageDao localPageDao;

    // for writing data
    private ArrayBlockingQueue<SimpleFeature> curFeaturesToStore = null;
    private ThreadLocal<SimpleFeatureBuilder> simpleFeatureBuilder;
    private static final Integer BUFFER_SIZE = 200;

    private static final Logger LOG = LoggerFactory.getLogger(PostGISDB.class);

    private PostGISSpatialDataDao(PostGISDB postGisDb, WikidataDao wikidataDao, LocalPageDao localPageDao){
        this.db = postGisDb;
        this.wikidataDao = wikidataDao;
        this.localPageDao = localPageDao;
        this.curFeaturesToStore = new ArrayBlockingQueue<SimpleFeature>(BUFFER_SIZE * 10);
        simpleFeatureBuilder = new ThreadLocal<SimpleFeatureBuilder>() {
            @Override
            public SimpleFeatureBuilder initialValue() {
                try {
                    return new SimpleFeatureBuilder(db.getSchema());
                } catch (DaoException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Override
    public Geometry getGeometry(int itemId, String layerName, String refSysName) throws DaoException {

        return db.getGeometry(itemId, layerName, refSysName);

    }

    @Override
    public Map<String, Geometry> getGeometries(int itemId) throws DaoException {
        Map<String, Geometry> result = new HashMap<String, Geometry>();
        for (String refSys : this.getAllRefSysNames()){
            for (String layer : this.getAllLayerNames(refSys)){
                Geometry g = getGeometry(itemId, layer, refSys);
                if (g != null) {
                    result.put(layer, g);
                }
            }
        }
        return result;
    }

    @Override
    public Map<Integer, Geometry> getAllGeometriesInLayer(String layerName, String refSysName) throws DaoException {
        return db.getAllGeometriesInLayer(layerName, refSysName);
    }

    @Override
    public Map<Integer, Geometry> getAllGeometriesInLayer(String layerName) throws DaoException {
        return getAllGeometriesInLayer(layerName, RefSys.EARTH);
    }

    @Override
    public Map<Integer, Geometry> getAllGeometriesInLayer(String layerName, Precision.LatLonPrecision minPrecision) throws DaoException {

        Map<Integer, Geometry> geoms = getAllGeometriesInLayer(layerName);
        Set<Integer> keys = Sets.newHashSet();

        for (Integer curKey : keys){
            Geometry g = geoms.get(curKey);
            if (this.filterByPrecision(g, minPrecision) == null){
                geoms.remove(curKey);
            }
        }

        return geoms;

    }

    @Override
    public Map<Integer, Geometry> getAllGeometriesInLayer(String layerName, String[] notInLayers, String refSysName) throws DaoException {

        Map<Integer, Geometry> rVal = this.getAllGeometriesInLayer(layerName, refSysName);

        for (String notInLayer : notInLayers){
            Map<Integer, Geometry> temp = this.getAllGeometriesInLayer(notInLayer, refSysName);
            if (temp != null) {
                for (Integer key : temp.keySet()) {
                    rVal.remove(key);
                }
            }else{
                LOG.warn("Could not find any geometries in layer: " + layerName);
            }
        }

        return rVal;

    }

    @Override
    public Iterable<String> getAllRefSysNames() throws DaoException {
        return db.getAllReferenceSystems();
    }

    @Override
    public Iterable<String> getAllLayerNames(String refSysName) throws DaoException {
        return db.getLayersInReferenceSystem(refSysName);
    }

    @Override
    public SpatialContainerMetadata getReferenceSystemMetadata(String refSysName) throws DaoException {

        try{
            int count = 0;
            SpatialContainerMetadata rVal = null;
            for (String layerName : getAllLayerNames(refSysName)){
                if (rVal == null){
                    rVal = getLayerMetadata(layerName, refSysName);
                }else{
                    rVal.merge(getLayerMetadata(layerName, refSysName));
                }
                count++;
            }
            rVal.toReferenceSystem();
            return rVal;
        }catch(WikiBrainException e){
            throw new DaoException(e);
        }


    }

    @Override
    public SpatialContainerMetadata getLayerMetadata(String layerName, String refSysName) throws DaoException {

        SpatialContainerMetadata rVal = db.getLayerMetadata(layerName, refSysName);
        return rVal;

    }

    @Override
    public Geometry getGeometry(int itemId, String layerName) throws DaoException {
        return getGeometry(itemId, layerName, RefSys.EARTH);
    }

    @Override
    public Geometry getGeometry(int itemId, String layerName, Precision.LatLonPrecision minPrecision) throws DaoException {

        Geometry g = this.getGeometry(itemId, layerName);
        return filterByPrecision(g, minPrecision);

    }


    @Override
    public Geometry getGeometry(String articleName, Language language, String layerName) throws DaoException {

        return getGeometry(articleName, language, layerName, RefSys.EARTH);

    }

    private Geometry filterByPrecision(Geometry g, Precision.LatLonPrecision minPrecision){

        if (g == null) return null;

        if (!(g instanceof Point)) return g;

        if (Precision.isGreaterThanOrEqualTo(Precision.getLatLonPrecision((Point)g), minPrecision)){
            return g;
        }else{
            return null;
        }

    }

    @Override
    public Geometry getGeometry(String articleName, Language language, String layerName, Precision.LatLonPrecision minPrecision) throws DaoException {

        Geometry g = getGeometry(articleName, language, layerName);
        return filterByPrecision(g, minPrecision);

    }

    @Override
    public Geometry getGeometry(String articleName, Language language, String layerName, String refSysName) throws DaoException {

        LocalPage lp = localPageDao.getByTitle(new Title(articleName, language), NameSpace.ARTICLE);
        if (lp == null) return null;
        Integer id = wikidataDao.getItemId(lp);
        if (id == null) throw new DaoException("Could not find Wikidata item for \"" + lp.toString() + "\"");

        return getGeometry(id, layerName);
    }

    @Override
    public Map<Integer, Geometry> getBulkGeometriesInLayer(List<Integer> idList, String layerName, String refSysName) throws DaoException{
        return db.getBulkGeometriesInLayer(idList, layerName, refSysName);
    }


    @Override
    public void beginSaveGeometries() throws DaoException {
        try {
            curFeaturesToStore.clear();
        }catch(Exception e){
            throw new DaoException(e);
        }
    }

    @Override
    public void endSaveGeometries() throws DaoException {
        flushFeatureBuffer(true);
    }

    private void flushFeatureBuffer(boolean force) throws DaoException{
        try {
            List<SimpleFeature> batch = new ArrayList<SimpleFeature>();
            synchronized (curFeaturesToStore) {
                if (!force && curFeaturesToStore.size() < BUFFER_SIZE) {
                    return;
                }
                curFeaturesToStore.drainTo(batch);
            }
            SimpleFeatureCollection featuresToStore = new ListFeatureCollection(db.getSchema(), batch);
            ((SimpleFeatureStore) db.getFeatureSource()).addFeatures(featuresToStore); // GeoTools can be so weird sometimes
        }catch(IOException e){
            throw new DaoException(e);
        }
    }

    @Override
    public void saveGeometry(int itemId, String layerName, String refSysName, Geometry g) throws DaoException {

        try {
            SimpleFeature curFeature = simpleFeatureBuilder.get().buildFeature("n/a", new Object[]{new Integer(itemId), layerName, refSysName, g});
            curFeaturesToStore.put(curFeature);
            if (curFeaturesToStore.size() > BUFFER_SIZE){
                flushFeatureBuffer(false);
            }


        }catch(Exception e){
            throw new DaoException(e);
        }

    }

    @Override
    public void removeLayer(String refSysName, String layerName) throws DaoException {
        db.removeLayer(refSysName, layerName);
    }

    @Override
    public void optimize() throws DaoException {
        db.optimize();
    }

    public static class Provider extends org.wikibrain.conf.Provider<PostGISSpatialDataDao> {
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
        public PostGISSpatialDataDao get(String name, Config config,
                                         Map<String, String> runtimeParams) throws ConfigurationException {

            return new PostGISSpatialDataDao( getConfigurator().get(PostGISDB.class, config.getString("dataSource")),
                    getConfigurator().get(WikidataDao.class), getConfigurator().get(LocalPageDao.class));

        }
    }

}
