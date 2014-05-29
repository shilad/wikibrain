package org.wikibrain.spatial.loader;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;

import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataFilter;
import org.wikibrain.wikidata.WikidataStatement;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Created by bjhecht on 5/21/14.
 *
 * Starting point for Aaron's OSM work.
 */
public class OSMLayerLoader {

    public static final Logger LOG = Logger.getLogger(GADMConverter.class.getName());

    private final WikidataDao wdDao;

    private static final int OSM_RELATION_ID = 402;

    public OSMLayerLoader(WikidataDao wdDao){

        this.wdDao = wdDao;

    }

    /**
     *
     * @param outputFile
     * @throws Exception
     */

    public void printGeometries(File outputFile) throws Exception{

        final SimpleFeatureType OSMTYPE = getOutputFeatureType();
        final SimpleFeatureSource outputFeatureSource = getOutputDataFeatureSource(outputFile, OSMTYPE);
        final Transaction transaction = new DefaultTransaction("create");
        final ConcurrentLinkedQueue<List<SimpleFeature>> writeQueue = new ConcurrentLinkedQueue<List<SimpleFeature>>();

        try {

            Iterable<WikidataStatement> osmRelations = getAllOSMRelations();

            ParallelForEach.iterate(osmRelations.iterator(), new Procedure<WikidataStatement>() {
                @Override
                public void call(WikidataStatement osmRelation) throws Exception {

                    int itemId = osmRelation.getItem().getId();
                    String itemLabel = osmRelation.getItem().getLabels().get(Language.SIMPLE); //language might need to change to EN

                    synchronized (this) {LOG.log(Level.INFO, "Writing " + itemLabel + " to the shapefile.");}

                    Geometry itemGeometry = getGeometry(readGeoJson(itemId));
                    List<SimpleFeature> features = buildOutputFeature(itemGeometry, itemLabel, OSMTYPE);
                    writeQueue.add(features);
                    writeToShpFile(outputFeatureSource, OSMTYPE, transaction, writeQueue.poll());

                }
            });

            /*for (WikidataStatement osmRelation : osmRelations){
                int itemId = osmRelation.getItem().getId();
                String itemLabel = osmRelation.getItem().getLabels().get(Language.SIMPLE); //language might need to change to EN

                Geometry itemGeometry = getGeometry(readGeoJson(itemId));
                List<SimpleFeature> features = buildOutputFeature(itemGeometry, itemLabel, OSMTYPE);
                writeToShpFile(outputFeatureSource, OSMTYPE, transaction, features);
            }*/


        } catch(DaoException e){
            throw new WikiBrainException(e);
        } finally {
            transaction.close();
        }

    }

    private synchronized void writeToShpFile(SimpleFeatureSource outputFeatureSource, SimpleFeatureType outputFeatureType, Transaction transaction, List<SimpleFeature> features) throws IOException {
        if (outputFeatureSource instanceof SimpleFeatureStore) {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) outputFeatureSource;

            SimpleFeatureCollection collection = new ListFeatureCollection(outputFeatureType, features);
            featureStore.setTransaction(transaction);
            try {
                featureStore.addFeatures(collection);
                transaction.commit();
            } catch (Exception e) {
                e.printStackTrace();
                transaction.rollback();
            }
        } else {
            LOG.log(Level.INFO, outputFeatureType.getTypeName() + " does not support read/write access");
        }

    }

    private List<SimpleFeature> buildOutputFeature(Geometry itemGeom, String itemLabel, SimpleFeatureType outputFeatureType) {

        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(outputFeatureType);
        featureBuilder.add(itemGeom);
        featureBuilder.add(itemLabel);

        SimpleFeature feature = featureBuilder.buildFeature(null);

        List<SimpleFeature> features = new ArrayList<SimpleFeature>();
        features.add(feature);

        return features;

    }

    private SimpleFeatureSource getOutputDataFeatureSource(File outputFile, SimpleFeatureType type) throws IOException {

        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        Map<String, Serializable> outputParams = new HashMap<String, Serializable>();
        outputParams.put("url", outputFile.toURI().toURL());
        outputParams.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore outputDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(outputParams);
        outputDataStore.createSchema(type);
        String typeName = outputDataStore.getTypeNames()[0];
        return outputDataStore.getFeatureSource(typeName);

    }

    private Geometry getGeometry(String geoJsonString) throws IOException {

        GeometryJSON geoJson = new GeometryJSON();
        Reader reader = new StringReader(geoJsonString);

        return geoJson.readGeometryCollection(reader);

    }

    private String readGeoJson(int itemId) throws MalformedURLException, IOException{

        String baseURL = "http://tools.wmflabs.org/wiwosm/osmjson/getGeoJSON.php?lang=wikidata&article=Q";
        URL jsonURL = new URL(baseURL + itemId);
        String geoJson;

        BufferedReader in = new BufferedReader(new InputStreamReader(jsonURL.openStream()));
        geoJson = in.readLine();
        in.close();

        return geoJson;
    }

    private SimpleFeatureType getOutputFeatureType() {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("OSMTYPE");
        typeBuilder.setCRS(DefaultGeographicCRS.WGS84);
        typeBuilder.add("the_geom", MultiPolygon.class);
        typeBuilder.add("TITLE1_EN", String.class);
        typeBuilder.setDefaultGeometry("the_geom");

        return typeBuilder.buildFeatureType();

    }

    public Iterable<WikidataStatement> getAllOSMRelations() throws DaoException{

        WikidataFilter filter = (new WikidataFilter.Builder()).withPropertyId(OSM_RELATION_ID).build();

        Iterable<WikidataStatement> osmRelations = wdDao.get(filter);

        return osmRelations;
    }



}
