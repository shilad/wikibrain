package org.wikibrain.spatial.cookbook.tflevaluate;

import au.com.bytecode.opencsv.CSVWriter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.geotools.referencing.GeodeticCalculator;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.Title;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.spatial.dao.SpatialDataDao;
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
 *
 */
public class ToblersLawEvaluator {


    private static int WIKIDATA_CONCEPTS = 1;


    private static final Logger LOG = LoggerFactory.getLogger(ToblersLawEvaluator.class);

    private Random random = new Random();

    private final SpatialDataDao sdDao;
    private final LocalPageDao lpDao;
    private final UniversalPageDao upDao;
    private final List<Language> langs;
    private final Map<Language, SRMetric> metrics;

    private final List<UniversalPage> concepts = new ArrayList<UniversalPage>();
    private final Map<Integer, Point> locations = new HashMap<Integer, Point>();
    private final Env env;
    private CSVWriter output;


    public ToblersLawEvaluator(Env env, LanguageSet languages) throws ConfigurationException {
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

        // build SR metrics
        this.metrics = new HashMap<Language, SRMetric>();
        for(Language lang : langs){
            SRMetric m = c.get(SRMetric.class, "ensemble", "language", lang.getLangCode());
            metrics.put(lang, m);
        }
    }

    /**
     * Load all locations from all language editions of Wikipedia to concepts
     *
     * @throws DaoException
     */

    public void retrieveAllLocations() throws DaoException {
        // Get all known concept geometries
        Map<Integer, Geometry> geometries = sdDao.getAllGeometriesInLayer("wikidata", "earth");
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
    }

    /**
     * Load specified tagged geometries to concepts
     * @param geometries
     * @throws DaoException
     */
    public void retrieveLocations(Map<Integer, Geometry> geometries) throws DaoException {
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
        LOG.info(String.format("Finish loading %d geometries with articles in %s", concepts.size(), langs));

    }



    /**
     * Evaluate a specified number of random pairs from loaded concepts
     * @param outputPath
     * @param numSamples
     * @throws IOException
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
        }

        writeRow(c1, c2, results);
    }

    /**
     * Evaluate all pairs from loaded concepts
     * @param outputPath
     * @throws IOException
     * @throws DaoException
     * @throws WikiBrainException
     */
    public void evaluateAll(File outputPath) throws IOException, DaoException, WikiBrainException {
        this.output = new CSVWriter(new FileWriter(outputPath), ',');
        writeHeader();
        if(concepts.size() == 0)
            LOG.warn("No cocept has been retrieved");
        int counter = 0;
        int total_size = concepts.size() * concepts.size();

        for(UniversalPage c1: concepts){
            for(UniversalPage c2: concepts){
                counter ++;
                if(counter % 1000 == 0)
                    LOG.info(String.format("Evaluating %d out of %d pairs", counter, total_size));
                if(c1.equals(c2))
                    continue;
                List<SRResult> results = new ArrayList<SRResult>();
                for (Language lang : langs) {
                    SRMetric sr = metrics.get(lang);
                    results.add(sr.similarity(c1.getLocalId(lang), c2.getLocalId(lang), false));
                }
                writeRow(c1, c2, results);


            }
        }

        this.output.close();

    }

    /**
     *
     * @return A list of parsed concepts
     */
    public List<UniversalPage> getParsedConcepts(){
        return concepts;
    }

    /**
     * Evaluate all pairs that one location is in "concepts1" and the other one is in "concepts2"
     * @param outputPath
     * @param concepts1
     * @param concepts2
     * @throws IOException
     * @throws DaoException
     * @throws WikiBrainException
     */
    public void evaluateBipartite(File outputPath, List<UniversalPage> concepts1, List<UniversalPage> concepts2) throws IOException, DaoException, WikiBrainException {

        this.output = new CSVWriter(new FileWriter(outputPath), ',');
        writeHeader();
        if(concepts1.size() == 0 || concepts2.size() == 0)
            LOG.warn("No concept has been retrieved");
        int counter = 0;
        int total_size = concepts1.size() * concepts2.size();

        for(UniversalPage c1: concepts1){
            for(UniversalPage c2: concepts2){
                counter ++;
                if(counter % 1000 == 0)
                    LOG.info(String.format("Evaluating %d out of %d pairs", counter, total_size));
                if(c1.equals(c2))
                    continue;
                try {
                    List<SRResult> results = new ArrayList<SRResult>();
                    for (Language lang : langs) {
                        SRMetric sr = metrics.get(lang);
                        results.add(sr.similarity(c1.getLocalId(lang), c2.getLocalId(lang), false));
                    }
                    writeRow(c1, c2, results);
                }
                catch (Exception e){
                    LOG.warn(String.format("Error evaluating between %s and %s", c1.getBestEnglishTitle(lpDao, true), c2.getBestEnglishTitle(lpDao, true)));
                }

            }
        }

        this.output.close();

    }




    private void writeHeader() throws IOException {
        String[] headerEntries = new String[5 + langs.size()];
        headerEntries[0] = "ITEM_NAME_1";
        headerEntries[1] = "ITEM_ID_1";
        headerEntries[2] = "ITEM_NAME_2";
        headerEntries[3] = "ITEM_ID_2";
        headerEntries[4] = "SPATIAL_DISTANCE";
        int counter = 0;
        for (Language lang : langs) {
            headerEntries[5 + counter] = lang.getLangCode() + "_SR";
            counter ++;
        }
        output.writeNext(headerEntries);
        output.flush();
    }

    private void writeRow(UniversalPage c1, UniversalPage c2, List<SRResult> results) throws WikiBrainException, IOException {
        double km;
        Point p1 = locations.get(c1.getUnivId()).getCentroid();
        Point p2 = locations.get(c2.getUnivId()).getCentroid();

        GeodeticCalculator geoCalc = new GeodeticCalculator();
        geoCalc.setStartingGeographicPoint(p1.getX(), p1.getY());
        geoCalc.setDestinationGeographicPoint(p2.getX(), p2.getY());
        km = geoCalc.getOrthodromicDistance() / 1000;

        Title t1 = c1.getBestEnglishTitle(lpDao, true);
        Title t2 = c2.getBestEnglishTitle(lpDao, true);

        String[] rowEntries = new String[5 + langs.size()];
        rowEntries[0] = t1.getCanonicalTitle();
        rowEntries[1] = String.valueOf(c1.getUnivId());
        rowEntries[2] = t2.getCanonicalTitle();
        rowEntries[3] = String.valueOf(c2.getUnivId());
        rowEntries[4] = String.valueOf(km);
        int counter = 0;
        for (SRResult result : results) {
            rowEntries[5 + counter] = String.valueOf(result.getScore());
            counter ++;
        }
        output.writeNext(rowEntries);
        output.flush();
    }

}
