package org.wikibrain.spatial.cookbook;

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
import org.wikibrain.core.model.Title;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.spatial.dao.SpatialDataDao;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpIOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class SimpleToblersLawEvaluator {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleToblersLawEvaluator.class);
    private static int NUM_SAMPLES = 1000000;

    private Random random = new Random();

    private final SpatialDataDao sdDao;
    private final LocalPageDao lpDao;
    private final UniversalPageDao upDao;
    private final List<Language> langs;
    private final Map<Language, SRMetric> metrics;

    private final List<UniversalPage> concepts = new ArrayList<UniversalPage>();
    private final Map<UniversalPage, Point> locations = new HashMap<UniversalPage, Point>();
    private final Env env;
    private BufferedWriter output;


    public SimpleToblersLawEvaluator(Env env) throws ConfigurationException {
        this.env = env;
        this.langs = new ArrayList<Language>(env.getLanguages().getLanguages());

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

    public void retrieveLocations() throws DaoException {
        // Get all known concept geometries
        Map<Integer, Geometry> geometries = sdDao.getAllGeometriesInLayer("wikidata", "earth");
        LOG.info(String.format("Get %d geometries, now building id-name mapping", geometries.size()));

        // Build up list of concepts in all languages
        for (Integer conceptId : geometries.keySet()){
            UniversalPage concept = upDao.getById(conceptId);
            if (concept != null && concept.hasAllLanguages(env.getLanguages())) {
                concepts.add(concept);
                locations.put(concept, geometries.get(conceptId).getCentroid());
                if (concepts.size() % 1000 == 0) {
                    LOG.info(String.format("Loaded %d geometries with articles in %s...", concepts.size(), env.getLanguages()));
                }
            }
        }
        LOG.info(String.format("Found %d geometries with articles in %s", concepts.size(), env.getLanguages()));
    }

    public void evaluate(File outputPath, int numSamples) throws IOException {
        this.output = WpIOUtils.openWriter(outputPath);
        writeHeader();

        ParallelForEach.range(0, numSamples, new Procedure<Integer>() {
            @Override
            public void call(Integer i) throws Exception {
                evaluateOneSample();
            }
        });

        this.output.close();
    }

    public void evaluateOneSample() throws DaoException, WikiBrainException, IOException {
        UniversalPage c1 = concepts.get(random.nextInt(concepts.size()));
        UniversalPage c2 = concepts.get(random.nextInt(concepts.size()));

        List<SRResult> results = new ArrayList<SRResult>();
        for (Language lang : langs) {
            SRMetric sr = metrics.get(lang);
            results.add(sr.similarity(c1.getLocalId(lang), c2.getLocalId(lang), false));
        }

        writeRow(c1, c2, results);
    }

    private void writeHeader() throws IOException {
        output.write("ITEM_NAME_1");
        output.write("\tITEM_ID_1");
        output.write("\tITEM_NAME_2");
        output.write("\tITEM_ID_2");
        output.write("\tSPATIAL_DISTANCE");
        for (Language lang : langs) {
            output.write("\t" + lang.getLangCode() + "_SR");
        }
    }

    private void writeRow(UniversalPage c1, UniversalPage c2, List<SRResult> results) throws WikiBrainException, IOException {
        Point p1 = locations.get(c1).getCentroid();
        Point p2 = locations.get(c2).getCentroid();

        GeodeticCalculator geoCalc = new GeodeticCalculator();
        geoCalc.setStartingGeographicPoint(p1.getX(), p1.getY());
        geoCalc.setDestinationGeographicPoint(p2.getX(), p2.getY());
        double km = geoCalc.getOrthodromicDistance() / 1000;

        Title t1 = c1.getBestEnglishTitle(lpDao, true);
        Title t2 = c2.getBestEnglishTitle(lpDao, true);
        synchronized (output) {
            output.write(t1.getCanonicalTitle() +
                    "\t" + c1.getUnivId() +
                    "\t" + t2.getCanonicalTitle() +
                    "\t" + c2.getUnivId() +
                    "\t" + km
            );
            for (SRResult result : results) {
                output.write("\t" + result.getScore());
            }
            output.write("\n");
        }
    }


    public static void main(String[] args) throws Exception {
        Env env = EnvBuilder.envFromArgs(args);
        SimpleToblersLawEvaluator eval = new SimpleToblersLawEvaluator(env);
        eval.retrieveLocations();
        eval.evaluate(new File("toblers_eval.tsv"), NUM_SAMPLES);
    }
}
