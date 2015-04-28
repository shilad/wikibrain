package org.wikibrain.spatial.cookbook.tflevaluate;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import gnu.trove.set.TIntSet;
import org.geotools.referencing.GeodeticCalculator;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.Title;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.spatial.constants.RefSys;
import org.wikibrain.spatial.dao.SpatialContainmentDao;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.spatial.dao.SpatialNeighborDao;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by toby on 5/22/14.
 */
public class TopoEvaluator {

    private static int WIKIDATA_CONCEPTS = 1;


    private static final Logger LOG = LoggerFactory.getLogger(TopoEvaluator.class);

    private Random random = new Random();

    private final SpatialDataDao sdDao;
    private final LocalPageDao lpDao;
    private final UniversalPageDao upDao;
    private final SpatialNeighborDao snDao;
    private final SpatialContainmentDao scDao;
    private final List<Language> langs;
    private final Map<Language, SRMetric> metrics;
    private final DistanceMetrics distanceMetrics;

    private final List<UniversalPage> concepts = new ArrayList<UniversalPage>();
    private final Map<Integer, Point> locations = new HashMap<Integer, Point>();
    private final Map<Integer, List<Integer>> polygonWAG = new HashMap<Integer, List<Integer>>();
    private final Env env;
    private CSVWriter output;
    private String layerName = "wikidata";


    private Map<Integer, Integer> pointPolygonContainingMap = new HashMap<Integer, Integer>();
    private Map<Map.Entry, Integer> polygonPairDistanceMap = new HashMap<Map.Entry, Integer>();

    public TopoEvaluator(Env env, LanguageSet languages) throws ConfigurationException {
        this.env = env;
        //this.langs = new ArrayList<Language>(env.getLanguages().getLanguages());
        langs = new ArrayList<Language>();
        for(Language lang : languages.getLanguages())
            langs.add(lang);

        // Get data access objects
        Configurator c = env.getConfigurator();
        this.sdDao = c.get(SpatialDataDao.class);
        this.lpDao = c.get(LocalPageDao.class);
        this.upDao = c.get(UniversalPageDao.class);
        this.snDao = c.get(SpatialNeighborDao.class);
        this.scDao = c.get(SpatialContainmentDao.class);

        this.distanceMetrics = new DistanceMetrics(env, c, snDao);

        // build SR metrics
        this.metrics = new HashMap<Language, SRMetric>();
        for(Language lang : langs){
            SRMetric m = c.get(SRMetric.class, "ensemble", "language", lang.getLangCode());
            metrics.put(lang, m);
        }
    }
    public void retrieveAllLocations(String pointLayer, String polygonLayer) throws DaoException, WikiBrainException{

        Map<Integer, Geometry> geometries = sdDao.getAllGeometriesInLayer(pointLayer, "earth");
        retrieveLocations(geometries, pointLayer, polygonLayer);

    }

    public void retrieveLocations(Map<Integer, Geometry> geometries, String pointLayer, String polygonLayer) throws DaoException, WikiBrainException {
        // Get all known concept geometries

        Map<Integer, Geometry> polygons = sdDao.getAllGeometriesInLayer(polygonLayer, "earth");

        LOG.info(String.format("Found %d total geometries, now loading geometries", geometries.size()));

        // Build up list of concepts in all languages
        for (Integer conceptId : geometries.keySet()){
            UniversalPage concept = upDao.getById(conceptId);
            if (concept != null && concept.hasAllLanguages(new LanguageSet(langs))) {
                concepts.add(concept);
                Geometry g1 = geometries.get(conceptId);
                locations.put(conceptId, g1.getCentroid());
                if (concepts.size() % 1000 == 0) {
                    LOG.info(String.format("Loaded %d geometries with articles in %s...", concepts.size(), langs));
                }
            }
        }
        LOG.info(String.format("Found %d geometries with articles in %s", concepts.size(), langs));

        //Build polygon WAG
        //Build point-polygon mapping
        int counter = 0;
        int dummy = 0;
        for(Map.Entry<Integer, Geometry> i : polygons.entrySet()){
            counter ++;
            if(counter % 1 == 0){
                LOG.info(String.format("Processing the %d th polygon : %s out of %d", counter, upDao.getById(i.getKey()).getBestEnglishTitle(lpDao, true).getCanonicalTitle(), polygons.size()));
            }
            Map<Integer, Geometry> neighbors = snDao.getNeighbors(i.getValue(), polygonLayer, "earth", new HashSet<Integer>());
            if(!polygonWAG.containsKey(i.getKey())){
                polygonWAG.put(i.getKey(), new ArrayList<Integer>());
            }
            polygonWAG.get(i.getKey()).addAll(neighbors.keySet());
            Set<String> layerSet = new HashSet<String>();
            layerSet.add(pointLayer);
            TIntSet containedItem = scDao.getContainedItemIds(i.getValue(), "earth", layerSet, SpatialContainmentDao.ContainmentOperationType.CONTAINMENT);
            dummy++;
            for(Integer k : containedItem.toArray()){
                pointPolygonContainingMap.put(k, i.getKey());
            }
            dummy++;

        }






    }

    /**
     * Evaluate a specified number of random pairs from loaded concepts
     * @param outputPath
     * @param numSamples
     * @throws java.io.IOException
     */
    public void evaluateSample(File outputPath, int numSamples) throws IOException {
        this.output = new CSVWriter(new FileWriter(outputPath), ',');
        writeHeader();
        if(concepts.size() == 0)
            LOG.warn("No concept has been retrieved");

        ParallelForEach.range(0, numSamples, new Procedure<Integer>() {
            @Override
            public void call(Integer i) throws Exception {
                evaluateOneSample();
            }
        });

        this.output.close();
    }

    private void evaluateOneSample() throws DaoException, WikiBrainException, IOException {
        UniversalPage c1 = concepts.get(random.nextInt(concepts.size()));
        UniversalPage c2 = concepts.get(random.nextInt(concepts.size()));

        List<SRResult> results = new ArrayList<SRResult>();

        for (Language lang : langs) {

            SRMetric sr = metrics.get(lang);
            results.add(sr.similarity(c1.getLocalId(lang), c2.getLocalId(lang), false));
            if(sr.similarity(c1.getLocalId(lang), c2.getLocalId(lang), false) == null){
                LOG.warn(String.format("error calculating SR for universal page %d %s and %d %s", c1.getUnivId(), c1.getBestEnglishTitle(lpDao, true), c2.getUnivId(), c2.getBestEnglishTitle(lpDao, true)));            }
        }

        writeRow(c1, c2, results);
    }

    private void writeHeader() throws IOException {
        String[] headerEntries = new String[8 + langs.size()];
        headerEntries[0] = "ITEM_NAME_1";
        headerEntries[1] = "ITEM_ID_1";
        headerEntries[2] = "CONTAINED_1";
        headerEntries[3] = "ITEM_NAME_2";
        headerEntries[4] = "ITEM_ID_2";
        headerEntries[5] = "CONTAINED_2";
        headerEntries[6] = "SPATIAL_DISTANCE";
        headerEntries[7] = "TOPO_DISTANCE";

        int counter = 0;
        for (Language lang : langs) {
            headerEntries[8 + counter] = lang.getLangCode() + "_SR";
            counter ++;
        }
        output.writeNext(headerEntries);
        output.flush();
    }

    public Integer polygonDistance(Integer itemIdA, Integer itemIdB, String layer, String refSys){
        Map.Entry<Integer,Integer> keyEntry = new AbstractMap.SimpleEntry<Integer, Integer>(itemIdA, itemIdB);
        if (polygonPairDistanceMap.containsKey(keyEntry)){
            return polygonPairDistanceMap.get(keyEntry);
        }

        Map<Integer, Integer> distList = new HashMap<Integer, Integer>();
        for(Integer k : polygonWAG.keySet()){
            distList.put(k, -1);
        }
        Queue<Integer> Q = new LinkedList<Integer>();
        Set<Integer> V = new HashSet<Integer>();
        distList.put(itemIdA, 0);
        Q.add(itemIdA);
        V.add(itemIdA);

        while(!Q.isEmpty()){
            Integer t = Q.poll();
            if(t.equals(itemIdB)){
                return distList.get(t);
            }
            if(!polygonWAG.containsKey(t))
                continue;

            for(Integer k : polygonWAG.get(t)){
                if(!V.contains(k)){
                    V.add(k);
                    Q.add(k);
                    distList.put(k, distList.get(t) + 1);
                    polygonPairDistanceMap.put(new AbstractMap.SimpleEntry(itemIdA, k), distList.get(k));
                }
            }

        }
        return -1;
    }

    private void writeRow(UniversalPage c1, UniversalPage c2, List<SRResult> results) throws WikiBrainException, IOException, DaoException {
        try {
            double km;
            if((!locations.containsKey(c1.getUnivId())) || (!locations.containsKey(c2.getUnivId())))
                return;
            Point p1 = locations.get(c1.getUnivId()).getCentroid();
            Point p2 = locations.get(c2.getUnivId()).getCentroid();


            //TODO: change this to a topological metric


            GeodeticCalculator geoCalc = new GeodeticCalculator();
            geoCalc.setStartingGeographicPoint(p1.getX(), p1.getY());
            geoCalc.setDestinationGeographicPoint(p2.getX(), p2.getY());
            km = geoCalc.getOrthodromicDistance() / 1000;




            if(! (pointPolygonContainingMap.containsKey(c1.getUnivId()) && pointPolygonContainingMap.containsKey(c2.getUnivId())))
                return;
            double TopoDist = polygonDistance(pointPolygonContainingMap.get(c1.getUnivId()), pointPolygonContainingMap.get(c2.getUnivId()), layerName, "earth");
            Title t1 = c1.getBestEnglishTitle(lpDao, true);
            Title t2 = c2.getBestEnglishTitle(lpDao, true);

            String[] rowEntries = new String[8 + langs.size()];
            rowEntries[0] = t1.getCanonicalTitle();
            rowEntries[1] = String.valueOf(c1.getUnivId());
            rowEntries[2] = upDao.getById(pointPolygonContainingMap.get(c1.getUnivId())).getBestEnglishTitle(lpDao, true).getCanonicalTitle();
            rowEntries[3] = t2.getCanonicalTitle();
            rowEntries[4] = String.valueOf(c2.getUnivId());
            rowEntries[5] = upDao.getById(pointPolygonContainingMap.get(c2.getUnivId())).getBestEnglishTitle(lpDao, true).getCanonicalTitle();
            rowEntries[6] = String.format("%.2f", km);
            rowEntries[7] = String.valueOf(TopoDist);
            int counter = 0;
            for (SRResult result : results) {
                if(result != null)
                    rowEntries[8 + counter] = String.format("%.2f", result.getScore());
                else
                    rowEntries[8 + counter] = "0";
                counter ++;
            }
            output.writeNext(rowEntries);
            output.flush();
        }
        catch (Exception e){
            LOG.warn(String.format("error writing row for universal page %d %s and %d %s", c1.getUnivId(), c1.getBestEnglishTitle(lpDao, true), c2.getUnivId(), c2.getBestEnglishTitle(lpDao, true)));
            //do nothing
        }
    }

    public static void main(String[] args) throws Exception {

        Env env = EnvBuilder.envFromArgs(args);
        Configurator conf = env.getConfigurator();
        TopoEvaluator evaluator = new TopoEvaluator(env, new LanguageSet("simple"));
        SpatialDataDao sdDao = conf.get(SpatialDataDao.class);
        //Map<Integer, Geometry> allGeometries = sdDao.getAllGeometriesInLayer("wikidata", "earth");
        //Map<Integer, Geometry> geometryMap = new HashMap<Integer, Geometry>();




        Set<String> subLayers = Sets.newHashSet();
        subLayers.add("wikidata");
        SpatialContainmentDao scDao =  conf.get(SpatialContainmentDao.class);
        TIntSet containedItemIds = scDao.getContainedItemIds(30, "country", RefSys.EARTH,
                subLayers, SpatialContainmentDao.ContainmentOperationType.CONTAINMENT);

        LinkedList<Integer> itemIdList = new LinkedList<Integer>();
        int[] itemIds = containedItemIds.toArray();
        for(Integer k : itemIds){
            itemIdList.add(k);
        }

        Map<Integer, Geometry> geometryMap = sdDao.getBulkGeometriesInLayer(itemIdList, "wikidata", "earth");

        evaluator.retrieveLocations(geometryMap, "wikidata", "states");


        //evaluator.retrieveAllLocations("wikidata", "country");
        evaluator.evaluateSample(new File("TopoEval.csv"), 500000);
    }










}
