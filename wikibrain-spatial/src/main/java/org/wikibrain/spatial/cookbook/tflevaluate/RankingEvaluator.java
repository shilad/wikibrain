package org.wikibrain.spatial.cookbook.tflevaluate;

import au.com.bytecode.opencsv.CSVWriter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
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
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.spatial.core.dao.SpatialNeighborDao;
import org.wikibrain.sr.MonolingualSRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by toby on 6/22/14.
 */

class KNNEntry {

    public KNNEntry(Integer itemId1, String title1, Integer itemId2, String title2, Double spatialDist, Integer KNNDist, Double SR){
        this.itemId1 = itemId1;
        this.title1 = title1;
        this.itemId2 = itemId2;
        this.title2 = title2;
        this.spatialDist = spatialDist;
        this.KNNDist = KNNDist;
        this.SR = SR;
    }

    public Integer itemId1;
    public String title1;
    public Integer itemId2;
    public String title2;
    public Double spatialDist;
    public Integer KNNDist;
    public Double SR;


}

class KNNComparator implements Comparator<KNNEntry> {
    @Override
    public int compare(KNNEntry a, KNNEntry b){
        return a.spatialDist < b.spatialDist ? -1 : a.spatialDist == b.spatialDist ? 0 : 1;
    }
}



public class RankingEvaluator {

    private static int WIKIDATA_CONCEPTS = 1;


    private static final Logger LOG = Logger.getLogger(KNNEvaluator.class.getName());

    private Random random = new Random();

    private final SpatialDataDao sdDao;
    private final LocalPageDao lpDao;
    private final UniversalPageDao upDao;
    private final SpatialNeighborDao snDao;
    private final List<Language> langs;
    private final Map<Language, MonolingualSRMetric> metrics;
    private final DistanceMetrics distanceMetrics;

    private final List<UniversalPage> concepts = new ArrayList<UniversalPage>();
    private final Map<Integer, Point> locations = new HashMap<Integer, Point>();
    private final Env env;
    private CSVWriter output;
    private String layerName = "wikidata";

    public RankingEvaluator(Env env, LanguageSet languages) throws ConfigurationException {
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
        this.metrics = new HashMap<Language, MonolingualSRMetric>();
        for(Language lang : langs){
            MonolingualSRMetric m = c.get(MonolingualSRMetric.class, "ensemble", "language", lang.getLangCode());
            metrics.put(lang, m);
        }

    }

    public void retrieveLocations(Map<Integer, Geometry> geometries) throws DaoException {
        LOG.log(Level.INFO, String.format("Found %d total geometries, now loading geometries", geometries.size()));

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
        LOG.info(String.format("Finish loading %d geometries with articles in %s", concepts.size(), langs));

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
        String[] headerEntries = new String[6 + langs.size()];
        headerEntries[0] = "ITEM_NAME_1";
        headerEntries[1] = "ITEM_ID_1";
        headerEntries[2] = "ITEM_NAME_2";
        headerEntries[3] = "ITEM_ID_2";
        headerEntries[4] = "SPATIAL_DISTANCE";
        headerEntries[5] = "KNN_DISTANCE";
        int counter = 0;
        for (Language lang : langs) {
            headerEntries[6 + counter] = lang.getLangCode() + "_SR";
            counter ++;
        }
        output.writeNext(headerEntries);
        output.flush();
    }

    private void writeRow(KNNEntry entry) throws WikiBrainException, IOException {


        String[] rowEntries = new String[6 + langs.size()];
        rowEntries[0] = entry.title1.toString();
        rowEntries[1] = entry.itemId1.toString();
        rowEntries[2] = entry.itemId2.toString();
        rowEntries[3] = entry.title2.toString();
        rowEntries[4] = entry.spatialDist.toString();
        rowEntries[5] = entry.KNNDist.toString();
        rowEntries[6] = entry.SR.toString();
        output.writeNext(rowEntries);
        output.flush();
        //if(CSVRowCounter % 1000 == 0
        //    LOG.info("Finished writing to CSV Row " + CSVRowCounter);
        //}

    }

  


    public List<KNNEntry> evaluate(final Integer originId, Integer numSamples) throws DaoException, IOException, WikiBrainException{
        //TODO: parallel this process...originId.size() should definitely be larger than the number of available threads
        final List<KNNEntry> resultEntries = Collections.synchronizedList(new ArrayList<KNNEntry>()); Collections.synchronizedList(new ArrayList<KNNEntry>());
        final String originTitle = upDao.getById(originId).getBestEnglishTitle(lpDao, true).getCanonicalTitle();
        final Point originGeom = sdDao.getGeometry(originId, layerName).getCentroid();
        ParallelForEach.range(0, numSamples, new Procedure<Integer>() {
            @Override
            public void call(Integer i) throws Exception {
                evaluateOneSample(originId, originTitle, originGeom, resultEntries);
            }
        });

        return resultEntries;
    }

    private void evaluateOneSample(Integer originId, String originTitle, Point originGeom, List<KNNEntry> resultEntries) throws DaoException, WikiBrainException, IOException {
        UniversalPage c2 = concepts.get(random.nextInt(concepts.size()));

        List<SRResult> results = new ArrayList<SRResult>();

        for (Language lang : langs) {
            MonolingualSRMetric sr = metrics.get(lang);
            results.add(sr.similarity(originId, c2.getLocalId(lang), false));
        }
        Point p2 = locations.get(c2.getUnivId()).getCentroid();
        GeodeticCalculator geoCalc = new GeodeticCalculator();
        geoCalc.setStartingGeographicPoint(originGeom.getX(), originGeom.getY());
        geoCalc.setDestinationGeographicPoint(p2.getX(), p2.getY());
        double km = geoCalc.getOrthodromicDistance() / 1000;


        MonolingualSRMetric sr = metrics.get(langs.get(0));

        resultEntries.add(new KNNEntry(originId, originTitle,  c2.getLocalId(langs.get(0)), c2.getBestEnglishTitle(lpDao, true).getCanonicalTitle(), km, null, sr.similarity(upDao.getById(originId).getLocalId(langs.get(0)), c2.getLocalId(langs.get(0)), false).getScore()));
    }

    private void sortAndPrint(List<List<KNNEntry>> entryListList,  String outPath) throws IOException, WikiBrainException{
        List<KNNEntry> wholeList = new ArrayList<KNNEntry>();
        for(List<KNNEntry> entryList : entryListList){
            Collections.sort(entryList, new KNNComparator());
            for(int i = 0; i < entryList.size(); i++){
                entryList.get(i).KNNDist = i + 1;
            }
            wholeList.addAll(entryList);
            this.output = new CSVWriter(new FileWriter(outPath), ',');
            writeHeader();
            for(KNNEntry entry : wholeList){
                writeRow(entry);
            }


        }

    }
    public static void main(String[] args) throws Exception {

        Env env = EnvBuilder.envFromArgs(args);
        Configurator conf = env.getConfigurator();
        RankingEvaluator evaluator = new RankingEvaluator(env, new LanguageSet("simple"));
        SpatialDataDao sdDao = conf.get(SpatialDataDao.class);
        Map<Integer, Geometry> geometries = sdDao.getAllGeometriesInLayer("wikidata", "earth");
        Integer list2[] = new Integer[100];
        List<Integer> geomList = getRandomSubList(Arrays.asList(geometries.keySet().toArray(list2)), 100);
        List<List<KNNEntry>> wholeList = new LinkedList<List<KNNEntry>>();
        evaluator.retrieveLocations(geometries);
        int counter = 0;
        for(Integer id :geomList){
            wholeList.add(evaluator.evaluate(id, 10000));
            counter ++;
            LOG.info(String.format("Done with iteration %d", counter));
        }
        evaluator.sortAndPrint(wholeList, "rank.csv");


    }



}
