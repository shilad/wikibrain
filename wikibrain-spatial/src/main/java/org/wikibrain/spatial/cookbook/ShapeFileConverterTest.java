package org.wikibrain.spatial.cookbook;

import org.geotools.data.shapefile.files.ShpFiles;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.spatial.loader.GADMConverter;
import org.wikibrain.spatial.loader.SpatialDataFolder;
import org.apache.commons.codec.digest.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Created by aaroniidx on 4/13/14.
 *
 */
public class ShapeFileConverterTest {

    public static void main(String[] args) throws WikiBrainException, IOException {
        GADMConverter converter = new GADMConverter();
        converter.downloadAndConvert(new SpatialDataFolder(new File("test_spatial_data_folder")));




    }
}
