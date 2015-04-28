package org.wikibrain.spatial.matcher;

import com.typesafe.config.Config;
import com.vividsolutions.jts.geom.Geometry;
import org.apache.commons.io.FileUtils;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.CsvMapReader;
import org.supercsv.prefs.CsvPreference;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.spatial.loader.SpatialDataDownloader;
import org.wikibrain.spatial.loader.SpatialDataFolder;
import org.wikibrain.spatial.WikiBrainShapeFile;
import org.wikibrain.utils.WpIOUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates or updates a mapping csv file from a shapefile to WikiBrain.
 *
 * @author Shilad Sen
 */
public class ShapeFileMatcher {
    private static final char STATUS_UNKNOWN = 'U';
    private static final char STATUS_VERIFIED = 'V';

    private static final Logger LOG = LoggerFactory.getLogger(ShapeFileMatcher.class);
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
        WikiBrainShapeFile shapeFile = downloader.download(refSys, layerGroup, datasetName, false);
        writeMatches(config, shapeFile);
    }

    public void writeMatches(Config config, WikiBrainShapeFile shapeFile) throws IOException, ConfigurationException, DaoException {
        Map<String, MappingInfo> existing = readExisting(shapeFile);
        File newFile = File.createTempFile("wbmapping", "csv");
        CsvListWriter csv = new CsvListWriter(WpIOUtils.openWriter(newFile), CsvPreference.STANDARD_PREFERENCE);

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
        GeoResolver resolver = new GeoResolver(env, config);
        try {
            writeHeader(csv, extraFields);
            SimpleFeatureIterator iter = shapeFile.getFeatureIter();
            int n = 0;
            while (iter.hasNext()) {
                if (n++ % 1000 == 0) {
                    LOG.info("Mapping row " + n + " of " + shapeFile.getFile());
                }
                SimpleFeature row = iter.next();
                Map<String, String> rowMap = makeRow(featureNames, config.getStringList("key"), row);
                Geometry geometry = (Geometry) row.getDefaultGeometry();
                writeRow(resolver, csv, extraFields, rowMap, geometry, existing);
            }
            iter.close();
        } finally {
            csv.close();
        }

        // Move original to a backup if it exists
        if (shapeFile.getMappingFile().exists()) {
            File backup = new File(shapeFile.getMappingFile().getAbsoluteFile() + ".bak");
            FileUtils.deleteQuietly(backup);
            FileUtils.moveFile(shapeFile.getMappingFile(), backup);
        }
        FileUtils.moveFile(newFile, shapeFile.getMappingFile());
    }

    /**
     * TODO: keep track of duplicate or missing keys with special status codes
     * @param shapeFile
     * @return
     * @throws IOException
     */
    private Map<String, MappingInfo> readExisting(WikiBrainShapeFile shapeFile) throws IOException {
        HashMap<String, MappingInfo> mapping = new HashMap<String, MappingInfo>();
        if (!shapeFile.hasMappingFile()) {
            return mapping;
        }
        CsvMapReader reader = new CsvMapReader(
                WpIOUtils.openBufferedReader(shapeFile.getMappingFile()),
                CsvPreference.STANDARD_PREFERENCE
        );
        String [] header = reader.getHeader(true);
        while (true) {
            Map<String, String> row = reader.read(header);
            if (row == null) {
                break;
            }
            MappingInfo info = new MappingInfo(row);
            if (!info.isUnknown()) {
                mapping.put(info.key, info);
            }
        }
        return mapping;
    }

    private Map<String, String> makeRow(List<String> featureNames, List<String> keyFields, SimpleFeature row) {
        Map<String, String> rowMap = new HashMap<String, String>();
        for (int i = 0; i < row.getAttributeCount(); i++) {
            rowMap.put(featureNames.get(i).toUpperCase(), row.getAttribute(i).toString());
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

    private void writeRow(GeoResolver resolver, CsvListWriter writer, List<String> extraFields, Map<String, String> row, Geometry geometry, Map<String, MappingInfo> existing) throws DaoException, IOException {
        String tstamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        LinkedHashMap<LocalPage, Double> guesses = resolver.resolve(row, geometry, 3);
        List<LocalPage> sorted = new ArrayList<LocalPage>(guesses.keySet());

        List<String> newRow = new ArrayList<String>();
        MappingInfo prev = existing.get(row.get("WB_KEY"));

        newRow.add(row.get("WB_ID"));
        newRow.add(row.get("WB_KEY"));
        newRow.add(prev == null ? tstamp : prev.timestamp);
        newRow.add(String.valueOf(prev == null ? STATUS_UNKNOWN : prev.status));

        // Calculate best title
        String title = "";
        if (prev != null) title = prev.title;
        else if (sorted.size() > 0) title = sorted.get(0).getTitle().getTitleStringWithoutNamespace();
        newRow.add(title);

        for (int i = 0; i < 3; i++) {
            if (sorted.size() > i) {
                newRow.add(sorted.get(i).getTitle().getTitleStringWithoutNamespace());
            } else {
                newRow.add("");
            }
        }

        double score = 0;
        if (sorted.size() >= 2) {
            score = 2 * guesses.get(sorted.get(0)) - guesses.get(sorted.get(1));
        } else if (sorted.size() == 1) {
            score = guesses.get(sorted.get(0));
        }
        newRow.add(""+score);

        for (String f : extraFields) {
            newRow.add(row.get(f).toString());
        }
        writer.write(newRow);
    }

    public static class MappingInfo {
        public final String key;
        public final String timestamp;
        public final char status;
        public final String title;

        public MappingInfo(Map<String, String> row) {
            key = row.get("WB_KEY");
            timestamp = row.get("WB_UPDATED");
            status = row.get("WB_STATUS").toUpperCase().charAt(0);
            title = row.get("WB_TITLE");
        }

        public boolean isUnknown() {
            return status == STATUS_UNKNOWN;
        }
    }

    public static void main(String args[]) throws Exception {
        Env env = EnvBuilder.envFromArgs(args);
        ShapeFileMatcher matcher = new ShapeFileMatcher(env);
        matcher.match("earth", "marine", "naturalEarth");
    }
}
