package org.wikibrain.spatial.cookbook;

import org.geotools.data.shapefile.files.ShpFiles;
import org.wikibrain.spatial.loader.GADMConverter;

import java.net.MalformedURLException;

/**
 * Created by aaroniidx on 4/13/14.
 * Uses the less big China shape file as an example
 */
public class ShapeFileConverterTest {

    public static void main(String[] args){
        GADMConverter converter = new GADMConverter();
        //converter.downloadGADMShapeFile();

        converter.convertShpFile("tmp/gadm_v2_shp/gadm2.shp");

    }
}
