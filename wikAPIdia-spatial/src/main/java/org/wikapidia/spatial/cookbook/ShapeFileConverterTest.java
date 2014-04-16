package org.wikapidia.spatial.cookbook;

import org.geotools.data.shapefile.files.ShpFiles;
import org.wikapidia.spatial.loader.GADMConverter;

import java.net.MalformedURLException;

/**
 * Created by aaroniidx on 4/13/14.
 */
public class ShapeFileConverterTest {

    public static void main(String[] args){
        GADMConverter converter = new GADMConverter();
        //converter.downloadGADMShapeFile();
        try {
            ShpFiles shpFile = new ShpFiles("tmp/gadm_v2_shp/gadm2.dbf");
            converter.convertShpFile(shpFile);
        } catch (MalformedURLException e){
            e.printStackTrace();
        }
    }
}
