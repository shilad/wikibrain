package org.wikibrain.spatial.loader;


import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.io.FileUtils;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.shapefile.dbf.DbaseFileWriter;
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
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.spatial.core.dao.postgis.PostGISDB;
import org.wikibrain.wikidata.WikidataDao;
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

    /**
     * Download GADM shape file
     *
     */
    public void downloadGADMShapeFile() {

        String fileName = "gadm_v2_shp.zip";
        String gadmURL = "http://biogeo.ucdavis.edu/data/gadm2/" + fileName;
        File gadmShapeFile = new File("tmp/" + fileName);
        try {
            System.out.println("Downloading shape file" +"...");
            FileUtils.copyURLToFile(new URL(gadmURL), gadmShapeFile, 5000, 5000); //connection and read timeout are both 5000ms
            System.out.println("Download complete.");
            System.out.println(gadmShapeFile.getAbsolutePath());
            ZipFile zipFile = new ZipFile(gadmShapeFile.getAbsolutePath());

            System.out.println("Extracting...");
            zipFile.extractAll(gadmShapeFile.getParent() + "/gadm_v2_shp/");
            System.out.println("Extraction complete.");
        } catch (MalformedURLException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        } catch (ZipException e){
            e.printStackTrace();
        }
    }


    /**
     *
     * @param fileName
     * Takes in the GADM shape file and convert it into the kind of shape file that we can read
     * For some reason it won't recognize the CRS ID
     */
    public void convertShpFile(String fileName) {
        File file = new File(fileName);
        Map map = new HashMap();
        HashMap<String, List<Geometry>> stateShape = new HashMap<String, List<Geometry>>();
        HashMap<String, String> stateCountry = new HashMap<String, String>();
        File outputFile = new File("gadm2.shp");

        ShapefileReader shpReader;
        GeometryFactory geometryFactory;
        SimpleFeatureBuilder featureBuilder;
        DataStore inputDataStore;
        List<SimpleFeature> features = new ArrayList<SimpleFeature>();

        try{
            map.put("url", file.toURI().toURL());
            inputDataStore = DataStoreFinder.getDataStore(map);
            SimpleFeatureSource inputFeatureSource = inputDataStore.getFeatureSource(inputDataStore.getTypeNames()[0]);
            SimpleFeatureCollection inputCollection = inputFeatureSource.getFeatures();
            SimpleFeatureIterator inputFeatures = inputCollection.features();
            while (inputFeatures.hasNext()) {
                SimpleFeature feature = inputFeatures.next();
                //feature.getType().getGeometryDescriptor().getType().getBinding();

                if (!stateShape.containsKey(feature.getAttribute(5))) {
                    stateShape.put((String) feature.getAttribute(5), new ArrayList<Geometry>());
                    stateCountry.put((String) feature.getAttribute(5), (String) feature.getAttribute(3)); //set up the state-country map
                }

                stateShape.get(feature.getAttribute(5)).add((Geometry)feature.getAttribute(0)); //and put all the polygons under a state into another map
            }



            final SimpleFeatureType WIKITYPE = DataUtilities.createType("wikiType",
                    "geom:MultiPolygon:srid=4326, TITLE1_EN:String, TITLE2_EN:String"  // Code 4326 not found
            );
            geometryFactory = JTSFactoryFinder.getGeometryFactory();
            featureBuilder = new SimpleFeatureBuilder(WIKITYPE);

            System.out.println("Processing polygons...");

            for (String state: stateCountry.keySet()){    //create the feature collection for the new shpfile
                Geometry newGeom = geometryFactory.buildGeometry(stateShape.get(state)).buffer(0);
                featureBuilder.add(newGeom);
                featureBuilder.add(state);
                featureBuilder.add(state + ", " + stateCountry.get(state));
                SimpleFeature feature = featureBuilder.buildFeature(null);
                features.add(feature);
            }

            System.out.println("Processing complete.");

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
                System.exit(0); // success!
            } else {
                System.out.println(typeName + " does not support read/write access");
                System.exit(1);
            }


        } catch (SchemaException e){
            e.printStackTrace();

        } catch (MalformedURLException e){
            e.printStackTrace();

        } catch (IOException e){
            e.printStackTrace();

        }



    }



    /**
     * Convert GADM shapefile into the format we can read
     * @param shpFile
     */

    @Deprecated
    private void convertShpFile(ShpFiles shpFile) {
        DbaseFileReader dbfReader;
        DbaseFileWriter dbfWriter;
        DbaseFileHeader dbfHeader;
        Object[] entry, newEntry = new Object[2];
        try {
            dbfReader = new DbaseFileReader(shpFile, false, Charset.forName("UTF-8"));
            dbfHeader = new DbaseFileHeader();
            dbfHeader.addColumn("TITLE1_EN",'c',254,0);
            dbfHeader.addColumn("TITLE2_EN",'c',254,0);
            File f = new File("gadm2.dbf");
            FileOutputStream out = new FileOutputStream(f);
            dbfWriter = new DbaseFileWriter(dbfHeader, out.getChannel(), Charset.forName("UTF-8"));
            int count = 0;
            HashMap<Integer, HashSet<Integer>> id = new HashMap<Integer, HashSet<Integer>>(); //key: entry[1] = ID_0 value: entry[4] = ID_1
            while (dbfReader.hasNext()) {
                entry = dbfReader.readEntry();
                if (!id.containsKey(entry[1])) id.put((Integer)entry[1], new HashSet<Integer>());
                if (!id.get(entry[1]).contains(entry[4])) { //check duplicate
                    count++;
                    newEntry[0] = (String) entry[5];
                    newEntry[1] = (String) entry[5] + ", " + (String) entry[3];
                    dbfWriter.write(newEntry);
                    id.get(entry[1]).add((Integer)entry[4]);
                }
                else continue;  //skip duplicate records
            }
            System.out.println("Total number of records: " + count);
            dbfWriter.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }





}
