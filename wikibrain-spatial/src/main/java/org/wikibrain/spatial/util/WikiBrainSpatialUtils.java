package org.wikibrain.spatial.util;

import org.geotools.data.shapefile.files.ShpFileType;
import org.geotools.data.shapefile.files.ShpFiles;
import org.wikibrain.core.WikiBrainException;

import java.io.File;

/**
 * Created by bjhecht on 5/21/14.
 */
public class WikiBrainSpatialUtils {

    /**
     * Returns the base name of a shapefile (e.g. "gadm0.shp" -> "gadm")
     * @param mainShapeFile
     * @param absolutePath If true, will return the full path to the file, minus the extension. If false, just returns the name.
     * @return
     */
    public static String getBaseNameOfShapefile(File mainShapeFile, boolean absolutePath){

        String baseWithExt = (absolutePath) ? mainShapeFile.getAbsolutePath() : mainShapeFile.getName();
        int dotIndex = baseWithExt.lastIndexOf(".");
        String base = baseWithExt.substring(0, dotIndex);
        return base;

    }

    public static void deleteShapefile(File mainShapefile){

        String baseName = WikiBrainSpatialUtils.getBaseNameOfShapefile(mainShapefile,false);

        File parentFolder = mainShapefile.getParentFile();
        for (File f : parentFolder.listFiles()) {
            if (f.getName().startsWith(baseName + ".")) {
                f.delete();
            }
        }

    }

}
