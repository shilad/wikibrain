package org.wikibrain.spatial.loader;

import net.lingala.zip4j.core.ZipFile;
import org.codehaus.plexus.util.FileUtils;
import org.geotools.data.shapefile.files.ShpFiles;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.download.FileDownloader;
import org.wikibrain.utils.WpIOUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Random;

/**
 * Created by bjhecht on 5/19/14.
 */
public class BasicShapefileDownloader {

    public static void saveShapefileToReferenceSystem(String url, String refSysName,
                                                      SpatialDataFolder sdFolder) throws WikiBrainException{
        try {

            // download file
            Random r = new Random(); // random zip file name
            File downloadedFile = new File(sdFolder.getRefSysFolder(refSysName).getAbsolutePath()
                    + "/" + r.nextInt() + ".zip");

            FileDownloader downloader = new FileDownloader();
            downloader.download(new URL(url), downloadedFile);

            // unzip
            ZipFile zipFile = new ZipFile(downloadedFile);
            zipFile.extractAll(sdFolder.getRefSysFolder(refSysName).getAbsolutePath());
            downloadedFile.delete();

        }catch(Exception e){
            throw new WikiBrainException(e);
        }

    }
}
