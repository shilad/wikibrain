package org.wikibrain.sr.evaluation;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.junit.Test;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.sr.dataset.Dataset;
import org.wikibrain.sr.dataset.DatasetDao;
import org.wikibrain.utils.WpIOUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Shilad Sen
 */
public class TestEvaluator {

    @Test
    public void testSimilarity() throws IOException, DaoException, ConfigurationException, WikiBrainException {
        DatasetDao dsDao = new DatasetDao();
        Language simple = Language.getByLangCode("simple");
        File file = WpIOUtils.createTempDirectory("evaluator");

        SimilarityEvaluator evaluator = new SimilarityEvaluator(file);
        evaluator.setWriteToStdout(false);
        evaluator.addCrossfolds(dsDao.get(simple, "radinsky.txt"), 7);
        evaluator.addCrossfolds(dsDao.get(simple, "atlasify240.txt"), 7);

        TestLocalSR.Factory factory = new TestLocalSR.Factory();
        SimilarityEvaluationLog eval = evaluator.evaluate(factory);

        List<String> lines = FileUtils.readLines(FileUtils.getFile(file, "local-similarity", "summary.tsv"));
        assertEquals(lines.size(), 4);
        assertFalse(StringUtils.join(lines).contains("null"));
        assertTrue(StringUtils.join(lines).contains("thisIsTheMetric"));
        assertTrue(StringUtils.join(lines).contains("thisIsTheDisambiguator"));

        assertEquals(14, eval.getChildFiles().size());

        TDoubleList actual = new TDoubleArrayList();
        TDoubleList estimated = new TDoubleArrayList();

        int missing = 0;
        int failed = 0;
        int successful = 0;
        int total = 0;

        assertEquals(14, factory.metrics.size());
        assertEquals(14, evaluator.getSplits().size());
        for (int i = 0; i < factory.metrics.size(); i++) {
            Dataset test = evaluator.getSplits().get(i).getTest();
            TestLocalSR testSr = factory.metrics.get(i);
            actual.addAll(testSr.getActual(test.getData()));
            estimated.addAll(testSr.getEstimated(test.getData()));
            missing += testSr.getMissing();
            failed += testSr.getFailed();
            successful += testSr.getSuccessful();
            total += testSr.getTotal();
        }
        assertEquals("thisIsTheDisambiguator", eval.getSummaryAsMap().get("disambigConfig"));
        assertEquals("thisIsTheMetric", eval.getSummaryAsMap().get("metricConfig"));
        assertEquals(missing, eval.getMissing());
        assertEquals(failed, eval.getFailed());
        assertEquals(total, eval.getTotal());
        assertEquals(successful, eval.getSuccessful());
        assertEquals(actual, eval.getActual());
        assertEquals(estimated, eval.getEstimates());
        assertEquals(
                new PearsonsCorrelation().correlation(actual.toArray(), estimated.toArray()),
                eval.getPearsonsCorrelation(),
                0.00001
            );
        assertEquals(
                new SpearmansCorrelation().correlation(actual.toArray(), estimated.toArray()),
                eval.getSpearmansCorrelation(),
                0.00001
        );
    }

    @Test
    public void testRunNumber() throws IOException, DaoException, ConfigurationException, WikiBrainException {
        DatasetDao dsDao = new DatasetDao();
        Language simple = Language.getByLangCode("simple");
        File file = WpIOUtils.createTempDirectory("evaluator");

        SimilarityEvaluator simEvaluator = new SimilarityEvaluator(file);
        simEvaluator.setWriteToStdout(false);
        simEvaluator.addCrossfolds(dsDao.get(simple, "wordsim353.txt"), 7);
        simEvaluator.addCrossfolds(dsDao.get(simple, "atlasify240.txt"), 7);

        MostSimilarEvaluator mostSimEvaluator = new MostSimilarEvaluator(file);
        mostSimEvaluator.setWriteToStdout(false);
        mostSimEvaluator.addCrossfolds(dsDao.get(simple, "wordsim353.txt"), 7);
        mostSimEvaluator.addCrossfolds(dsDao.get(simple, "atlasify240.txt"), 7);

        TestLocalSR.Factory factory = new TestLocalSR.Factory();
        BaseEvaluationLog eval = simEvaluator.evaluate(factory);
        assertTrue(eval.getChildFiles().get(0).toString().contains("0-"));
        eval = simEvaluator.evaluate(factory);
        assertTrue(eval.getChildFiles().get(0).toString().contains("1-"));
        eval = mostSimEvaluator.evaluate(factory);
        assertTrue(eval.getChildFiles().get(0).toString().contains("2-"));
        eval = mostSimEvaluator.evaluate(factory);
        System.out.println(eval.getChildFiles().get(0));
        assertTrue(eval.getChildFiles().get(0).toString().contains("3-"));
        eval = simEvaluator.evaluate(factory);
        assertTrue(eval.getChildFiles().get(0).toString().contains("4-"));
    }
}
