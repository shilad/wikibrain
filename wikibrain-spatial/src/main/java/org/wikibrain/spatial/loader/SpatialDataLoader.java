package org.wikibrain.spatial.loader;

import com.vividsolutions.jts.geom.Geometry;
import org.apache.commons.cli.*;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.spatial.constants.Layers;
import org.wikibrain.spatial.constants.RefSys;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.spatial.matcher.MappedShapefileLoader;
import org.wikibrain.wikidata.WikidataDao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author bjhecht, Shilad
 */
public class SpatialDataLoader {

    private static final Logger LOG = LoggerFactory.getLogger(SpatialDataLoader.class);

    private final Env env;
    private final LanguageSet langs;
    private final MetaInfoDao metaDao;
    private final SpatialDataDao spatialDao;
    private final WikidataDao wdDao;

    public SpatialDataLoader(Env env) throws ConfigurationException, WikiBrainException {
        this.env = env;
        this.langs = env.getLanguages();
        this.spatialDao = env.getConfigurator().get(SpatialDataDao.class);
        this.metaDao = env.getConfigurator().get(MetaInfoDao.class);
        this.wdDao = env.getConfigurator().get(WikidataDao.class);
    }

    public void loadExogenousData(String refSys, String layerGroup, String dataset) throws IOException, InterruptedException, DaoException, WikiBrainException, ConfigurationException {
        spatialDao.removeLayer(refSys, layerGroup);
        SpatialDataDownloader downloader  = new SpatialDataDownloader(env.getConfiguration());
        downloader.download(refSys, layerGroup, dataset);
        MappedShapefileLoader shapefileLoader = new MappedShapefileLoader(env);
        shapefileLoader.load(refSys, layerGroup, dataset);
    }

    public void loadWikidataData() throws DaoException {
        spatialDao.removeLayer(RefSys.EARTH, Layers.WIKIDATA);
        WikidataLayerLoader loader = new WikidataLayerLoader(metaDao, wdDao, spatialDao);
        loader.loadData(langs);
    }

    public void dropAllLayers() throws DaoException {
        LOG.info("dropping all spatial data");
        for (String refSys : spatialDao.getAllRefSysNames()) {
            for (String layer : spatialDao.getAllLayerNames(refSys)) {
                LOG.info("dropping spatial data for layer " + layer);
                spatialDao.removeLayer(refSys, layer);
            }
        }
        metaDao.clear(Geometry.class);
    }

    public void dropLayers(List<LayerInfo> layers) throws DaoException {
        Set<String> dropped = new HashSet<String>();
        for (LayerInfo info : layers) {
            if (!dropped.contains(info.referenceSystem + info.layer)) {
                LOG.info("dropping spatial data for layer " + info.layer);
                spatialDao.removeLayer(info.referenceSystem, info.layer);
                dropped.add(info.referenceSystem + info.layer);
            }
        }
    }

    public void loadLayers(List<LayerInfo> layers) throws DaoException, InterruptedException, WikiBrainException, ConfigurationException, IOException {
        for (LayerInfo layer : layers) {
            if (layer.layer.equalsIgnoreCase(Layers.WIKIDATA)) {
                loadWikidataData();
            } else {
                loadExogenousData(layer.referenceSystem, layer.layer, layer.dataset);
            }
        }
    }

    public static class LayerInfo {
        private final String referenceSystem;
        private final String layer;
        private final String dataset;

        public LayerInfo(String arg) {
            if (arg.trim().equals(Layers.WIKIDATA)) {
                arg = String.format("%s,%s,%s", RefSys.EARTH, Layers.WIKIDATA, Layers.WIKIDATA);
            }
            String tokens[] = arg.split(",");
            if (tokens.length == 2) {
                referenceSystem = RefSys.EARTH;
                layer = tokens[0].trim();
                dataset = tokens[1].trim();
            } else if (tokens.length == 3) {
                referenceSystem = tokens[0].trim();
                layer = tokens[1].trim();
                dataset = tokens[2].trim();
            } else {
                throw new IllegalArgumentException("Invalid layer specified: " + arg);
            }
        }
    }

    public static void main(String args[]) throws ConfigurationException, DaoException, WikiBrainException, IOException, InterruptedException {
        Options options = new Options();

        //Specify the Datasets
        options.addOption(
                new DefaultOptionBuilder()
                        .withLongOpt("delete")
                        .withDescription("Delete data from all layers before loading anything")
                        .create("d"));

        //Specify the Datasets
        options.addOption(
                new DefaultOptionBuilder()
                        .hasArgs()
                        .withLongOpt("layers")
                        .withDescription("Load the specified layers. Format can be one of 'wikidata', 'layer,dataset' or 'referenceSystem,layer,dataset'")
                        .create("y"));

        EnvBuilder.addStandardOptions(options);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Invalid option usage: " + e.getMessage());
            new HelpFormatter().printHelp("SpatialDataLoader", options);
            System.exit(1);
            return;
        }

        String [] layerArgs = cmd.getOptionValues("y");
        if (layerArgs == null || layerArgs.length == 0) {
            layerArgs = new String[] {
                    "wikidata",
                    "country,naturalEarth",
                    "state,naturalEarth",
            };
        }

        List<LayerInfo> layers = new ArrayList<LayerInfo>();
        for (String arg : layerArgs) {
            try {
                layers.add(new LayerInfo(arg));
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid layer '" + arg + "'. Format must be 'layer,dataset' or 'referenceSystem,layer,dataset");
                new HelpFormatter().printHelp("SpatialDataLoader", options);
                System.exit(1);
                return;
            }
        }

        Env env = new EnvBuilder(cmd).build();
        SpatialDataLoader loader = new SpatialDataLoader(env);
        Configurator conf = env.getConfigurator();
        SpatialDataDao spatialDao = conf.get(SpatialDataDao.class);

        // Drop necessary data
        if (cmd.hasOption("d")) {
            loader.dropAllLayers();
        } else {
            loader.dropLayers(layers);
        }

        spatialDao.beginSaveGeometries();
        loader.loadLayers(layers);
        spatialDao.endSaveGeometries();

        LOG.info("optimizing database.");
        spatialDao.optimize();
    }
}
