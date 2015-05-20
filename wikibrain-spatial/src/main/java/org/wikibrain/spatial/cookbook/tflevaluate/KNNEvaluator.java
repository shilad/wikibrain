package org.wikibrain.spatial.cookbook.tflevaluate;

import au.com.bytecode.opencsv.CSVWriter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
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
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.spatial.dao.SpatialNeighborDao;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by toby on 5/17/14.
 */
public class KNNEvaluator {


    private static int WIKIDATA_CONCEPTS = 1;


    private static final Logger LOG = LoggerFactory.getLogger(KNNEvaluator.class);

    private Random random = new Random();

    private final SpatialDataDao sdDao;
    private final LocalPageDao lpDao;
    private final UniversalPageDao upDao;
    private final SpatialNeighborDao snDao;
    private final List<Language> langs;
    private final Map<Language, SRMetric> metrics;
    private final DistanceMetrics distanceMetrics;

    private final List<UniversalPage> concepts = new ArrayList<UniversalPage>();
    private final Map<Integer, Point> locations = new HashMap<Integer, Point>();
    private final Env env;
    private CSVWriter output;
    private String layerName = "wikidata";



    public KNNEvaluator(Env env, LanguageSet languages) throws ConfigurationException {
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

        this.distanceMetrics = new DistanceMetrics(env, c, snDao);

        // build SR metrics
        this.metrics = new HashMap<Language, SRMetric>();
        for(Language lang : langs){
            SRMetric m = c.get(SRMetric.class, "ensemble", "language", lang.getLangCode());
            metrics.put(lang, m);
        }

    }

    public static <T> List<T> getRandomSubList(List<T> input, int subsetSize)
    {
        if(subsetSize > input.size())
            subsetSize = input.size();
        Random r = new Random();
        int inputSize = input.size();
        for (int i = 0; i < subsetSize; i++)
        {
            int indexToSwap = i + r.nextInt(inputSize - i);
            T temp = input.get(i);
            input.set(i, input.get(indexToSwap));
            input.set(indexToSwap, temp);
        }
        return input.subList(0, subsetSize);
    }

    public static <T>T getRandomElement(List<T> input){
        return getRandomSubList(input, 1).get(0);

    }

    private void writeHeader() throws IOException {
        String[] headerEntries = new String[5 + langs.size()];
        headerEntries[0] = "ITEM_NAME_1";
        headerEntries[1] = "ITEM_ID_1";
        headerEntries[2] = "ITEM_NAME_2";
        headerEntries[3] = "ITEM_ID_2";
        headerEntries[4] = "KNN_DISTANCE";
        int counter = 0;
        for (Language lang : langs) {
            headerEntries[5 + counter] = lang.getLangCode() + "_SR";
            counter ++;
        }
        output.writeNext(headerEntries);
        output.flush();
    }

    private void writeRow(UniversalPage c1, UniversalPage c2, Integer KNNDistance, List<SRResult> results) throws WikiBrainException, IOException {

        Title t1 = c1.getBestEnglishTitle(lpDao, true);
        Title t2 = c2.getBestEnglishTitle(lpDao, true);

        String[] rowEntries = new String[5 + langs.size()];
        rowEntries[0] = t1.getCanonicalTitle();
        rowEntries[1] = String.valueOf(c1.getUnivId());
        rowEntries[2] = t2.getCanonicalTitle();
        rowEntries[3] = String.valueOf(c2.getUnivId());
        rowEntries[4] = String.valueOf(KNNDistance);
        int counter = 0;
        for (SRResult result : results) {
            rowEntries[5 + counter] = String.valueOf(result.getScore());
            counter ++;
        }
        output.writeNext(rowEntries);
        output.flush();
        //if(CSVRowCounter % 1000 == 0
        //    LOG.info("Finished writing to CSV Row " + CSVRowCounter);
        //}

    }

    /**
     *
     * @param originId Origins to start
     * @param k K as in "K-nearest neighbors"
     * @param limitPerLevel Number of samples to pick from each "K-nearest neighbors" to evaluate
     * @param limitBranch not used
     * @param maxDist Max distance (depth of search)
     * @param outputPath The path for the output CSV file
     * @throws DaoException
     * @throws IOException
     */


    public void evaluate(Iterable<Integer> originId, final Integer k, final Integer limitPerLevel, Integer limitBranch, final Integer maxDist, String outputPath) throws DaoException, IOException{
        //TODO: parallel this process...originId.size() should definitely be larger than the number of available threads
        this.output = new CSVWriter(new FileWriter(outputPath), ',');

        writeHeader();
        ParallelForEach.iterate(originId.iterator(), new Procedure<Integer>() {
            @Override
            public void call(Integer arg) throws Exception {
                evaluateForOne(arg, sdDao.getGeometry(arg, layerName, "earth"), k, limitPerLevel, maxDist);
            }
        });
    }

    //TODO: return only a limited number of pairs for each recursion

    public void evaluateForOne(Integer originId, Geometry originGeom, Integer k, Integer limitPerLevel, Integer maxDist) throws DaoException{
        Integer CSVRowCounter = 0;
        Set<Integer> excludeIds = new HashSet<Integer>();
        Map<Integer, Integer> evalResult = new HashMap<Integer, Integer>();
        List<Integer> nodeToDiscover = new LinkedList<Integer>();
        Map<Integer, Geometry> geometryMap = new HashMap<Integer, Geometry>();
        evalResult.put(originId, 0);
        geometryMap.put(originId, originGeom);
        excludeIds.add(originId);
        nodeToDiscover.add(originId);
        int counter = 0;
        while(counter < maxDist){
            counter ++;
            Integer nodeToExpand = getRandomElement(nodeToDiscover);
            Map<Integer, Geometry> thisLevel;
            //No need to lock as they are all Read-Read
            // synchronized (this){
                thisLevel = snDao.getKNNeighbors(geometryMap.get(nodeToExpand), k, layerName, "earth", excludeIds);
            //}
            if(thisLevel == null || thisLevel.size() == 0)
                break;
            excludeIds.addAll(thisLevel.keySet());



            List<Integer> nodesToPutInTheCSV = getRandomSubList(new ArrayList(thisLevel.keySet()), limitPerLevel);

            for(Integer i : nodesToPutInTheCSV){
                evalResult.put(i, counter);
            }


            Integer nodeToAdd = getRandomElement(new ArrayList<Integer>(thisLevel.keySet()));
            nodeToDiscover.add(nodeToAdd);
            geometryMap.put(nodeToAdd, thisLevel.get(nodeToAdd));
        }

        for(Integer x : evalResult.keySet()){
            for(Integer y : evalResult.keySet()){
                try {
                    List<SRResult> results = new ArrayList<SRResult>();
                    //synchronized (this){
                        for (Language lang : langs) {
                            SRMetric sr = metrics.get(lang);
                            results.add(sr.similarity(upDao.getById(x).getLocalId(lang), upDao.getById(y).getLocalId(lang), false));
                        }
                        writeRow(upDao.getById(x), upDao.getById(y), Math.abs(evalResult.get(x) - evalResult.get(y)), results);
                    //}
                    CSVRowCounter++;
                    if(CSVRowCounter % 5000 == 0)
                        LOG.info("Thread " + Thread.currentThread().getId() + " Now printing " + CSVRowCounter + " From " + x + " To " + y + " at level " + Math.abs(evalResult.get(x) - evalResult.get(y)));
                }
                catch (Exception e){
                    //do nothing
                }
            }
        }

    }


/*
    public Map<Integer, Integer> evaluateRecursive(Integer originId, Geometry originGeom, Integer k, Integer limitPerLevel, Integer limitBranch, Integer maxDist) throws DaoException{
        if (maxDist == 0){
            return new HashMap<Integer, Integer>();
        }
        if (maxDist < currentLevel){
            currentLevel = maxDist;
            LOG.info("reached level " + currentLevel);
        }
        excludeIds.add(originId);
        Map<Integer, Geometry> thisLevel = snDao.getKNNeighbors(originGeom, k, layerName, "earth", excludeIds);
        excludeIds.addAll(thisLevel.keySet());
        Map<Integer, Integer> thisLevelRes = new HashMap<Integer, Integer>();

        if(limitBranch > thisLevel.size())
            limitBranch = thisLevel.size();

        List<Integer> candidateList = getRandomSubList(new LinkedList<Integer>(thisLevel.keySet()), limitBranch);


        for(Integer i : candidateList){
            thisLevelRes.put(i, 1);
            Map<Integer, Integer> childLevelRes = evaluateRecursive(i, thisLevel.get(i), k, limitPerLevel, limitBranch, maxDist - 1);
            for(Integer q: childLevelRes.keySet()){
                thisLevelRes.put(q, childLevelRes.get(q) + 1);
            }
        }
        UniversalPage originPage = upDao.getById(originId, WIKIDATA_CONCEPTS);
        for(Integer i: thisLevelRes.keySet()){
            try {
                List<SRResult> results = new ArrayList<SRResult>();
                for (Language lang : langs) {
                    MonolingualSRMetric sr = metrics.get(lang);
                    results.add(sr.similarity(originPage.getLocalId(lang), upDao.getById(i, WIKIDATA_CONCEPTS).getLocalId(lang), false));
                }
                LOG.info("Now printing " + CSVRowCounter + " From " + originId + " To " + i + " at level " + maxDist);
                writeRow(originPage, upDao.getById(i, WIKIDATA_CONCEPTS), thisLevelRes.get(i), results);
            }
            catch (Exception e){
                //do nothing
            }

        }

        if(limitPerLevel > thisLevelRes.size())
            limitPerLevel = thisLevelRes.size();

        List<Integer> returnList = getRandomSubList(new LinkedList<Integer>(thisLevelRes.keySet()), limitPerLevel);
        Map<Integer, Integer> returnMap = new HashMap<Integer, Integer>();
        for(Integer i : returnList){
            returnMap.put(i, thisLevelRes.get(i));
        }

        return returnMap;
    }
 */

    public static void main(String[] args) throws Exception {

        Env env = EnvBuilder.envFromArgs(args);
        Configurator conf = env.getConfigurator();
        KNNEvaluator evaluator = new KNNEvaluator(env, new LanguageSet("simple"));
        SpatialDataDao sdDao = conf.get(SpatialDataDao.class);
        Set<Integer> originSet = new HashSet<Integer>();
        originSet.add(36091);originSet.add(956);originSet.add(64);originSet.add(258);originSet.add(60);originSet.add(65);originSet.add(90);originSet.add(84);originSet.add(1490);
        evaluator.evaluate(originSet, 100, 5, 1, 30, "test-topo.csv");
    }

}
