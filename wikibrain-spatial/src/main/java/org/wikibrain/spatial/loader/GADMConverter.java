package org.wikibrain.spatial.loader;


import com.google.common.collect.*;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import de.tudarmstadt.ukp.wikipedia.parser.Link;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.apache.commons.io.FileUtils;

import org.geotools.data.simple.SimpleFeatureIterator;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.data.*;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.wikibrain.core.WikiBrainException;

import org.wikibrain.spatial.core.constants.RefSys;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpIOUtils;

import org.wikibrain.download.*;


import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Created by aaroniidx on 4/13/14.
 */
public class GADMConverter {


    public static final Logger LOG = Logger.getLogger(GADMConverter.class.getName());

    public void downloadAndConvert(SpatialDataFolder folder) throws WikiBrainException {

        try {


            WpIOUtils ioUtils = new WpIOUtils();
            String tmpFolderName = "_gadmdownload";

            File tmpFolder = WpIOUtils.createTempDirectory(tmpFolderName, true);


            // Download to a temp folder (Note that WikiBrain will ignore all reference systems that begin with "_"
            //folder.createNewReferenceSystemIfNotExists(tmpFolder.getCanonicalPath());
            File rawFile = downloadGADMShapeFile(tmpFolder.getCanonicalPath());
            //File rawFile = new File("tmp/gadm_v2_shp/gadm2.shp");

            //copy level 2 shapefile to earth reference system
            LOG.log(Level.INFO, "Copying level 2 shapefiles to " + folder.getRefSysFolder("earth").getCanonicalPath());
            FileUtils.copyDirectory(new File(tmpFolder.getCanonicalPath()), folder.getRefSysFolder("earth"));

            // convert file and save as layer in earth reference system
            LOG.log(Level.INFO, "Start mapping level 1 shapefiles.");
            convertShpFile(rawFile, folder, 1);
            LOG.log(Level.INFO, "Start mapping level 0 shapefiles.");
            convertShpFile(rawFile, folder, 0);


        } catch (Exception e) {
            throw new WikiBrainException(e);
        } finally {
            folder.deleteSpecificFile("read_me.pdf", RefSys.EARTH);
            folder.deleteLayer("gadm2", RefSys.EARTH);
        }


    }

    /**
     * Download GADM shape file
     *
     * @param tmpFolder
     * @return
     */
    public File downloadGADMShapeFile(String tmpFolder) throws IOException, ZipException, InterruptedException {

        String baseFileName = "gadm_v2_shp";
        String zipFileName = baseFileName + ".zip";
        String gadmURL = "http://biogeo.ucdavis.edu/data/gadm2/" + zipFileName;
        File gadmShapeFile = new File(tmpFolder + "/" + zipFileName);

        FileDownloader downloader = new FileDownloader();
        downloader.download(new URL(gadmURL), gadmShapeFile);
        ZipFile zipFile = new ZipFile(gadmShapeFile.getCanonicalPath());
        LOG.log(Level.INFO, "Extracting to " + gadmShapeFile.getParent());
        zipFile.extractAll(gadmShapeFile.getParent());
        File f = new File(tmpFolder + "/gadm2.shp");
        LOG.log(Level.INFO, "Extraction complete.");
        gadmShapeFile.delete();
        return f;


    }

    //private int countryCount = 0;
    private AtomicInteger countryCount = new AtomicInteger(0);
    private List<String> exceptionList;


    /**
     * @param outputFolder
     * @param level        //TODO: reduce memory usage
     *                     Converts raw GADM shapefile into WikiBrain readable type
     *                     Recommended JVM max heapsize = 4G
     */


    public void convertShpFile(File rawFile, SpatialDataFolder outputFolder, int level) throws IOException, WikiBrainException {
        if ((level != 0) && (level != 1)) throw new IllegalArgumentException("Level must be 0 or 1");

        File outputFile = new File(outputFolder.getRefSysFolder("earth").getCanonicalPath() + "/" + "gadm" + level + ".shp");
        ListMultimap<String, String> countryStatePair = ArrayListMultimap.create();

        final SimpleFeatureType WIKITYPE = getOutputFeatureType(level);

        final SimpleFeatureSource outputFeatureSource = getOutputDataFeatureSource(outputFile, WIKITYPE);

        final Transaction transaction = new DefaultTransaction("create");

        final SimpleFeatureCollection inputCollection = getInputCollection(rawFile);
        SimpleFeatureIterator inputFeatures = inputCollection.features();

        final ConcurrentLinkedQueue<List<SimpleFeature>> writeQueue = new ConcurrentLinkedQueue<List<SimpleFeature>>();

        try {

            while (inputFeatures.hasNext()) {
                SimpleFeature feature = inputFeatures.next();
                String country = ((String) feature.getAttribute(4)).intern();
                String state = ((String) feature.getAttribute(6)).intern();
                if (!countryStatePair.containsEntry(country, state))
                    countryStatePair.put(country, state);
            }

            final Multimap<String, String> countryState = countryStatePair;

            inputFeatures.close();

            exceptionList = new ArrayList<String>();

            LOG.log(Level.INFO, "Start processing polygons for level " + level + " administrative districts.");



            if (level == 1) {
                for (String country : countryState.keySet()) {

                    ParallelForEach.loop(countryState.get(country), new Procedure<String>() {
                        @Override
                        public void call(String state) throws Exception {

                            List<SimpleFeature> features = inputFeatureHandler(inputCollection, state, 1, WIKITYPE, countryState);
                            writeQueue.add(features);
                            writeToShpFile(outputFeatureSource, WIKITYPE, transaction, writeQueue.poll());
                        }
                    });
                }


            } else {

                ParallelForEach.loop(countryState.keySet(), new Procedure<String>() {
                    @Override
                    public void call(String country) throws Exception {

                        List<SimpleFeature> features = inputFeatureHandler(inputCollection, country, 0, WIKITYPE, countryState);
                        writeQueue.add(features);
                        writeToShpFile(outputFeatureSource, WIKITYPE, transaction, writeQueue.poll());

                    }
                });

                LOG.log(Level.INFO, "Start processing polygons where exceptions occurred.");
                int count = 0;
                for (String country: exceptionList) {
                    count++;
                    LOG.log(Level.INFO, "Combining polygons for " + country + "(" + count + "/" + exceptionList.size() + ")");
                    List<SimpleFeature> features = inputFeatureHandler(inputCollection, country, 0, WIKITYPE, countryState);
                    writeToShpFile(outputFeatureSource, WIKITYPE, transaction, features);
                }


            }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            transaction.close();
            countryCount.set(0);
        }


    }

    private List<SimpleFeature> inputFeatureHandler(SimpleFeatureCollection inputCollection, String featureName, int level, SimpleFeatureType outputFeatureType, Multimap<String, String> relation) {

        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        List<Geometry> geometryList = new ArrayList<Geometry>();
        SimpleFeatureIterator inputFeatures = inputCollection.features();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(outputFeatureType);
        Multimap<String, String> reverted = ArrayListMultimap.create();
        Geometry newGeom = null;
        String country;


        if (!exceptionList.contains(featureName)) {
            if (level == 1) {
                country = (String) Multimaps.invertFrom(relation, reverted).get(featureName).toArray()[0];
                synchronized (this) {LOG.log(Level.INFO, "Combining polygons for level 1 administrative district: " + featureName + " in " + country + " (" + countryCount.incrementAndGet() + "/" + relation.keySet().size() + ")");}
            } else {
                country = featureName;
                synchronized (this) {LOG.log(Level.INFO, "Combining polygons for " + country + " (" + countryCount.incrementAndGet() + "/" + relation.keySet().size() + ")");}
            }
        }

        while (inputFeatures.hasNext()) {
            SimpleFeature feature = inputFeatures.next();
            if (level == 1) {
                if (feature.getAttribute(6).equals(featureName)) geometryList.add((Geometry) feature.getAttribute(0));
            } else if (feature.getAttribute(4).equals(featureName))
                geometryList.add((Geometry) feature.getAttribute(0));
        }
        inputFeatures.close();

        try {
            newGeom = geometryFactory.buildGeometry(geometryList).union().getBoundary();

        } catch (Exception e) {
            LOG.log(Level.INFO, "Exception occurred at " + featureName + ": " + e.getMessage() + ". Attempting different combining methods.");
            if (level == 1 || exceptionList.contains(featureName))
                newGeom = geometryFactory.buildGeometry(geometryList).buffer(0).getBoundary();
            else exceptionList.add(featureName);

        }

        featureBuilder.add(newGeom);
        if (level == 1) {
            featureBuilder.add(featureName);
            featureBuilder.add(featureName + ", " + Multimaps.invertFrom(relation, reverted).get(featureName).toArray()[0]);
        } else
            featureBuilder.add(featureName);
        SimpleFeature feature = featureBuilder.buildFeature(null);

        List<SimpleFeature> features = new ArrayList<SimpleFeature>();
        features.add(feature);

        return features;

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
            LOG.log(Level.INFO, "WIKITYPE does not support read/write access");
        }

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

    private SimpleFeatureCollection getInputCollection(File rawFile) throws IOException {

        Map<String, URL> map = new HashMap<String, URL>();
        map.put("url", rawFile.toURI().toURL());
        DataStore inputDataStore = DataStoreFinder.getDataStore(map);
        SimpleFeatureSource inputFeatureSource = inputDataStore.getFeatureSource(inputDataStore.getTypeNames()[0]);
        return inputFeatureSource.getFeatures();


    }

    private SimpleFeatureType getOutputFeatureType(int level) {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("WIKITYPE");
        typeBuilder.setCRS(DefaultGeographicCRS.WGS84);
        typeBuilder.add("the_geom", MultiPolygon.class);
        typeBuilder.add("TITLE1_EN", String.class);
        if (level == 1) typeBuilder.add("TITLE2_EN", String.class);
        typeBuilder.setDefaultGeometry("the_geom");

        return typeBuilder.buildFeatureType();

    }


//            while (inputFeatures.hasNext()) {
//                SimpleFeature feature = inputFeatures.next();
//                String country = ((String) feature.getAttribute(4)).intern();
//                String state = ((String) feature.getAttribute(6)).intern();
//                if (!locList.contains(state + "_" + country))
//                    locList.add(state + "_" + country);
//            }

//            for (String stateCountryPair : locList) {
//
//                geoList = new ArrayList<Geometry>();
//                String state = stateCountryPair.split("_")[0];
//                String country = stateCountryPair.split("_")[1];
//                inputFeatures = inputCollection.features();
//                if (level == 1) {
//                    count++;
//                    if (count % 10 == 0)
//                        LOG.log(Level.INFO, count + "/" + total + " level 1 administrative districts processed.");
//
//                    while (inputFeatures.hasNext()) {
//                        SimpleFeature feature = inputFeatures.next();
//                        if (feature.getAttribute(6).equals(state) && feature.getAttribute(4).equals(country))
//                            geoList.add((Geometry) feature.getAttribute(0));
//                    }
//                } else {
//                    if (!visited.contains(country)) {
//                        visited.add(country);
//                    } else continue;
//
//                    count++;
//                    LOG.log(Level.INFO, "Combining polygons for " + country + " (" + count + "/" + total + ")");
//
//                    while (inputFeatures.hasNext()) {
//                        SimpleFeature feature = inputFeatures.next();
//                        if (feature.getAttribute(4).equals(country))
//                            geoList.add((Geometry) feature.getAttribute(0));
//                    }
//                }
//
//                inputFeatures.close();
//                Geometry newGeom;
//
//                try {
//                    newGeom = geometryFactory.buildGeometry(geoList).union();
//                } catch (Exception e) {
//                    if (level == 1)
//                        LOG.log(Level.INFO, "Exception occurred at " + state + ": " + e.getMessage() + ". Attempting different combining methods.");
//                    else
//                        LOG.log(Level.INFO, "Exception occurred at " + country + ": " + e.getMessage() + ". Attempting different combining methods.");
//                    newGeom = geometryFactory.buildGeometry(geoList).buffer(0);
//                }
//
//                featureBuilder.add(newGeom);
//                if (level == 1) {
//                    featureBuilder.add(state);
//                    featureBuilder.add(state + ", " + country);
//                } else
//                    featureBuilder.add(country);
//                SimpleFeature feature = featureBuilder.buildFeature(null);
//
//                List<SimpleFeature> features = new ArrayList<SimpleFeature>();
//                features.add(feature);
//
//                if (outputFeatureSource instanceof SimpleFeatureStore) {
//                    SimpleFeatureStore featureStore = (SimpleFeatureStore) outputFeatureSource;
//
//                    SimpleFeatureCollection collection = new ListFeatureCollection(WIKITYPE, features);
//                    featureStore.setTransaction(transaction);
//                    try {
//                        featureStore.addFeatures(collection);
//                        transaction.commit();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        transaction.rollback();
//                    }
//                } else {
//                    LOG.log(Level.INFO, "WIKITYPE does not support read/write access");
//                }
//            }


//    public static void convertShpFile(File rawFile, SpatialDataFolder outputFolder, int level) throws IOException {
//        if ((level != 0) && (level != 1)) throw new IllegalArgumentException("Level must be 0 or 1");
//
//        File outputFile;
//        Map map = new HashMap();
//        HashMap<String, List<Geometry>> stateShape = new HashMap<String, List<Geometry>>();
//        HashMap<String, String> stateCountry = new HashMap<String, String>();
//        if (level == 1)
//            outputFile = new File(outputFolder.getRefSysFolder("earth").getCanonicalPath() + "/" + "gadm1.shp");
//        else
//            outputFile = new File(outputFolder.getRefSysFolder("earth").getCanonicalPath() + "/" + "gadm0.shp");
//
//        ShapefileReader shpReader;
//        GeometryFactory geometryFactory;
//        SimpleFeatureTypeBuilder typeBuilder;
//        SimpleFeatureBuilder featureBuilder;
//        DataStore inputDataStore;
//        List<SimpleFeature> features = new ArrayList<SimpleFeature>();
//
//        try {
//            map.put("url", rawFile.toURI().toURL());
//            inputDataStore = DataStoreFinder.getDataStore(map);
//            SimpleFeatureSource inputFeatureSource = inputDataStore.getFeatureSource(inputDataStore.getTypeNames()[0]);
//            SimpleFeatureCollection inputCollection = inputFeatureSource.getFeatures();
//            SimpleFeatureIterator inputFeatures = inputCollection.features();
//
//
//            //level 1 mapping
//            if (level == 1) {
//                LOG.log(Level.INFO, "Mapping polygons to the corresponding administrative districts.");
//                while (inputFeatures.hasNext()) {
//                    SimpleFeature feature = inputFeatures.next();
//                    String country = ((String) feature.getAttribute(4)).intern();
//                    String state = ((String) feature.getAttribute(6)).intern();
//
//                    if (!stateShape.containsKey(state)) {
//                        stateShape.put(state, new ArrayList<Geometry>());
//                        stateCountry.put(state, country); //set up the state-country map
//                    }
//
//                    stateShape.get(state).add((Geometry) feature.getAttribute(0)); //and put all the polygons under a state into another map
//                }
//            } else {
//                //level 0 mapping
//                LOG.log(Level.INFO, "Mapping polygons to the corresponding countries.");
//                while (inputFeatures.hasNext()) {
//                    SimpleFeature feature = inputFeatures.next();
//                    String country = ((String) feature.getAttribute(4)).intern();
//
//                    if (!stateShape.containsKey(country))
//                        stateShape.put(country, new ArrayList<Geometry>());
//
//                    stateShape.get(country).add((Geometry) feature.getAttribute(0));
//                }
//            }
//            inputFeatures.close();
//            inputDataStore.dispose();
//
//            LOG.log(Level.INFO, "Mapping complete.");
//
//            typeBuilder = new SimpleFeatureTypeBuilder();  //build the output feature type
//            typeBuilder.setName("WIKITYPE");
//            typeBuilder.setCRS(DefaultGeographicCRS.WGS84);
//            typeBuilder.add("the_geom", MultiPolygon.class);
//            typeBuilder.add("TITLE1_EN", String.class);
//            if (level == 1) typeBuilder.add("TITLE2_EN", String.class);
//            typeBuilder.setDefaultGeometry("the_geom");
//
//
//            final SimpleFeatureType WIKITYPE = typeBuilder.buildFeatureType();
//
//            geometryFactory = JTSFactoryFinder.getGeometryFactory();
//            featureBuilder = new SimpleFeatureBuilder(WIKITYPE);
//
//            LOG.log(Level.INFO, "Combining polygons that belongs to the same administrative districts.");
//
//            int count = 0;
//            if (level == 1) {
//                int total = stateShape.keySet().size(); //TODO: We need a lot more in the way of progress statements here
//                for (String state : stateCountry.keySet()) {    //create the feature collection for the new shpfile
//                    count++;
//                    try {
//                        Geometry newGeom = geometryFactory.buildGeometry(stateShape.get(state)).union();
//                        featureBuilder.add(newGeom);
//                        featureBuilder.add(state);
//                        featureBuilder.add(state + ", " + stateCountry.get(state));
//                        SimpleFeature feature = featureBuilder.buildFeature(null);
//                        if (count % 10 == 0)
//                            LOG.log(Level.INFO, count + "/" + total + " level 1 administrative districts processed.");
//                        features.add(feature);
//                        stateShape.remove(state);
//                        System.gc();
//                    }
//                    catch (Exception e) {
//                        Geometry newGeom = geometryFactory.buildGeometry(stateShape.get(state)).buffer(0);
//                        featureBuilder.add(newGeom);
//                        featureBuilder.add(state);
//                        featureBuilder.add(state + ", " + stateCountry.get(state));
//                        SimpleFeature feature = featureBuilder.buildFeature(null);
//                        if (count % 10 == 0)
//                            LOG.log(Level.INFO, count + "/" + total + " level 1 administrative districts processed.");
//                        features.add(feature);
//                        LOG.log(Level.INFO, "Exception occured at " + state + ": " + e.getMessage() + ". Attempting different combining methods.");
//                        stateShape.remove(state);
//                        System.gc();
//                        continue;
//                    }
//                }
//            } else {
//                int total = stateShape.keySet().size();
//                HashSet<String> countryList = new HashSet<String>();
//                countryList.addAll(stateShape.keySet());
//                for (String country: countryList) {
//                    count++;
//                    try {
//                        LOG.log(Level.INFO, "Combining " + stateShape.get(country).size() + " polygons for " + country + " (" + count + "/" + total + ")");
//                        Geometry newGeom = geometryFactory.buildGeometry(stateShape.get(country)).union();
//                        featureBuilder.add(newGeom);
//                        featureBuilder.add(country);
//                        SimpleFeature feature = featureBuilder.buildFeature(null);
//                        features.add(feature);
//                        stateShape.remove(country);
//                        System.gc();
//                    }
//                    catch (Exception e){
//                        LOG.log(Level.INFO, "Exception occured at " + country + " : " + e.getMessage() + ". Attempting different combining methods.");
//                        Geometry newGeom = geometryFactory.buildGeometry(stateShape.get(country)).buffer(0);
//                        featureBuilder.add(newGeom);
//                        featureBuilder.add(country);
//                        SimpleFeature feature = featureBuilder.buildFeature(null);
//                        features.add(feature);
//                        stateShape.remove(country);
//                        System.gc();
//                        continue;
//                    }
//                }
//            }
//
//            if (level == 1) {
//                LOG.log(Level.INFO, "Combining complete. " + count + " administrative districts processed.");
//            } else {
//                LOG.log(Level.INFO, "Combining complete. " + count + " countries processed.");
//            }
//
//            stateCountry = null;
//            stateShape = null;
//            System.gc();
//
//            ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();  //create the output datastore
//
//            Map<String, Serializable> outputParams = new HashMap<String, Serializable>();
//            outputParams.put("url", outputFile.toURI().toURL());
//            outputParams.put("create spatial index", Boolean.TRUE);
//
//            ShapefileDataStore outputDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(outputParams);
//
//            outputDataStore.createSchema(WIKITYPE);
//
//            Transaction transaction = new DefaultTransaction("create");
//
//            String typeName = outputDataStore.getTypeNames()[0];
//            SimpleFeatureSource outputFeatureSource = outputDataStore.getFeatureSource(typeName);
//            SimpleFeatureType SHAPE_TYPE = outputFeatureSource.getSchema();
//
//            LOG.log(Level.INFO, "Writing to " + outputFile.getCanonicalPath());
//
//            if (outputFeatureSource instanceof SimpleFeatureStore) {
//                SimpleFeatureStore featureStore = (SimpleFeatureStore) outputFeatureSource;
//
//                SimpleFeatureCollection collection = new ListFeatureCollection(WIKITYPE, features);
//                featureStore.setTransaction(transaction);
//                try {
//                    featureStore.addFeatures(collection);
//                    transaction.commit();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    transaction.rollback();
//                } finally {
//                    transaction.close();
//                }
//                LOG.log(Level.INFO, "Writing success.");
//
//            } else {
//                LOG.log(Level.INFO, typeName + " does not support read/write access");
//
//            }
//
//
//        } catch (MalformedURLException e){
//            e.printStackTrace();
//
//        } catch (IOException e){
//            e.printStackTrace();
//
//        }
//
//
//
//    }


}
