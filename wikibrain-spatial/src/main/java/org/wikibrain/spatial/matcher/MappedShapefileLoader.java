package org.wikibrain.spatial.matcher;

import com.typesafe.config.Config;
import com.vividsolutions.jts.geom.Geometry;
import org.apache.commons.cli.*;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.spatial.loader.SpatialDataFolder;
import org.wikibrain.spatial.WikiBrainShapeFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Shilad Sen
 */
public class MappedShapefileLoader {
    private static final Logger LOG = LoggerFactory.getLogger(MappedShapefileLoader.class);

    private final Env env;

    private final LocalPageDao pageDao;
    private final SpatialDataFolder folder;
    private final SpatialDataDao spatialDao;
    private final MetaInfoDao metaDao;
    private final UniversalPageDao conceptDao;
    private final Language lang;

    public MappedShapefileLoader(Env env) throws ConfigurationException, WikiBrainException {
        this.env = env;
        this.lang = env.getLanguages().getBestAvailableEnglishLang(false);
        this.pageDao = env.getConfigurator().get(LocalPageDao.class);
        this.conceptDao = env.getConfigurator().get(UniversalPageDao.class);
        this.metaDao = env.getConfigurator().get(MetaInfoDao.class);
        this.spatialDao = env.getConfigurator().get(SpatialDataDao.class);
        this.folder = new SpatialDataFolder(
                new File(env.getConfiguration().get().getString("spatial.dir")));
    }

    public void removeLayer(String refSys, String layerGroup) throws DaoException {
        this.spatialDao.removeLayer(refSys, layerGroup);
    }

    public void load(String refSys, String layerGroup, String dataset) throws IOException, DaoException {
        Config c = getConfig(refSys, layerGroup, dataset);
        WikiBrainShapeFile shapefile = folder.getShapeFile(refSys, layerGroup, dataset, c.getString("encoding"));
        Map<String, String> mapping = shapefile.readMapping();
        List<String> keyFields = c.getStringList("key");

        // Read in and uppercase feature names
        List<String> featureNames = shapefile.getFeatureNames();
        for (int i = 0; i < featureNames.size(); i++) {
            featureNames.set(i, featureNames.get(i).toUpperCase());
        }

        int missingTitles = 0;
        int missingConcepts = 0;
        int missingKeys = 0;
        int numRows = 0;
        int numMatches = 0;
        SimpleFeatureIterator iter = shapefile.getFeatureIter();
        while (iter.hasNext()) {
            if (++numRows % 1000 == 0) {
                LOG.info(String.format("for %s, matched %d of %d rows (no key = %d, no title = %d, no concept = %d)",
                        shapefile.getFile(), numMatches, numRows, missingKeys, missingTitles, missingConcepts));
            }
            SimpleFeature row = iter.next();
            String key = makeKey(keyFields, featureNames, row);
            if (!mapping.containsKey(key)) {
                missingKeys++;
            }
            String title = mapping.get(key);
            int pageId = pageDao.getIdByTitle(title, lang, NameSpace.ARTICLE);
            if (pageId < 0) {
                missingTitles++;
                continue;
            }
            int conceptId = conceptDao.getUnivPageId(lang, pageId);
            if (conceptId < 0) {
                missingConcepts++;
                continue;
            }
            numMatches++;
            Geometry geometry = (Geometry) row.getDefaultGeometry();
            spatialDao.saveGeometry(conceptId, layerGroup, refSys, geometry);
            metaDao.incrementRecords(Geometry.class);
        }
        iter.close();
        LOG.info(String.format("for %s, matched %d of %d rows (no key = %d, no title = %d, no concept = %d)",
                shapefile.getFile(), numMatches, numRows, missingKeys, missingTitles, missingConcepts));
    }

    private String makeKey(List<String> keyFields, List<String> featureNames, SimpleFeature row) {
        String key = "";
        for (String f : keyFields) {
            int i = featureNames.indexOf(f);
            if (i < 0) {
                throw new IllegalArgumentException("Wikibrain match key requires feature " + f + " not found in dbf");
            }
            if (key.length() > 0) {
                key += "|";
            }
            key += row.getAttribute(i).toString();
        }
        return key;
    }

    public void begin() throws DaoException {
        spatialDao.beginSaveGeometries();
    }

    public void end() throws DaoException {
        spatialDao.endSaveGeometries();
    }

    private Config getConfig(String refSys, String layerGroup, String dataset) {
        return env.getConfiguration().getConfig("spatial.datasets", refSys, layerGroup, dataset);
    }

    public static void main(String args[]) throws ConfigurationException, DaoException, IOException, WikiBrainException {
        Options options = new Options();
        options.addOption("d", false, "Delete existing data for specified layer group before starting.");
        options.addOption("n", true, "Dataset name.");
        options.addOption("r", true, "Reference system");

        options.addOption(
                new DefaultOptionBuilder()
                        .withDescription("Layer group")
                        .isRequired()
                        .hasArg()
                        .create("y"));

        options.addOption(
                new DefaultOptionBuilder()
                        .withDescription("Dataset name")
                        .isRequired()
                        .hasArg()
                        .create("n"));

        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("MatchedShapeFileLoader", options);
            return;
        }

        String refSys = cmd.getOptionValue("r", "earth");
        String layer = cmd.getOptionValue("y");
        String datasetName = cmd.getOptionValue("n");

        Env env = new EnvBuilder(cmd).build();

        MappedShapefileLoader loader = new MappedShapefileLoader(env);

        if (cmd.hasOption("d")) {
            loader.removeLayer(refSys, layer);
        }

        loader.begin();
        loader.load(refSys, layer, datasetName);
        loader.end();
    }
}
