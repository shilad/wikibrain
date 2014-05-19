package org.wikibrain.spatial.loader;

import org.geotools.data.shapefile.files.ShpFileType;
import org.geotools.data.shapefile.files.ShpFiles;
import org.wikibrain.core.WikiBrainException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;

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
            String path = refSysFolder + "/" + layerName + ".shp";
            ShpFiles shpFiles = new ShpFiles(path);

            return new File(shpFiles.get(ShpFileType.SHP));
        }catch(MalformedURLException e){
            throw new WikiBrainException(e);
        }


    }

    /**
     * Gets the folder for a given reference system
     * @param refSysName
     * @return
     * @throws FileNotFoundException
     */
    public File getRefSysFolder(String refSysName){

        String path = baseFolderPath + "/" + refSysName;
        File rVal = new File(path);
        return rVal;

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




}
