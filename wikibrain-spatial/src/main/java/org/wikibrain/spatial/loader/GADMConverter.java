package org.wikibrain.spatial.loader;


import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.codec.digest.*;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.shapefile.dbf.DbaseFileWriter;
import org.geotools.data.shapefile.files.ShpFileType;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapefileException;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.*;
import org.geotools.data.DataUtilities;
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
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.*;
import org.opengis.geometry.coordinate.*;
import org.geotools.geometry.jts.*;
import org.hibernate.annotations.SourceType;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.phrases.PhraseAnalyzer;
import org.wikibrain.spatial.core.constants.RefSys;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.spatial.core.dao.postgis.PostGISDB;
import org.wikibrain.utils.WpIOUtils;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.download.*;
import net.lingala.zip4j.*;

import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
* Created by aaroniidx on 4/13/14.
*/
public class GADMConverter {


    public static final Logger LOG = Logger.getLogger(GADMConverter.class.getName());

    public static void downloadAndConvert(SpatialDataFolder folder) throws WikiBrainException {

        try {


            WpIOUtils ioUtils = new WpIOUtils();
            String tmpFolderName = "_gadmdownload";

            File tmpFolder = WpIOUtils.createTempDirectory(tmpFolderName, true);


            // Download to a temp folder (Note that WikiBrain will ignore all reference systems that begin with "_"
            //folder.createNewReferenceSystemIfNotExists(tmpFolder.getCanonicalPath());
            File rawFile = downloadGADMShapeFile(tmpFolder.getCanonicalPath());

            //copy level 2 shapefile to earth reference system
            LOG.log(Level.INFO, "Copying level 2 shapefiles to " + folder.getRefSysFolder("earth").getCanonicalPath());
            FileUtils.copyDirectory(new File(tmpFolder.getCanonicalPath()), folder.getRefSysFolder("earth"));

            // convert file and save as layer in earth reference system
            LOG.log(Level.INFO, "Start mapping level 1 shapefiles.");
            convertShpFile(rawFile, folder, 1);
            LOG.log(Level.INFO, "Start mapping level 0 shapefiles.");
            convertShpFile(rawFile, folder, 0);


        } catch(IOException e){
            throw new WikiBrainException(e);
        } catch (ZipException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * Download GADM shape file
     * @param tmpFolder
     * @return
     *
     */
    public static File downloadGADMShapeFile(String tmpFolder) throws IOException, ZipException, InterruptedException {

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


    /**
     *
     * @param rawFile
     * @param outputFolder
     * @return
     * //TODO: reduce memory usage
     * Converts raw GADM shapefile into WikiBrain readable type
     * Recommended JVM max heapsize = 4G
     *
     */
    public static void convertShpFile(File rawFile, SpatialDataFolder outputFolder, int level) throws IOException {
        if ((level != 0) && (level != 1)) throw new IllegalArgumentException("Level must be 0 or 1");

        File outputFile;
        Map map = new HashMap();
        HashMap<String, List<Geometry>> stateShape = new HashMap<String, List<Geometry>>();
        HashMap<String, String> stateCountry = new HashMap<String, String>();
        if (level == 1)
            outputFile = new File(outputFolder.getRefSysFolder("earth").getCanonicalPath() + "/" + "gadm1.shp");
        else
            outputFile = new File(outputFolder.getRefSysFolder("earth").getCanonicalPath() + "/" + "gadm0.shp");

        ShapefileReader shpReader;
        GeometryFactory geometryFactory;
        SimpleFeatureTypeBuilder typeBuilder;
        SimpleFeatureBuilder featureBuilder;
        DataStore inputDataStore;
        List<SimpleFeature> features = new ArrayList<SimpleFeature>();

        try {
            map.put("url", rawFile.toURI().toURL());
            inputDataStore = DataStoreFinder.getDataStore(map);
            SimpleFeatureSource inputFeatureSource = inputDataStore.getFeatureSource(inputDataStore.getTypeNames()[0]);
            SimpleFeatureCollection inputCollection = inputFeatureSource.getFeatures();
            SimpleFeatureIterator inputFeatures = inputCollection.features();


            //level 1 mapping
            if (level == 1) {
                LOG.log(Level.INFO, "Mapping polygons to the corresponding administrative districts.");
                while (inputFeatures.hasNext()) {
                    SimpleFeature feature = inputFeatures.next();
                    String country = ((String) feature.getAttribute(4)).intern();
                    String state = ((String) feature.getAttribute(6)).intern();

                    if (!stateShape.containsKey(state)) {
                        stateShape.put(state, new ArrayList<Geometry>());
                        stateCountry.put(state, country); //set up the state-country map
                    }

                    stateShape.get(state).add((Geometry) feature.getAttribute(0)); //and put all the polygons under a state into another map
                }
            } else {
                //level 0 mapping
                LOG.log(Level.INFO, "Mapping polygons to the corresponding countries.");
                while (inputFeatures.hasNext()) {
                    SimpleFeature feature = inputFeatures.next();
                    String country = ((String) feature.getAttribute(4)).intern();

                    if (!stateShape.containsKey(country))
                        stateShape.put(country, new ArrayList<Geometry>());

                    stateShape.get(country).add((Geometry) feature.getAttribute(0));
                }
            }
            inputFeatures.close();
            inputDataStore.dispose();

            LOG.log(Level.INFO, "Mapping complete.");

            typeBuilder = new SimpleFeatureTypeBuilder();  //build the output feature type
            typeBuilder.setName("WIKITYPE");
            typeBuilder.setCRS(DefaultGeographicCRS.WGS84);
            typeBuilder.add("the_geom", MultiPolygon.class);
            typeBuilder.add("TITLE1_EN", String.class);
            if (level == 1) typeBuilder.add("TITLE2_EN", String.class);
            typeBuilder.setDefaultGeometry("the_geom");


            final SimpleFeatureType WIKITYPE = typeBuilder.buildFeatureType();

            geometryFactory = JTSFactoryFinder.getGeometryFactory();
            featureBuilder = new SimpleFeatureBuilder(WIKITYPE);

            LOG.log(Level.INFO, "Combining polygons that belongs to the same administrative districts.");

            int count = 0;
            if (level == 1) {
                int total = stateShape.keySet().size(); //TODO: We need a lot more in the way of progress statements here
                for (String state : stateCountry.keySet()) {    //create the feature collection for the new shpfile
                    count++;
                    try {
                        Geometry newGeom = geometryFactory.buildGeometry(stateShape.get(state)).union();
                        featureBuilder.add(newGeom);
                        featureBuilder.add(state);
                        featureBuilder.add(state + ", " + stateCountry.get(state));
                        SimpleFeature feature = featureBuilder.buildFeature(null);
                        if (count % 10 == 0)
                            LOG.log(Level.INFO, count + "/" + total + " level 1 administrative districts processed.");
                        features.add(feature);
                        stateShape.remove(state);
                        System.gc();
                    }
                    catch (Exception e) {
                        Geometry newGeom = geometryFactory.buildGeometry(stateShape.get(state)).buffer(0);
                        featureBuilder.add(newGeom);
                        featureBuilder.add(state);
                        featureBuilder.add(state + ", " + stateCountry.get(state));
                        SimpleFeature feature = featureBuilder.buildFeature(null);
                        if (count % 10 == 0)
                            LOG.log(Level.INFO, count + "/" + total + " level 1 administrative districts processed.");
                        features.add(feature);
                        LOG.log(Level.INFO, "Exception occured at " + state + ": " + e.getMessage() + ". Attempting different combining methods.");
                        stateShape.remove(state);
                        System.gc();
                        continue;
                    }
                }
            } else {
                int total = stateShape.keySet().size();
                HashSet<String> countryList = new HashSet<String>();
                countryList.addAll(stateShape.keySet());
                for (String country: countryList) {
                    count++;
                    try {
                        LOG.log(Level.INFO, "Combining " + stateShape.get(country).size() + " polygons for " + country + " (" + count + "/" + total + ")");
                        Geometry newGeom = geometryFactory.buildGeometry(stateShape.get(country)).union();
                        featureBuilder.add(newGeom);
                        featureBuilder.add(country);
                        SimpleFeature feature = featureBuilder.buildFeature(null);
                        features.add(feature);
                        stateShape.remove(country);
                        System.gc();
                    }
                    catch (Exception e){
                        LOG.log(Level.INFO, "Exception occured at " + country + " : " + e.getMessage() + ". Attempting different combining methods.");
                        Geometry newGeom = geometryFactory.buildGeometry(stateShape.get(country)).buffer(0);
                        featureBuilder.add(newGeom);
                        featureBuilder.add(country);
                        SimpleFeature feature = featureBuilder.buildFeature(null);
                        features.add(feature);
                        stateShape.remove(country);
                        System.gc();
                        continue;
                    }
                }
            }

            if (level == 1) {
                LOG.log(Level.INFO, "Combining complete. " + count + " administrative districts processed.");
            } else {
                LOG.log(Level.INFO, "Combining complete. " + count + " countries processed.");
            }

            stateCountry = null;
            stateShape = null;
            System.gc();

            ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();  //create the output datastore

            Map<String, Serializable> outputParams = new HashMap<String, Serializable>();
            outputParams.put("url", outputFile.toURI().toURL());
            outputParams.put("create spatial index", Boolean.TRUE);

            ShapefileDataStore outputDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(outputParams);

            outputDataStore.createSchema(WIKITYPE);

            Transaction transaction = new DefaultTransaction("create");

            String typeName = outputDataStore.getTypeNames()[0];
            SimpleFeatureSource outputFeatureSource = outputDataStore.getFeatureSource(typeName);
            SimpleFeatureType SHAPE_TYPE = outputFeatureSource.getSchema();

            LOG.log(Level.INFO, "Writing to " + outputFile.getCanonicalPath());

            if (outputFeatureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) outputFeatureSource;

                SimpleFeatureCollection collection = new ListFeatureCollection(WIKITYPE, features);
                featureStore.setTransaction(transaction);
                try {
                    featureStore.addFeatures(collection);
                    transaction.commit();
                } catch (Exception e) {
                    e.printStackTrace();
                    transaction.rollback();
                } finally {
                    transaction.close();
                }
                LOG.log(Level.INFO, "Writing success.");

            } else {
                LOG.log(Level.INFO, typeName + " does not support read/write access");

            }


        } catch (MalformedURLException e){
            e.printStackTrace();

        } catch (IOException e){
            e.printStackTrace();

        }



    }


}
