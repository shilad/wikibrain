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
import org.wikibrain.spatial.loader.SpatialDataDownloader;
import org.wikibrain.spatial.loader.SpatialDataFolder;
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
    private final SpatialDataFolder dir;
    private final SpatialDataDownloader downloader;


    public ShapeFileMatcher(Env env) {
        this.env = env;
        this.dir = new SpatialDataFolder(new File(env.getConfiguration().get().getString("spatial.dir")));
        this.downloader = new SpatialDataDownloader(env);
    }

    public void match(String refSys, String layerGroup, String datasetName) throws IOException, InterruptedException, DaoException, ConfigurationException {
        Config config = env.getConfiguration().getConfig("spatial.datasets", refSys, layerGroup, datasetName);
        WbShapeFile shapeFile = downloader.download(refSys, layerGroup, datasetName);
        writeMatches(config, shapeFile);
    }

    public void writeMatches(Config config, WbShapeFile shapeFile) throws IOException, ConfigurationException, DaoException {
        GeoResolver resolver = new GeoResolver(env, config);
        File csvPath = shapeFile.getMappingFile();
        CsvListWriter csv = new CsvListWriter(WpIOUtils.openWriter(csvPath), CsvPreference.STANDARD_PREFERENCE);

        List<String> featureNames = shapeFile.getFeatureNames();

        List<String> extraFields = new ArrayList<String>();
        for (String fieldsKey : new String[] { "titles", "context", "other" }) {
            if (config.hasPath(fieldsKey)) {
                for (String field : config.getStringList(fieldsKey)) {
                    extraFields.add(field);
                }
            }
        }

        try {
            List<String> fields = new ArrayList<String>();
            fields.add("ID");
            fields.add("KEY");
            fields.add("updated");
            fields.add("status");
            fields.add("WB");
            fields.add("WB1");
            fields.add("WB2");
            fields.add("WB3");
            fields.add("WB_SCORE");
            fields.addAll(extraFields);
            csv.write(fields);

            String tstamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            SimpleFeatureIterator iter = shapeFile.getFeatureIter();
            while (iter.hasNext()) {
                SimpleFeature row = iter.next();

                Map<String, String> rowMap = new HashMap<String, String>();
                for (int i = 0; i < row.getAttributeCount(); i++) {
                    rowMap.put(featureNames.get(i), row.getAttribute(i).toString());
                }
                LinkedHashMap<LocalPage, Double> guesses = resolver.resolve(rowMap, 3);
                List<LocalPage> sorted = new ArrayList<LocalPage>(guesses.keySet());

                String key = "";
                for (String field : config.getStringList("key")) {
                    if (key.length() != 0) {
                        key += "|";
                    }
                    key += rowMap.get(field);
                }

                List<String> newRow = new ArrayList<String>();

                newRow.add(row.getID());
                newRow.add(key);
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

                for (String f : extraFields) {
                    newRow.add(rowMap.get(f).toString());
                }
                csv.write(newRow);
            }
            iter.close();
        } finally {
            csv.close();
        }
    }

    public static void main(String args[]) throws Exception {
        Env env = EnvBuilder.envFromArgs(args);
        ShapeFileMatcher matcher = new ShapeFileMatcher(env);
        matcher.match("earth", "country", "naturalEarth");
    }
}
