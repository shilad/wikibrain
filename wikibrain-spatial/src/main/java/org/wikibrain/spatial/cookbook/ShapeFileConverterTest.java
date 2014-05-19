package org.wikibrain.spatial.cookbook;

import org.geotools.data.shapefile.files.ShpFiles;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.spatial.loader.GADMConverter;
import org.wikibrain.spatial.loader.SpatialDataFolder;
import org.apache.commons.codec.digest.*;

import java.io.File;
import java.io.FileInputStream;
import java.net.MalformedURLException;

/**
 * Created by aaroniidx on 4/13/14.
 * Uses the less big China GADM shape file as an example
 * For the entire world you need to change the indexes in lines 138-143 of GADMConverter: 5 -> 6, 3 -> 4
 *
 */
public class ShapeFileConverterTest {

    public static void main(String[] args) throws WikiBrainException {
        GADMConverter converter = new GADMConverter();
        converter.downloadAndConvert(new SpatialDataFolder(new File("gadmtest")));
        //converter.downloadGADMShapeFile();  //Uncomment this line if you don't have the shape file for the world

        //converter.convertShpFile("tmp/CHN_adm/CHN_adm2.shp");
        //converter.convertShpFile("tmp/gadm_v2_shp/gadm2.shp");  //Uncomment this line if you want to do the entire world.

    }
}
