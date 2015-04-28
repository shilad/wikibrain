package org.wikibrain.spatial;

import org.apache.commons.io.FileUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.h2.util.StringUtils;
import org.opengis.feature.simple.SimpleFeatureType;
import org.supercsv.io.CsvMapReader;
import org.supercsv.prefs.CsvPreference;
import org.wikibrain.utils.WpIOUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility wrapper around GeoTool's shape file class.
 * Also contains many static helper methods.
 *
 * Several of the methods contain variants that take a layer and ones that do not.
 * The ones that do not take a layer default to the "first" layer in the shapefile.
 *
 * @author Shilad Sen
 */
public class WikiBrainShapeFile {
    private static final Logger LOG = LoggerFactory.getLogger(WikiBrainShapeFile.class);

    public static final String [] EXTENSIONS = new String[] { ".shp", ".shx", ".dbf" };

    public static final String WB_MAP_EXTENSINO = ".wbmapping.csv";

    // Ends with ".shp" extension
    private final File file;
    private final String encoding;

    private DataStore dataStore;

    /**
     * Creates a new shapefile wrapper associated with the given file.
     * @param file Must end with ".shp"
     */
    public WikiBrainShapeFile(File file) {
        this(file, "UTF-8");
    }

    /**
     * Creates a new shapefile wrapper associated with the given file.
     * @param file Must end with ".shp"
     */
    public WikiBrainShapeFile(File file, String encoding) {
        ensureHasShpExtension(file);
        this.file = file;
        this.encoding = encoding;
    }

    public synchronized void initDataStoreIfNecessary() throws IOException {
        if (this.dataStore == null) {
            this.dataStore = fileToDataStore(file, encoding);
        }
    }

    static class KeyAndScore implements Comparable<KeyAndScore> {
        String key;
        Double score;

        KeyAndScore(String key, Double score) {
            this.key = key;
            this.score = score;
        }

        @Override
        public int compareTo(KeyAndScore o) {
            int r = -1 * score.compareTo(o.score);
            if (r == 0) {
                r = key.compareTo(o.key);
            }
            return r;
        }

        @Override
        public String toString() {
            return "{" + "key='" + key + '\'' + ", score=" + score + '}';
        }
    }

    /**
     * Reads in a mapping from shapefile key to title for all entries with status != U
     * @return
     */
    public Map<String, String> readMapping() throws IOException {
        HashMap<String, String> mapping = new HashMap<String, String>();
        if (!hasMappingFile()) {
            throw new IOException("No mapping file found: " + getMappingFile());
        }
        CsvMapReader reader = new CsvMapReader(
                WpIOUtils.openBufferedReader(getMappingFile()),
                CsvPreference.STANDARD_PREFERENCE
        );

        // Read in the data and scores
        Map<String, List<KeyAndScore>> scores = new HashMap<String, List<KeyAndScore>>();
        String [] header = reader.getHeader(true);
        while (true) {
            Map<String, String> row = reader.read(header);
            if (row == null) {
                break;
            }
            if (!row.get("WB_STATUS").equalsIgnoreCase("U")) {
                String key = row.get("WB_KEY");
                String title = row.get("WB_TITLE");
                double score = Double.valueOf(row.get("WB_SCORE"));
                if (StringUtils.isNullOrEmpty(title)) {
                    continue;
                }
                if (!scores.containsKey(title)) {
                    scores.put(title, new ArrayList<KeyAndScore>());
                }
                scores.get(title).add(new KeyAndScore(key, score));
            }
        }

        for (String title : scores.keySet()) {
            List<KeyAndScore> titleScores = scores.get(title);
            Collections.sort(titleScores);
            mapping.put(titleScores.get(0).key, title);
            if (titleScores.size() > 1) {
                LOG.warn("duplicate keys for title " + title + ": " + titleScores);
            }
        }

        return mapping;
    }

    /**
     * Returns all feature names (i.e. column ids) for the specified layer.
     * @param layer
     * @return
     * @throws IOException
     */
    public List<String> getFeatureNames(String layer) throws IOException {
        initDataStoreIfNecessary();
        SimpleFeatureCollection features = getFeatureCollection(layer);
        SimpleFeatureType type = features.getSchema();
        List<String> fields = new ArrayList<String>();
        for (int i = 0; i < type.getAttributeCount(); i++) {
            fields.add(type.getDescriptor(i).getLocalName());
        }
        return fields;
    }

    /**
     * Returns all feature names (i.e. column ids) for the default layer.
     * @return
     * @throws IOException
     */
    public List<String> getFeatureNames() throws IOException {
        return getFeatureNames(getDefaultLayer());
    }

    /**
     * Returns an iterator over rows for the default layer.
     * @return
     * @throws IOException
     */
    public SimpleFeatureIterator getFeatureIter() throws IOException {
        return getFeatureCollection().features();
    }

    /**
     * Returns an iterator over rows for the specified layer.
     * @return
     * @throws IOException
     */
    public SimpleFeatureIterator getFeatureIter(String layer) throws IOException {
        return getFeatureCollection(layer).features();
    }

    public SimpleFeatureCollection getFeatureCollection(String layer) throws IOException {
        initDataStoreIfNecessary();
        SimpleFeatureSource featureSource = dataStore.getFeatureSource(layer);
        return featureSource.getFeatures();
    }

    public SimpleFeatureCollection getFeatureCollection() throws IOException {
        return getFeatureCollection(getDefaultLayer());
    }

    /**
     * Returns the name of the default layer
     * @return
     * @throws IOException
     */
    public String getDefaultLayer() throws IOException {
        initDataStoreIfNecessary();
        return dataStore.getTypeNames()[0];
    }


    public WikiBrainShapeFile move(File dest) throws IOException {
        ensureHasShpExtension(dest);
        dest.getParentFile().mkdirs();
        for (String ext : EXTENSIONS) {
            File extDest = getAlternateExtension(dest, ext);
            FileUtils.deleteQuietly(extDest);
            getAlternateExtension(file, ext).renameTo(extDest);
        }
        return new WikiBrainShapeFile(dest, encoding);
    }

    public File getMappingFile() {
        return getAlternateExtension(file, WB_MAP_EXTENSINO);
    }

    public boolean hasMappingFile() {
        return getMappingFile().isFile();
    }

    public File getFile() {
        return file;
    }

    public List<File> getComponentFiles() {
        List<File> files = new ArrayList<File>();
        for (String ext : EXTENSIONS) {
            files.add(getAlternateExtension(file, ext));
        }
        if (hasMappingFile()) {
            files.add(getMappingFile());
        }
        return files;
    }

    public boolean hasComponentFiles() {
        for (File f : getComponentFiles()) {
            if (!f.isFile()) return false;
        }
        return true;
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    public static DataStore fileToDataStore(File file) throws IOException {
        return fileToDataStore(file, "utf-8");
    }

    public static DataStore fileToDataStore(File file, String encoding) throws IOException {
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            map.put("url", file.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new IOException(e);
        }
        map.put(ShapefileDataStoreFactory.DBFCHARSET.getName(), encoding);
        return  DataStoreFinder.getDataStore(map);
    }

    public static boolean exists(File file) {
        ensureHasShpExtension(file);
        return new WikiBrainShapeFile(file).hasComponentFiles();
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
