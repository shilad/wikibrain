//package org.wikibrain.spatial.util;
//
//import com.google.common.io.Files;
//import net.lingala.zip4j.core.ZipFile;
//import net.lingala.zip4j.exception.ZipException;
//import org.apache.commons.io.FileUtils;
//import org.geotools.data.shapefile.files.ShpFileType;
//import org.geotools.data.shapefile.files.ShpFiles;
//import org.wikibrain.core.WikiBrainException;
//import org.wikibrain.download.FileDownloader;
//import org.wikibrain.spatial.loader.SpatialDataFolder;
//import org.wikibrain.utils.WpIOUtils;
//
//import java.io.File;
//import java.io.IOException;
//import java.net.URL;
//import java.util.Random;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
///**
// * Created by bjhecht on 5/21/14.
// */
//public class WikiBrainSpatialUtils {
//
//    private static final Logger LOG = Logger.getLogger(WikiBrainSpatialUtils.class.getName());
//
//
//    /**
//     * Returns the base name of a shapefile (e.g. "gadm0.shp" -> "gadm")
//     * @param mainShapeFile
//     * @param absolutePath If true, will return the full path to the file, minus the extension. If false, just returns the name.
//     * @return
//     */
//    public static String getBaseNameOfShapefile(File mainShapeFile, boolean absolutePath){
//
//        String baseWithExt = (absolutePath) ? mainShapeFile.getAbsolutePath() : mainShapeFile.getName();
//        int dotIndex = baseWithExt.lastIndexOf(".");
//        String base = baseWithExt.substring(0, dotIndex);
//        return base;
//
//    }
//
//    public static void deleteShapefile(File mainShapefile){
//
//        String baseName = WikiBrainSpatialUtils.getBaseNameOfShapefile(mainShapefile,false);
//
//        File parentFolder = mainShapefile.getParentFile();
//        for (File f : parentFolder.listFiles()) {
//            if (f.getName().startsWith(baseName + ".")) {
//                f.delete();
//            }
//        }
//
//    }
//
//    /**
//     * Moves all files associated with a shapefile to the destination folder. Only supports types supported by GeoTools' ShpFiles class.
//     * @param mainShapefile
//     * @param destFolder
//     * @throws WikiBrainException
//     */
//    public static void moveShapefile(File mainShapefile, File destFolder) throws WikiBrainException{
//
//
//        try {
//
//            ShpFiles shpFiles = new ShpFiles(mainShapefile);
//
//            for (ShpFileType type : ShpFileType.values()) {
//
//                String fileName = shpFiles.get(type);
//                File f = new File(fileName);
//
//                Files.move(f, new File(destFolder.getAbsolutePath() + System.getProperty("file.separator") + f.getName()));
//
//
//            }
//
//        }catch(Exception e){
//            throw new WikiBrainException(e);
//        }
//
//
//    }
//
//
//    /**
//     * Downloads, unzips, and moves a shapefile to the spatial data folder.
//     * @param url
//     * @param refSysName
//     * @param sdFolder
//     * @throws WikiBrainException
//     */
//    public static void downloadZippedShapefileToReferenceSystem(String url, String refSysName, SpatialDataFolder sdFolder) throws WikiBrainException{
//
//
//        try {
//
//
//            File folder = sdFolder.getRefSysFolder(refSysName, true);
//
//            Random r = new Random();
//            File zipFileFile = new File (folder.getAbsolutePath() + System.getProperty("file.separator") + r.nextInt(Integer.MAX_VALUE));
//
//            FileDownloader downloader = new FileDownloader();
//            downloader.download(new URL(url), zipFileFile);
//            ZipFile zipFile = new ZipFile(zipFileFile);
//            LOG.log(Level.INFO, "Extracting to " + folder.getName());
//            zipFile.extractAll(folder.getAbsolutePath());
//            zipFileFile.delete();
//            LOG.log(Level.INFO, "Extraction complete.");
//
//
//        }catch(IOException e){
//            throw new WikiBrainException(e);
//        } catch (InterruptedException e) {
//            throw new WikiBrainException(e);
//        } catch (ZipException e) {
//            throw new WikiBrainException(e);
//        }
//    }
//
//}
