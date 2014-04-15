package org.wikapidia.spatial.loader;


import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.io.FileUtils;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapefileException;
import org.geotools.data.shapefile.shp.ShapefileReader;
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

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Created by aaroniidx on 4/13/14.
 */
public class GADMConverter {

    private Map<String, String> countryCode = new HashMap<String, String>();

    /**
     * Auto-generate a map from country name to its ISO3 code
     */
    private void buildCodeMap(){
        String[] iso2Code = Locale.getISOCountries();
        for (String iso: iso2Code){
            Locale l = new Locale("", iso);
            countryCode.put(l.getDisplayCountry(), l.getISO3Country());
        }
    }

    /**
     * Downloads the GADM shape file by country name
     * @param country
     */
    public void downloadGADMShapeFile(String country) {
        if (countryCode.isEmpty()) {
            buildCodeMap();
        }
        String fileName = countryCode.get(country) + "_adm.zip";
        String gadmURL = "http://biogeo.ucdavis.edu/data/gadm2/shp/" + fileName;
        File gadmShapeFile = new File(System.getProperty("user.home").replace("\\", "/") + "/Desktop/" + fileName);
        try {
            System.out.println("Downloading shape file for " + country +"...");
            FileUtils.copyURLToFile(new URL(gadmURL), gadmShapeFile, 5000, 5000); //connection and read timeout are both 5000ms
            System.out.println("Download complete.");
        } catch (MalformedURLException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
    }




}
