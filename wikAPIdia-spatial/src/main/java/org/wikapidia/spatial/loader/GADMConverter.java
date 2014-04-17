package org.wikapidia.spatial.loader;


import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Geometry;
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
import org.hibernate.annotations.SourceType;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.phrases.PhraseAnalyzer;
import org.wikapidia.spatial.core.dao.SpatialDataDao;
import org.wikapidia.spatial.core.dao.postgis.PostGISDB;
import org.wikapidia.wikidata.WikidataDao;
import net.lingala.zip4j.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Created by aaroniidx on 4/13/14.
 */
public class GADMConverter {

    /**
     * Download GADM shape file
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

    //TODO: fix the index out of bound bug

    /**
     * Convert GADM shapefile into the format we can read
     * @param shpFile
     */
    public void convertShpFile(ShpFiles shpFile) {
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
