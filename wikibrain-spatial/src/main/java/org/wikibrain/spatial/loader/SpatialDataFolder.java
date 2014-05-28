package org.wikibrain.spatial.loader;

import com.google.common.collect.Sets;
import org.codehaus.plexus.util.FileUtils;
import org.geotools.data.shapefile.files.ShpFileType;
import org.geotools.data.shapefile.files.ShpFiles;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.spatial.core.constants.RefSys;
import org.wikibrain.spatial.util.WikiBrainSpatialUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

/**
 * Convenience class to deal with spatial data folder structure. Structure is as follows:
 * * Each reference system has its own folder (will ignore any reference system that starts with '_'
 * * Shapefiles that are placed in each folder will be loaded in as separate layers
 *
 * Created by bjhecht on 5/18/14
 */
public class SpatialDataFolder {

    private final File baseFolder;
    private final String baseFolderPath;


    /**
     * Will create the folder if it does not exist. By default, installs an empty "earth" reference system if "earth" reference system does not exist
     * @param baseFolder
     */
    public SpatialDataFolder(File baseFolder){

        this.baseFolder = baseFolder;
        this.baseFolderPath = baseFolder.getAbsolutePath();

        if (!baseFolder.exists()){
            baseFolder.mkdir();
        }

        this.createNewReferenceSystemIfNotExists("earth");

    }

    /**
     * Returns true of spatial data folder has a given layer
     * @param layerName
     * @param refSysName
     * @return
     */
    public boolean hasLayer(String layerName, String refSysName) throws WikiBrainException{

        File shpFile = getMainShapefile(layerName, refSysName);
        return shpFile.exists();

    }

    /**
     * Returns true if the spatial data folder has a reference system
     * @param refSysName
     * @return
     */
    public boolean hasReferenceSystem(String refSysName){
        File f = getRefSysFolder(refSysName);
        return f.exists();
    }

    /**
     * Gets the main shapefile (the .shp files) of a given layer in a given reference system
     * @param layerName
     * @param refSysName
     * @return
     * @throws FileNotFoundException
     * @throws MalformedURLException
     */
    public File getMainShapefile(final String layerName, String refSysName) throws WikiBrainException {

        try {
            File refSysFolder = getRefSysFolder(refSysName);
            String path = refSysFolder + System.getProperty("file.separator") + layerName + ".shp";
            ShpFiles shpFiles = new ShpFiles(path);

            URL url = new URL(shpFiles.get(ShpFileType.SHP));

            return new File(url.getFile());

        } catch(MalformedURLException e){
            throw new WikiBrainException(e);
        }


    }

    /**
     * Gets the folder for a given reference system
     * @param refSysName
     * @return
     * @throws FileNotFoundException
     */
    public File getRefSysFolder(String refSysName, boolean createIfNotExists){

        if (createIfNotExists){
            createNewReferenceSystemIfNotExists(refSysName);
        }

        String path = baseFolderPath + "/" + refSysName;
        File rVal = new File(path);
        return rVal;

    }

    public File getRefSysFolder(String refSysName){
        return getRefSysFolder(refSysName, false);
    }

    /**
     * Creates a new reference system if it doesn't already exist
     * @param refSysName
     */
    public void createNewReferenceSystemIfNotExists(String refSysName){

        if (!hasReferenceSystem(refSysName)){
            String path = baseFolderPath + "/" + refSysName;
            File folder = new File(path);
            folder.mkdir();
        }

    }

    /**
     * Deletes a reference system. Use with caution!
     * @param refSysName
     * @throws FileNotFoundException
     */
    public void deleteReferenceSystem(String refSysName) throws FileNotFoundException{


        File folder = getRefSysFolder(refSysName);
        for (File f : folder.listFiles()){
            f.delete();
        }
        folder.delete();

    }

    /**
     * Gets reference system names
     */
    public Set<String> getReferenceSystemNames(){

        Set<String> rVal = Sets.newHashSet();
        for (File curFolder : baseFolder.listFiles()){
            if (!curFolder.isHidden() && !curFolder.getName().startsWith("_")) {
                rVal.add(curFolder.getName());
            }
        }
        return rVal;
    }


    /**
     * Deletes a layer. Use with caution.
     * @param layerName Layer to delete
     * @param refSysName Reference system of layer to delete
     * @throws WikiBrainException
     */
    public void deleteLayer(String layerName, String refSysName) throws WikiBrainException {
        File mainShapefile = getMainShapefile(layerName, refSysName);
        WikiBrainSpatialUtils.deleteShapefile(mainShapefile);

    }

    /**
     * Deletes a specific file in a reference system folder. Good for removing spare files.
     * @param fileName
     * @param refSysName
     * @throws WikiBrainException
     */
    public void deleteSpecificFile(String fileName, String refSysName) throws WikiBrainException{

        File refSysFolder = this.getRefSysFolder(refSysName);
        String path = refSysFolder + "/" + fileName;
        File toDelete = new File(path);
        if (!toDelete.exists()){
            throw new WikiBrainException("No file at path: " + path);
        }else{
            toDelete.delete();
        }

    }

    /**
     * If the reference system does not exist, it will make it
     * @param mainShapefile
     * @param refSysName
     * @throws WikiBrainException
     */
    public void moveShapefileToReferenceSystemFolder(File mainShapefile, String refSysName) throws WikiBrainException {

        createNewReferenceSystemIfNotExists(refSysName);
        WikiBrainSpatialUtils.moveShapefile(mainShapefile, getRefSysFolder(refSysName));

    }




}
