package org.wikapidia.sr.evaluation;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.junit.Test;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.sr.dataset.Dataset;
import org.wikapidia.sr.dataset.DatasetDao;
import org.wikapidia.utils.WpIOUtils;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Shilad Sen
 */
public class TestEvaluator {

    @Test
    public void testSimple() throws IOException, DaoException, ConfigurationException {
        DatasetDao dsDao = new DatasetDao();
        Language simple = Language.getByLangCode("simple");
        File file = WpIOUtils.createTempDirectory("evaluator");
        Evaluator evaluator = new Evaluator(file);

        evaluator.addCrossfolds(dsDao.get(simple, "wordsim353.txt"), 7);
        evaluator.addCrossfolds(dsDao.get(simple, "atlasify240.txt"), 7);

        TestLocalSR.Factory factory = new TestLocalSR.Factory();
        SimilarityEvaluation eval = evaluator.evaluateSimilarity(factory);
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
        assertEquals(total, eval.getTotal());
        assertEquals(missing, eval.getMissing());
        assertEquals(failed, eval.getFailed());
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
}
