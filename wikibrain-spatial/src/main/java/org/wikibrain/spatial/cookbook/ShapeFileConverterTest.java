//package org.wikibrain.spatial.cookbook;
//
//import org.geotools.data.shapefile.files.ShpFiles;
//import org.wikibrain.spatial.loader.GADMConverter;
//
//import java.net.MalformedURLException;
//
///**
// * Created by aaroniidx on 4/13/14.
// * Uses the less big China GADM shape file as an example
// * For the entire world you need to change the indexes in lines 138-143 of GADMConverter: 5 -> 6, 3 -> 4
// *
// */
//public class ShapeFileConverterTest {
//
//    public static void main(String[] args){
//        GADMConverter converter = new GADMConverter();
//        //converter.downloadGADMShapeFile();  //Uncomment this line if you don't have the shape file for the world
//
//        //converter.convertShpFile("tmp/CHN_adm/CHN_adm2.shp");
//        converter.convertShpFile("tmp/gadm_v2_shp/gadm2.shp");  //Uncomment this line if you want to do the entire world.
//
//    }
//}
