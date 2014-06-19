package org.wikibrain.spatial.loader;

import com.vividsolutions.jts.geom.*;
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
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.WKTReader2;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;

import org.wikibrain.core.lang.UniversalId;
import org.wikibrain.core.model.Title;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataEntity;
import org.wikibrain.wikidata.WikidataFilter;
import org.wikibrain.wikidata.WikidataStatement;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;




/**
 * Created by bjhecht on 5/21/14.
 *
 */
public class OSMLayerLoader {

    public static final Logger LOG = Logger.getLogger(GADMConverter.class.getName());

    private final WikidataDao wdDao;


    private static final int OSM_RELATION_ID = 402;

    public OSMLayerLoader(WikidataDao wdDao){

        this.wdDao = wdDao;

    }

    //public OSMLayerLoader(){}

    /**
     * Print geometries of the wikidata items with OSM relation ID to a shapefile.
     * @param outputFile
     * @throws Exception
     */

    public void printGeometries(File outputFile) throws Exception{

        final SimpleFeatureType OSMTYPE = getOutputFeatureType();
        final SimpleFeatureSource outputFeatureSource = getOutputDataFeatureSource(outputFile, OSMTYPE);
        final Transaction transaction = new DefaultTransaction("create");
        final ConcurrentLinkedQueue<List<SimpleFeature>> writeQueue = new ConcurrentLinkedQueue<List<SimpleFeature>>();
        final List<String> exceptionList = new ArrayList<String>();

        try {

            Iterable<WikidataStatement> osmRelations = getAllOSMRelations();
            final AtomicInteger count = new AtomicInteger(0);


            ParallelForEach.iterate(osmRelations.iterator(), new Procedure<WikidataStatement>() {
                @Override
                public void call(WikidataStatement osmRelation) throws Exception {

                    int relationId = Integer.parseInt(osmRelation.getValue().getStringValue());
                    String itemLabel = wdDao.getItem(osmRelation.getItem().getId()).getLabels().get(Language.EN);
                    count.incrementAndGet();

                    synchronized (this) {LOG.log(Level.INFO, "Writing " + itemLabel + " to the shapefile.");}
                    try{

                    Geometry itemGeometry = getGeometry(readWkt(relationId));
                    if (itemGeometry != null){
                        List<SimpleFeature> features = buildOutputFeature(itemGeometry, itemLabel, OSMTYPE);
                        writeQueue.add(features);
                        writeToShpFile(outputFeatureSource, OSMTYPE, transaction, writeQueue.poll());
                    }
                    } catch(Exception e){
                        synchronized (this) {exceptionList.add(String.format("%s, %d\n", itemLabel, relationId));}
                    }

                }
            });


            /*for (WikidataStatement osmRelation : osmRelations){
                int relationId = Integer.parseInt(osmRelation.getValue().getStringValue());
                String itemLabel = wdDao.getItem(osmRelation.getItem().getId()).getLabels().get(Language.EN); //language might need to change to EN

                LOG.log(Level.INFO, "Writing " + itemLabel + " to the shapefile.");

                try{
                    Geometry itemGeometry = getGeometry(readWkt(relationId));
                    if (itemGeometry != null) {
                        List<SimpleFeature> features = buildOutputFeature(itemGeometry, itemLabel, OSMTYPE);
                        writeToShpFile(outputFeatureSource, OSMTYPE, transaction, features);
                    }
                } catch (Exception e){
                    e.printStackTrace();
                    exceptionList.add(String.format("%s, %d\n", itemLabel, relationId));
                }
            }*/
            for (String country: exceptionList){
                System.out.println(country);
            }


            /*int itemId = 270056;
            String itemLabel = "China";

            synchronized (this) {LOG.log(Level.INFO, "Writing " + itemLabel + " to the shapefile.");}

            Geometry itemGeometry = getGeometry(readWkt(itemId));
            List<SimpleFeature> features = buildOutputFeature(itemGeometry, itemLabel, OSMTYPE);
            writeQueue.add(features);
            writeToShpFile(outputFeatureSource, OSMTYPE, transaction, writeQueue.poll());*/


        } catch(Exception e){
            e.printStackTrace();
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

    private Geometry getGeometry(String wktString) throws Exception {
        //This is super hacky.

        WKTReader2 wktReader = new WKTReader2();
        if (wktString.equals("None")) return null;
        Geometry rawGeometry = wktReader.read(wktString.substring(10));

        return rawGeometry;


    }

    private String readWkt(int relationId) throws MalformedURLException, IOException{

        String base = "http://polygons.openstreetmap.fr/index.py?id=";
        URL baseURL = new URL(base + relationId);
        URLConnection baseUrlConnection = baseURL.openConnection();
        baseUrlConnection.getContent();
        BufferedReader baseIn = new BufferedReader(new InputStreamReader(baseUrlConnection.getInputStream()));
        while(baseIn.readLine()!=null)
            continue;

        URL wktURL = new URL(String.format("http://polygons.openstreetmap.fr/get_wkt.py?id=%d&params=0", relationId));
        URLConnection c = wktURL.openConnection();

        BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
        String wkt = in.readLine();
        System.out.println(wkt.trim());
        in.close();

        return wkt.trim();
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

    private Iterable<WikidataStatement> getAllOSMRelations() throws DaoException{

        WikidataFilter filter = (new WikidataFilter.Builder()).withPropertyId(OSM_RELATION_ID).build();

        Iterable<WikidataStatement> osmRelations = wdDao.get(filter);

        return osmRelations;
    }



}
