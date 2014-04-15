package org.wikapidia.spatial.cookbook;

import org.wikapidia.spatial.loader.GADMConverter;

/**
 * Created by aaroniidx on 4/13/14.
 */
public class ShapeFileConverterTest {

    public static void main(String[] args){
        GADMConverter converter = new GADMConverter();
        converter.downloadGADMShapeFile("China");
    }
}
