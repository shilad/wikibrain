package org.wikibrain.spatial.matcher;

import com.typesafe.config.Config;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.CharSet;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.download.FileDownloader;
import org.wikibrain.utils.WpIOUtils;

import java.io.*;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class ShapeFileMatcher {
    private static final Logger LOG = Logger.getLogger(ShapeFileMatcher.class.getName());
    private final Env env;
    private final File spatialDir;


    public ShapeFileMatcher(Env env) {
        this.env = env;
        this.spatialDir = new File(env.getConfiguration().get().getString("spatial.dir"));
        spatialDir.mkdirs();
    }

    /**
     * Returns true if the dbf and shp files for the given file already have been created.
     * @param name
     * @return
     */
    private boolean filesExist(String name) {
        Config config = getConfig(name);
        for (String layer : config.getObject("layers").keySet()) {
            if (!getDbf(name, layer).isFile() || !getShp(name, layer).isFile() || !getShx(name, layer).isFile()) {
                return false;
            }
        }
        return true;
    }

    private File getDbf(String name, String layer) {
        return FileUtils.getFile(spatialDir, "raw", name + "." + layer + ".dbf");
    }

    private File getShp(String name, String layer) {
        return FileUtils.getFile(spatialDir, "raw", name + "." + layer + ".shp");
    }

    private File getShx(String name, String layer) {
        return FileUtils.getFile(spatialDir, "raw", name + "." + layer + ".shx");
    }

    public void download(String name) throws InterruptedException, IOException, ZipException {
        if (filesExist(name)) {
            return;
        }

        Config config = getConfig(name);

        // Download the file if necessary
        URL url = new URL(config.getString("url"));
        String tokens[] = url.toString().split("/");
        File dest = FileUtils.getFile(spatialDir, "raw", tokens[tokens.length-1]);
        if (!dest.isFile()) {
            FileDownloader downloader = new FileDownloader();
            downloader.download(url, dest);
        }

        // Unzip the file
        ZipFile zipFile = new ZipFile(dest.getCanonicalPath());
        File tmpDir = File.createTempFile("wikibrain", ".exploded");
        FileUtils.deleteQuietly(tmpDir);
        FileUtils.forceMkdir(tmpDir);
        LOG.log(Level.INFO, "Extracting to " + tmpDir);
        zipFile.extractAll(tmpDir.getAbsolutePath());
        FileUtils.forceDeleteOnExit(tmpDir);

        // Move the appropriate layers over with standardized names
        for (String layer : config.getObject("layers").keySet()) {
            File srcDbf = FileUtils.getFile(tmpDir, config.getString("layers." + layer + ".dbf"));
            File srcShp = FileUtils.getFile(tmpDir, config.getString("layers." + layer + ".shp"));
            File srcShx = FileUtils.getFile(tmpDir, config.getString("layers." + layer + ".shx"));

            if (!srcDbf.isFile()) {
                throw new IllegalArgumentException("expected dbf file " + srcDbf + " not found");
            }
            if (!srcShp.isFile()) {
                throw new IllegalArgumentException("expected dbf file " + srcShp + " not found");
            }
            if (!srcShx.isFile()) {
                throw new IllegalArgumentException("expected dbf file " + srcShx + " not found");
            }
            File destDbf = getDbf(name, layer);
            File destShp = getShp(name, layer);
            File destShx= getShx(name, layer);
            FileUtils.deleteQuietly(destDbf);
            FileUtils.deleteQuietly(destShp);
            FileUtils.deleteQuietly(destShx);
            destDbf.getParentFile().mkdirs();
            srcDbf.renameTo(destDbf);
            srcShp.renameTo(destShp);
            srcShx.renameTo(destShx);
        }
    }

    public void writeMatches(String name, String layer) throws IOException, ConfigurationException, DaoException {
        Config config = getConfig(name).getConfig("layers." + layer);
        GeoResolver resolver = new GeoResolver(env, config);

        File f = FileUtils.getFile(spatialDir, "matches", name + "." + layer + ".csv");
        f.getParentFile().mkdirs();

        CsvListWriter csv = new CsvListWriter(new FileWriter(f), CsvPreference.STANDARD_PREFERENCE);


        Map<String, URL> map = new HashMap<String, URL>();
        map.put("url", getShp(name, layer).toURI().toURL());
        DataStore inputDataStore = DataStoreFinder.getDataStore(map);
        SimpleFeatureSource inputFeatureSource = inputDataStore.getFeatureSource(inputDataStore.getTypeNames()[0]);
        SimpleFeatureCollection features = inputFeatureSource.getFeatures();


        try {
            List<String> fields = new ArrayList<String>();
            fields.add("updated");
            fields.add("status");
            fields.add("WB");
            fields.add("WB1");
            fields.add("WB2");
            fields.add("WB3");
            fields.add("WB_SCORE");

            SimpleFeatureType type = features.getSchema();
            for (int i = 0; i < type.getAttributeCount(); i++) {
                fields.add(type.getDescriptor(i).getLocalName());
            }
            csv.write(fields);

            String tstamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            SimpleFeatureIterator iter = features.features();
            while (iter.hasNext()) {
                SimpleFeature row = iter.next();
                Map<String, String> rowMap = new HashMap<String, String>();

                for (int i = 0; i < row.getAttributeCount(); i++) {
                    rowMap.put(fields.get(i), row.getAttribute(i).toString());
                }
                LinkedHashMap<LocalPage, Double> guesses = resolver.resolve(rowMap, 3);
                List<LocalPage> sorted = new ArrayList<LocalPage>(guesses.keySet());

                List<String> newRow = new ArrayList<String>();

                newRow.add(tstamp);
                newRow.add("U");
                newRow.add(sorted.size() > 0 ? sorted.get(0).getTitle().getTitleStringWithoutNamespace() : "");

                for (int i = 0; i < 3; i++) {
                    if (sorted.size() > i) {
                        newRow.add(sorted.get(i).getTitle().getTitleStringWithoutNamespace());
                    } else {
                        newRow.add("");
                    }
                }

                double score = 0.1;
                if (sorted.size() >= 2) {
                    score = guesses.get(sorted.get(0)) - guesses.get(sorted.get(1));
                }
                newRow.add(""+score);

                for (int i = 0; i < row.getAttributeCount(); i++) {
                    newRow.add(row.getAttribute(i).toString());
                }
                csv.write(newRow);
            }
            iter.close();
        } finally {
            csv.close();
        }
    }

    private Config getConfig(String name) {
        return env.getConfiguration().get().getConfig("spatial.datasets." + name);
    }

    public static void main(String args[]) throws Exception {
        String name = "naturalEarthCountries";
        Env env = EnvBuilder.envFromArgs(args);

        ShapeFileMatcher matcher = new ShapeFileMatcher(env);
        matcher.download(name);
        matcher.writeMatches(name, "country");
    }
}
