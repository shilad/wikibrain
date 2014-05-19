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
        converter.downloadAndConvert(new SpatialDataFolder(new File("test_spatial_data_folder")));


    }
}
