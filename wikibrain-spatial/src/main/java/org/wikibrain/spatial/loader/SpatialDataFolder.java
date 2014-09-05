package org.wikibrain.spatial.loader;

import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.wikibrain.conf.Configuration;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.spatial.WikiBrainShapeFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Convenience class to deal with spatial data folder structure. Structure is as follows:
 *
 * Each reference system has its own folder (will ignore any reference system that starts with '_'
 * Each layerGroup has its own subdirectory
 * Shapefiles that are placed in each folder will be loaded for the same layer
 *
 * For example: baseFolder/earth/country/naturalEarth.shp
 *
 * @author bjhecht and shilad
 */
public class SpatialDataFolder {
    private final File baseFolder;


    /**
     * Will create the folder if it does not exist. By default, installs an empty "earth" reference system if "earth" reference system does not exist
     * @param baseFolder
     */
    public SpatialDataFolder(File baseFolder){
        this.baseFolder = baseFolder;
        if (!baseFolder.exists()){
            baseFolder.mkdirs();
        }
    }

    /**
     * Returns true of spatial data folder has a given layer
     * @param layerGroup
     * @param refSysName
     * @return
     */
    public boolean hasLayerGroup(String refSysName, String layerGroup) {
        return !getFilesInLayerGroup(refSysName, layerGroup).isEmpty();
    }

    public File getRawFolder() {
        return FileUtils.getFile(baseFolder, "_raw");
    }

    public WikiBrainShapeFile getShapeFile(String refSys, String layerGroup, String name) {
        return new WikiBrainShapeFile(FileUtils.getFile(baseFolder, refSys, layerGroup, name + ".shp"));
    }


    public WikiBrainShapeFile getShapeFile(String refSys, String layerGroup, String name, String encoding) {
        return new WikiBrainShapeFile(FileUtils.getFile(baseFolder, refSys, layerGroup, name + ".shp"), encoding);
    }

    /**
     * Returns true if the spatial data folder has a reference system
     * @param refSysName
     * @return
     */
    public boolean hasReferenceSystem(String refSysName){
        File refFolder = getReferenceSystemFolder(refSysName);
        return isImportantFile(refFolder) && !getLayerGroups(refSysName).isEmpty();
    }

    /**
     * Returns the file for the given reference system
     * @return
     * @param refSysName
     */
    public File getReferenceSystemFolder(String refSysName) {
        return FileUtils.getFile(baseFolder, refSysName);
    }

    /**
     * Returns the directory for the given reference system and layer
     * @param refSys e.g. "earth"
     * @param layerGroup e.g. "country"
     * @return
     */
    public File getLayerGroupFolder(String refSys, String layerGroup) {
        return FileUtils.getFile(baseFolder, refSys, layerGroup);
    }


    /**
     * Deletes a reference system. Use with caution!
     * @param refSysName
     * @throws FileNotFoundException
     */
    public void deleteReferenceSystem(String refSysName) throws FileNotFoundException{
        FileUtils.deleteQuietly(getReferenceSystemFolder(refSysName));
    }

    /**
     * Gets reference system names
     */
    public Set<String> getReferenceSystems(){
        Set<String> rVal = Sets.newHashSet();
        for (File refFolder : baseFolder.listFiles()){
            String refSysName = refFolder.getName();
            if (hasReferenceSystem(refSysName)) {
                rVal.add(refSysName);
            }
        }
        return rVal;
    }

    private static boolean isImportantFile(File file) {
        return file.exists() && !file.isHidden() && !file.getName().startsWith("_");
    }

    public Set<String> getLayerGroups(String refSysName) {
        Set<String> layerGroups = new HashSet<String>();
        for (File file : getReferenceSystemFolder(refSysName).listFiles()) {
            String layerGroup = file.getName();
            if (isImportantFile(file) && !getFilesInLayerGroup(refSysName, layerGroup).isEmpty()) {
                layerGroups.add(layerGroup);
            }
        }
        return layerGroups;
    }


    /**
     * Deletes a layer. Use with caution.
     * @param refSysName Reference system of layer to delete
     * @param layerGroup Layer to delete
     * @throws WikiBrainException
     */
    public void deleteLayer(String refSysName, String layerGroup) throws WikiBrainException {
        FileUtils.deleteQuietly(FileUtils.getFile(baseFolder, refSysName, layerGroup));
    }

    public List<WikiBrainShapeFile> getFilesInLayerGroup(String refSysName, String layerName) {
        List<WikiBrainShapeFile> shapeFiles = new ArrayList<WikiBrainShapeFile>();
        for (File file : getLayerGroupFolder(refSysName, layerName).listFiles()) {
            if (isImportantFile(file) && file.toString().toLowerCase().endsWith(".shp")) {
                shapeFiles.add(new WikiBrainShapeFile(file));
            }
        }
        return shapeFiles;
    }

    /**
     * Hack: Better to ask the configurator for this!
     * @param conf
     * @return
     */
    public static SpatialDataFolder get(Configuration conf) {
        return new SpatialDataFolder(conf.getFile("spatial.dir"));
    }
}
