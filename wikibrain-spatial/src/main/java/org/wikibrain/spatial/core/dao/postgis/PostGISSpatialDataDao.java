package org.wikibrain.spatial.core.dao.postgis;

import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import com.vividsolutions.jts.geom.Geometry;
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
import org.wikibrain.spatial.core.SpatialContainerMetadata;
import org.wikibrain.spatial.core.constants.RefSys;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.wikidata.WikidataDao;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by bjhecht on 4/7/14.
 */
public class PostGISSpatialDataDao implements SpatialDataDao {

    private final PostGISDB db;
    private final WikidataDao wikidataDao;
    private final LocalPageDao localPageDao;

    // for writing data
    private List<SimpleFeature> curFeaturesToStore = null;
    private SimpleFeatureBuilder simpleFeatureBuilder = null;
    private static final Integer BUFFER_SIZE = 200;



    private PostGISSpatialDataDao(PostGISDB postGisDb, WikidataDao wikidataDao, LocalPageDao localPageDao){

        this.db = postGisDb;
        this.wikidataDao = wikidataDao;
        this.localPageDao = localPageDao;
    }

    @Override
    public Geometry getGeometry(int itemId, String layerName, String refSysName) throws DaoException {

        return db.getGeometry(itemId, layerName, refSysName);

    }

    @Override
    public Iterable<Geometry> getGeometries(int itemId) throws DaoException {

        List<Geometry> rVal = Lists.newArrayList();
        for (String refSys : this.getAllRefSysNames()){
            for (String layer : this.getAllLayerNames(refSys)){
                Geometry g = getGeometry(itemId, layer, refSys);
                rVal.add(g);
            }
        }
        return rVal;

    }

    @Override
    public Map<Integer, Geometry> getAllGeometriesInLayer(String layerName, String refSysName) throws DaoException {
        return db.getAllGeometriesInLayer(layerName, refSysName);
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
    public Geometry getGeometry(String articleName, Language language, String layerName) throws DaoException {

        return getGeometry(articleName, language, layerName, RefSys.EARTH);

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
            if(curFeaturesToStore != null){
                SimpleFeatureCollection featuresToStore = new ListFeatureCollection(db.getSchema(), curFeaturesToStore);
                ((SimpleFeatureStore) db.getFeatureSource()).addFeatures(featuresToStore); // GeoTools can be so weird sometimes
                curFeaturesToStore.clear();
            }
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
