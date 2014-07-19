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
 * Creates or updates a mapping csv file from a shapefile to WikiBrain.
 *
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

        // Fields from the shapefile that should be included in the final CSV
        List<String> extraFields = new ArrayList<String>();
        for (String fieldsKey : new String[] { "titles", "context", "other" }) {
            if (config.hasPath(fieldsKey)) {
                for (String field : config.getStringList(fieldsKey)) {
                    extraFields.add(field);
                }
            }
        }

        List<String> featureNames = shapeFile.getFeatureNames();
        try {
            writeHeader(csv, extraFields);
            SimpleFeatureIterator iter = shapeFile.getFeatureIter();
            while (iter.hasNext()) {
                Map<String, String> row = makeRow(featureNames, config.getStringList("key"), iter.next());
                writeRow(resolver, csv, extraFields, row);
            }
            iter.close();
        } finally {
            csv.close();
        }
    }

    private Map<String, String> makeRow(List<String> featureNames, List<String> keyFields, SimpleFeature row) {
        Map<String, String> rowMap = new HashMap<String, String>();
        for (int i = 0; i < row.getAttributeCount(); i++) {
            rowMap.put(featureNames.get(i), row.getAttribute(i).toString());
        }
        rowMap.put("WB_ID", row.getID());

        String key = "";
        for (String field : keyFields) {
            if (key.length() != 0) {
                key += "|";
            }
            key += rowMap.get(field);
        }
        rowMap.put("WB_KEY", key);

        return rowMap;
    }

    private void writeHeader(CsvListWriter writer, List<String> extraFields) throws IOException {
        List<String> fields = new ArrayList<String>();
        fields.add("WB_ID");
        fields.add("WB_KEY");
        fields.add("WB_UPDATED");
        fields.add("WB_STATUS");
        fields.add("WB_TITLE");
        fields.add("WB_GUESS1");
        fields.add("WB_GUESS2");
        fields.add("WB_GUESS3");
        fields.add("WB_SCORE");
        fields.addAll(extraFields);
        writer.write(fields);
    }

    private void writeRow(GeoResolver resolver, CsvListWriter writer, List<String> extraFields, Map<String, String> row) throws DaoException, IOException {
        String tstamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        LinkedHashMap<LocalPage, Double> guesses = resolver.resolve(row, 3);
        List<LocalPage> sorted = new ArrayList<LocalPage>(guesses.keySet());

        List<String> newRow = new ArrayList<String>();

        newRow.add(row.get("WB_ID"));
        newRow.add(row.get("WB_KEY"));
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
            newRow.add(row.get(f).toString());
        }
        writer.write(newRow);
    }

    public static void main(String args[]) throws Exception {
        Env env = EnvBuilder.envFromArgs(args);
        ShapeFileMatcher matcher = new ShapeFileMatcher(env);
        matcher.match("earth", "country", "naturalEarth");
    }
}
