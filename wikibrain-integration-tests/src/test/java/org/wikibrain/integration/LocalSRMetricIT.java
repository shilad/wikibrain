//package org.wikibrain.integration;
//
//import org.junit.BeforeClass;
//import org.junit.Test;
//import org.wikibrain.conf.ConfigurationException;
//import org.wikibrain.core.cmd.Env;
//import org.wikibrain.core.dao.DaoException;
//import org.wikibrain.core.lang.Language;
//import org.wikibrain.core.lang.LanguageSet;
//import org.wikibrain.sr.Explanation;
//import org.wikibrain.sr.MonolingualSRMetric;
//import org.wikibrain.sr.SRResult;
//import org.wikibrain.sr.dataset.Dataset;
//import org.wikibrain.sr.dataset.DatasetDao;
//import org.wikibrain.sr.ensemble.EnsembleMetric;
//import org.wikibrain.sr.utils.ExplanationFormatter;
//
//import java.io.File;
//import java.io.IOException;
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.junit.Assert.assertNotNull;
//import static org.junit.Assert.assertTrue;
//
///**
// * @author Shilad Sen
// */
//public class LocalSRMetricIT {
//    private static final Language SIMPLE = Language.getByLangCode("simple");
//
//    // Set this to true
//    private static final Boolean SKIP_RESTORE = true;
//
//    @BeforeClass
//    public static void prepareDump() throws ConfigurationException, IOException, SQLException {
//        if (SKIP_RESTORE) {
//            return;
//        }
//        TestDB db = TestUtils.getTestDb();
//        db.restoreLucene();
//    }
//
//    @Test
//    public void testESAAccuracy() throws Exception {
//        testAccuracy("ESA", 0.50, 0.58, 0);
//        testExplain("ESA", "President", "Obama");
//    }
//
//    @Test
//    public void testMilneWittenAccuracy() throws Exception {
//        testAccuracy("milnewitten", 0.35, 0.37, 0);
//        testExplain("milnewitten", "President", "Obama");
//    }
//
//    @Test
//    public void testEnsembleAccuracy() throws Exception {
//        testAccuracy("ensemble", 0.5, 6, 0);
////        testExplain("ensemble", "President", "Obama");
//    }
//
//    @Test
//    public void testMostSimilarCosine() throws Exception {
//        LanguageSet lset = new LanguageSet(SIMPLE);
//        Env env = TestUtils.getEnv();
//        String path = env.getConfiguration().get().getString("sr.metric.path");
////        EnsembleMetric sr = (EnsembleMetric) env.getConfigurator().get(LocalSRMetric.class, "ensemble");
////        for (LocalSRMetric sr2 : sr.getMetrics()) {
////            sr2.writeMostSimilarCache(path, lset, 1000);
////        }
//        Dataset ds = getDataset(env, "wordsim353.txt");
////        sr.trainMostSimilar(ds, 500, null);
////        sr.writeMostSimilarCache(path, lset, 500);
////        LocalSRMetric cosSr = env.getConfigurator().get(LocalSRMetric.class, "ensemble");
//
//
//        testAccuracy("mostsimilarcosine", 0.5, 6, 0);
//    }
//
//    public void testAccuracy(String srName, double minPearson, double minSpearman, int maxNoPred) throws ConfigurationException, DaoException {
//        Env env = TestUtils.getEnv();
//        LocalSRMetric sr = env.getConfigurator().get(LocalSRMetric.class, srName);
//        Dataset ds = getDataset(env, "wordsim353.txt");
//        CrossValidation cv = new CrossValidation();
//
//        List<Dataset> allTrain = new ArrayList<Dataset>();
//        List<Dataset> allTest = new ArrayList<Dataset>();
//        CrossValidation.makeFolds(ds.split(7), allTrain, allTest);
//        for (int i = 0; i < allTrain.size(); i++) {
//            sr.trainDefaultSimilarity(allTrain.get(i));
//            sr.trainSimilarity(allTrain.get(i));
//            cv.evaluate(sr, allTest.get(i));
//        }
//        System.out.println("results for " + srName);
//        System.out.println("\tpearson: " + cv.getPearson());
//        System.out.println("\tspearman: " + cv.getSpearman());
//        System.out.println("\tmissing: " + cv.getMissing());
//        System.out.println("\tfailed: " + cv.getFailed());
//        assertTrue(cv.getPearson() >= minPearson);
//        assertTrue(cv.getSpearman() >= minSpearman);
//        assertTrue(cv.getMissing() + cv.getFailed() <= maxNoPred);
//    }
//
//    public Dataset getDataset(Env env, String name) throws DaoException {
//        DatasetDao datasetDao = new DatasetDao();
//        String datasetPath = env.getConfiguration().get().getString("sr.dataset.path");
//        datasetPath = datasetPath.replace("integration-tests/", "");
//        return datasetDao.read(SIMPLE, new File(datasetPath, name).toString());
//        MonolingualSRMetric sr = env.getConfigurator().get(MonolingualSRMetric.class, srName, "language", "simple");
//        DatasetDao datasetDao = new DatasetDao();
//        Dataset ds = datasetDao.get(SIMPLE, "wordsim353.txt");
////        CrossValidation cv = new CrossValidation();
////
////        List<Dataset> allTrain = new ArrayList<Dataset>();
////        List<Dataset> allTest = new ArrayList<Dataset>();
////        CrossValidation.makeFolds(ds.split(7), allTrain, allTest);
////        for (int i = 0; i < allTrain.size(); i++) {
////            sr.trainDefaultSimilarity(allTrain.get(i));
////            sr.trainSimilarity(allTrain.get(i));
////            cv.evaluate(sr, allTest.get(i));
////        }
////        System.out.println("results for " + srName);
////        System.out.println("\tpearson: " + cv.getPearson());
////        System.out.println("\tspearman: " + cv.getSpearman());
////        System.out.println("\tmissing: " + cv.getMissing());
////        System.out.println("\tfailed: " + cv.getFailed());
////        assertTrue(cv.getPearson() >= minPearson);
////        assertTrue(cv.getSpearman() >= minSpearman);
////        assertTrue(cv.getMissing() + cv.getFailed() <= maxNoPred);
//    }
//
//    public void testExplain(String srName, String phrase1, String phrase2) throws ConfigurationException, DaoException {
//        Env env = TestUtils.getEnv();
//        DatasetDao datasetDao = new DatasetDao();
//        String datasetPath = env.getConfiguration().get().getString("sr.dataset.path");
//        datasetPath = datasetPath.replace("integration-tests/", "");
//        Dataset ds = datasetDao.read(SIMPLE, new File(datasetPath, "wordsim353.txt").toString());
//        LocalSRMetric sr = env.getConfigurator().get(LocalSRMetric.class, srName);
//        sr.trainDefaultSimilarity(ds);
//        sr.trainSimilarity(ds);
//        ExplanationFormatter formatter = env.getConfigurator().get(ExplanationFormatter.class);
//        SRResult result = sr.similarity(phrase1, phrase2, SIMPLE, true);
//        System.out.println(srName + " explanation for " + phrase1 + ", " + phrase2 + " is:");
//        assertNotNull(result.getExplanations());
//        for (Explanation ex : result.getExplanations()) {
//            System.out.println("\t" + formatter.formatExplanation(ex));
//        }
//    }
//}
