package org.wikibrain.sr.evaluation;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.junit.Test;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.dataset.Dataset;
import org.wikibrain.sr.dataset.DatasetDao;
import org.wikibrain.sr.utils.KnownSim;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * @author Shilad Sen
 */
public class TestSimilarityEvaluation {

    @Test
    public void testSimple() throws IOException, DaoException {
        TDoubleList actual = new TDoubleArrayList();
        TDoubleList estimated = new TDoubleArrayList();

        Random rand = new Random();
        Language simple = Language.getByLangCode("simple");
        Dataset ds = new DatasetDao().get(simple, "wordsim353.txt");
        File log = File.createTempFile("evaluation", "log");
        log.deleteOnExit();
        SimilarityEvaluationLog se = new SimilarityEvaluationLog(log);

        for (int i = 0; i < ds.getData().size(); i++) {
            KnownSim ks = ds.getData().get(i);
            if (i % 20 == 0) {
                se.recordFailed(ks);
            } else if (i % 20 == 1) {
                se.record(ks, new SRResult(Double.NaN));
            } else if (i % 20 == 2) {
                se.record(ks, new SRResult(Double.POSITIVE_INFINITY));
            } else {
                double v = rand.nextDouble();
                se.record(ks, new SRResult(v));
                actual.add(ks.similarity);
                estimated.add(v);
            }
        }
        assertEquals(353, se.getTotal());
        assertEquals(18, se.getFailed());
        assertEquals(36, se.getMissing());
        assertEquals(353-18-36, se.getSuccessful());
        assertEquals(se.getPearsonsCorrelation(), new PearsonsCorrelation().correlation(actual.toArray(), estimated.toArray()), 0.000001);
        assertEquals(se.getSpearmansCorrelation(), new SpearmansCorrelation().correlation(actual.toArray(), estimated.toArray()), 0.000001);

        IOUtils.closeQuietly(se);
        List<String> logLines = FileUtils.readLines(log);
        assertTrue(logLines.size() > 300);
    }
}
