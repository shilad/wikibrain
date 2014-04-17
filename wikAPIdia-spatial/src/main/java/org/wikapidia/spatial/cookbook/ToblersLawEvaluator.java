package org.wikapidia.spatial.cookbook;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.geotools.referencing.GeodeticCalculator;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.UniversalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.Title;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.spatial.core.dao.SpatialDataDao;
import org.wikapidia.sr.MonolingualSRMetric;
import org.wikapidia.sr.SRResult;
import org.wikapidia.utils.ParallelForEach;
import org.wikapidia.utils.Procedure;
import org.wikapidia.utils.WpIOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class ToblersLawEvaluator {

    private static int NUM_SAMPLES = 10000;
    private static int WIKIDATA_CONCEPTS = 1;
    private static LanguageSet languageSet = new LanguageSet("simple");

    private static final Logger LOG = Logger.getLogger(ToblersLawEvaluator.class.getName());

    private Random random = new Random();

    private final SpatialDataDao sdDao;
    private final LocalPageDao lpDao;
    private final UniversalPageDao upDao;
    private final List<Language> langs;
    private final Map<Language, MonolingualSRMetric> metrics;

    private final List<UniversalPage> concepts = new ArrayList<UniversalPage>();
    private final Map<UniversalPage, Point> locations = new HashMap<UniversalPage, Point>();
    private final Env env;
    private BufferedWriter output;


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
        this.metrics = new HashMap<Language, MonolingualSRMetric>();
        for(Language lang : langs){
            MonolingualSRMetric m = c.get(MonolingualSRMetric.class, "ensemble", "language", lang.getLangCode());
            metrics.put(lang, m);
        }
    }

    public void retrieveLocations() throws DaoException {
        // Get all known concept geometries
        Map<Integer, Geometry> geometries = sdDao.getAllGeometries("wikidata", "earth");
        LOG.log(Level.INFO, String.format("Get %d geometries, now building id-name mapping", geometries.size()));

        // Build up list of concepts in all languages
        for (Integer conceptId : geometries.keySet()){
            UniversalPage concept = upDao.getById(conceptId, WIKIDATA_CONCEPTS);
            if (concept != null && concept.hasAllLanguages(languageSet)) {
                concepts.add(concept);
                Geometry g1 = sdDao.getGeometry(concept.getUnivId(), "wikidata", "earth");
                locations.put(concept, g1.getCentroid());
                if (concepts.size() % 1000 == 0) {
                    LOG.info(String.format("Loaded %d geometries with articles in %s...", concepts.size(), languageSet));
                }
            }
        }
        LOG.info(String.format("Found %d geometries with articles in %s", concepts.size(), languageSet));
    }

    public void evaluate(File outputPath) throws IOException {
        this.output = WpIOUtils.openWriter(outputPath);
        writeHeader();

        ParallelForEach.range(0, NUM_SAMPLES, new Procedure<Integer>() {
            @Override
            public void call(Integer i) throws Exception {
                evaluateOneSample();
            }
        });

        this.output.close();
    }

    public void evaluateOneSample() throws DaoException, WikapidiaException, IOException {
        UniversalPage c1 = concepts.get(random.nextInt(concepts.size()));
        UniversalPage c2 = concepts.get(random.nextInt(concepts.size()));

        List<SRResult> results = new ArrayList<SRResult>();
        for (Language lang : langs) {
            MonolingualSRMetric sr = metrics.get(lang);
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

    private void writeRow(UniversalPage c1, UniversalPage c2, List<SRResult> results) throws WikapidiaException, IOException {
        double km;
        Point p1 = locations.get(c1).getCentroid();
        Point p2 = locations.get(c2).getCentroid();

        GeodeticCalculator geoCalc = new GeodeticCalculator();
        geoCalc.setStartingGeographicPoint(p1.getX(), p1.getY());
        geoCalc.setDestinationGeographicPoint(p2.getX(), p2.getY());
        km = geoCalc.getOrthodromicDistance() / 1000;

        Title t1 = c1.getBestEnglishTitle(lpDao, true);
        Title t2 = c2.getBestEnglishTitle(lpDao, true);
        output.write(t1.getCanonicalTitle() +
                "," + c1.getUnivId() +
                "," + t2.getCanonicalTitle() +
                "," + c2.getUnivId() +
                "," + km
        );
        for (SRResult result : results) {
            output.write("," + result.getScore());
        }
        output.write("\n");
    }


    public static void main(String[] args) throws Exception {
        Env env = EnvBuilder.envFromArgs(args);
        ToblersLawEvaluator eval = new ToblersLawEvaluator(env, languageSet);
        eval.retrieveLocations();
        eval.evaluate(new File("toblers_eval.csv"));
    }
}
