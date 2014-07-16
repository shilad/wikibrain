package org.wikibrain.spatial.util;

import org.apache.commons.io.FileUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A utility wrapper around GeoTool's shape file class
 * @author Shilad Sen
 */
public class WbShapeFile {
    public static final String [] EXTENSIONS = new String[] { ".shp", ".dbx", ".dbf" };

    // Ends with ".shp" extension
    private final File file;

    private DataStore dataStore;
    private String layer;

    public WbShapeFile(File file) throws IOException {
        ensureHasShpExtension(file);
        this.file = file;
        this.dataStore = fileToDataStore(file);
        this.layer = dataStore.getTypeNames()[0];
    }

    public WbShapeFile(File file, String layer) throws IOException {
        ensureHasShpExtension(file);
        this.file = file;
        this.dataStore = fileToDataStore(file);
        this.layer = layer;
    }

    public List<String> getFeatureNames() throws IOException {
        SimpleFeatureCollection features = getFeatureCollection();
        SimpleFeatureType type = features.getSchema();
        List<String> fields = new ArrayList<String>();
        for (int i = 0; i < type.getAttributeCount(); i++) {
            fields.add(type.getDescriptor(i).getLocalName());
        }
        return fields;
    }

    public SimpleFeatureIterator getFeatureIter() throws IOException {
        return getFeatureCollection().features();
    }

    public SimpleFeatureCollection getFeatureCollection() throws IOException {
        SimpleFeatureSource featureSource = dataStore.getFeatureSource(layer);
        return featureSource.getFeatures();
    }

    public static DataStore fileToDataStore(File file) throws IOException {
        Map<String, URL> map = new HashMap<String, URL>();
        try {
            map.put("url", file.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new IOException(e);
        }
        return  DataStoreFinder.getDataStore(map);
    }

    public WbShapeFile move(File dest) throws IOException {
        ensureHasShpExtension(dest);
        dest.getParentFile().mkdirs();
        for (String ext : EXTENSIONS) {
            File extDest = getAlternateExtension(dest, ext);
            FileUtils.deleteQuietly(extDest);
            getAlternateExtension(file, ext).renameTo(extDest);
        }
        return new WbShapeFile(dest);
    }

    public File getFile() {
        return file;
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    public String getLayer() {
        return layer;
    }

    public static boolean exists(File file) {
        ensureHasShpExtension(file);
        for (File ext : getExtensions(file)) {
            if (!ext.isFile()) {
                return false;
            }
        }
        return true;
    }

    public static List<File> getExtensions(File file) {
        ensureHasShpExtension(file);
        List<File> files = new ArrayList<File>();
        for (String ext : EXTENSIONS) {
            files.add(getAlternateExtension(file, ext));
        }
        return files;
    }

    public static File getAlternateExtension(File file, String ext) {
        ensureHasShpExtension(file);
        String prefix = file.toString().substring(0, file.toString().length() - ".shp".length());
        return new File(prefix + ext);
    }

    public static boolean hasShpExtension(File file) {
        return file.toString().toLowerCase().endsWith(".shp");
    }

    private static void ensureHasShpExtension(File file) {
        if (!hasShpExtension(file)) {
            throw new IllegalArgumentException("File " + file + " does not have .shp extension");
        }
    }
}
