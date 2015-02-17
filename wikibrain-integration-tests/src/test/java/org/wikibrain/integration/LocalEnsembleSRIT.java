package org.wikibrain.integration;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.sr.Explanation;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.dataset.Dataset;
import org.wikibrain.sr.dataset.DatasetDao;
import org.wikibrain.sr.utils.ExplanationFormatter;

import java.io.IOException;
import java.sql.SQLException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Shilad Sen
 */
public class LocalEnsembleSRIT {
    private static final Language SIMPLE = Language.getByLangCode("simple");

    // Set this to true
    private static final Boolean SKIP_RESTORE = true;

    @BeforeClass
    public static void prepareDump() throws ConfigurationException, IOException, SQLException {
        if (SKIP_RESTORE) {
            return;
        }
        TestDB db = TestUtils.getTestDb();
        db.restoreLucene();
    }
    @Ignore
    @Test
    public void testESAAccuracy() throws Exception {
        testAccuracy("ESA", 0.50, 0.58, 0);
        testExplain("ESA", "President", "Obama");
    }
    @Ignore
    @Test
    public void testMilneWittenAccuracy() throws Exception {
        testAccuracy("milnewitten", 0.35, 0.37, 0);
        testExplain("milnewitten", "President", "Obama");
    }

    public void testAccuracy(String srName, double minPearson, double minSpearman, int maxNoPred) throws ConfigurationException, DaoException {
        Env env = TestUtils.getEnv();
        SRMetric sr = env.getConfigurator().get(SRMetric.class, srName, "language", "simple");
        DatasetDao datasetDao = new DatasetDao();
        Dataset ds = datasetDao.get(SIMPLE, "wordsim353.txt");

        /*
        CrossValidation cv = new CrossValidation();

        List<Dataset> allTrain = new ArrayList<Dataset>();
        List<Dataset> allTest = new ArrayList<Dataset>();
        CrossValidation.makeFolds(ds.split(7), allTrain, allTest);
        for (int i = 0; i < allTrain.size(); i++) {
            sr.trainDefaultSimilarity(allTrain.get(i));
            sr.trainSimilarity(allTrain.get(i));
            cv.evaluate(sr, allTest.get(i));
        }
        System.out.println("results for " + srName);
        System.out.println("\tpearson: " + cv.getPearson());
        System.out.println("\tspearman: " + cv.getSpearman());
        System.out.println("\tmissing: " + cv.getMissing());
        System.out.println("\tfailed: " + cv.getFailed());
        assertTrue(cv.getPearson() >= minPearson);
        assertTrue(cv.getSpearman() >= minSpearman);
        assertTrue(cv.getMissing() + cv.getFailed() <= maxNoPred);
        */
    }

    public void testExplain(String srName, String phrase1, String phrase2) throws ConfigurationException, DaoException {
        Env env = TestUtils.getEnv();
        DatasetDao datasetDao = new DatasetDao();
        Dataset ds = datasetDao.get(SIMPLE, "wordsim353.txt");
        SRMetric sr = env.getConfigurator().get(SRMetric.class, srName, "language", "simple");
        sr.trainSimilarity(ds);
        ExplanationFormatter formatter = env.getConfigurator().get(ExplanationFormatter.class);
        SRResult result = sr.similarity(phrase1, phrase2, true);
        System.out.println(srName + " explanation for " + phrase1 + ", " + phrase2 + " is:");
        assertNotNull(result.getExplanations());
        for (Explanation ex : result.getExplanations()) {
            System.out.println("\t" + formatter.formatExplanation(ex));
        }
    }
}
