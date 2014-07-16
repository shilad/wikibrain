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
import org.wikibrain.spatial.util.WbShapeFile;
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

    private WbShapeFile getShapeFile(String name, String layer) throws IOException {
        return new WbShapeFile(getPath(name, layer));
    }

    private File getPath(String name, String layer) {
        return FileUtils.getFile(spatialDir, "raw", name + "." + layer + ".shp");
    }

    private boolean hasAllShapefiles(String name) {
        Config config = getConfig(name);
        for (String layer : config.getObject("layers").keySet()) {
            if (WbShapeFile.exists(getPath(name, layer))) {
                return false;
            }
        }
        return true;
    }

    public void download(String name) throws InterruptedException, IOException, ZipException {
        if (hasAllShapefiles(name)) {
            return;
        }
        Config config = getConfig(name);

        // Download the file if necessary
        URL url = new URL(config.getString("url"));
        String tokens[] = url.toString().split("/");
        File zipDest = FileUtils.getFile(spatialDir, "raw", tokens[tokens.length-1]);
        if (!zipDest.isFile()) {
            FileDownloader downloader = new FileDownloader();
            downloader.download(url, zipDest);
        }

        // Unzip the file
        ZipFile zipFile = new ZipFile(zipDest.getCanonicalPath());
        File tmpDir = File.createTempFile("wikibrain", ".exploded");
        FileUtils.deleteQuietly(tmpDir);
        FileUtils.forceMkdir(tmpDir);
        LOG.log(Level.INFO, "Extracting to " + tmpDir);
        zipFile.extractAll(tmpDir.getAbsolutePath());
        FileUtils.forceDeleteOnExit(tmpDir);

        // Move the appropriate layers over with standardized names
        for (String layer : config.getObject("layers").keySet()) {
            File srcDbf = FileUtils.getFile(tmpDir, config.getString("layers." + layer + ".dbf"));
            for (File file : WbShapeFile.getExtensions(srcDbf)) {
                if (!file.isFile()) {
                    throw new IllegalArgumentException("expected file " + file.getAbsolutePath() + " not found");
                }
            }
            WbShapeFile src = new WbShapeFile(srcDbf);
            WbShapeFile dest = src.move(getPath(name, layer));
            if (!WbShapeFile.exists(dest.getFile())) {
                throw new IllegalArgumentException();
            }
        }
    }

    public void writeMatches(String name, String layer) throws IOException, ConfigurationException, DaoException {
        Config config = getConfig(name).getConfig("layers." + layer);
        GeoResolver resolver = new GeoResolver(env, config);

        File f = FileUtils.getFile(spatialDir, "matches", name + "." + layer + ".csv");
        f.getParentFile().mkdirs();

        CsvListWriter csv = new CsvListWriter(new FileWriter(f), CsvPreference.STANDARD_PREFERENCE);


        WbShapeFile shpFile = new WbShapeFile(getPath(name, layer));


        try {
            List<String> fields = new ArrayList<String>();
            fields.add("updated");
            fields.add("status");
            fields.add("WB");
            fields.add("WB1");
            fields.add("WB2");
            fields.add("WB3");
            fields.add("WB_SCORE");
            fields.addAll(shpFile.getFeatureNames());
            csv.write(fields);

            String tstamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            SimpleFeatureIterator iter = shpFile.getFeatureIter();
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
