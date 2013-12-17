package org.wikapidia.sr.evaluation;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.sr.LocalSRMetric;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.dataset.Dataset;
import org.wikapidia.sr.utils.KnownSim;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @see Evaluator
 * @author Shilad Sen
 */
public class SimilarityEvaluator extends Evaluator<SimilarityEvaluationLog> {
    private static final Logger LOG = Logger.getLogger(SimilarityEvaluator.class.getName());

    public SimilarityEvaluator(File outputDir) {
        super(outputDir, "local-similarity");
    }

    /**
     * Adds a crossfold validation of a particular dataset.
     * The group of the split is set to the name of the dataset.
     * @param ds
     * @param numFolds
     */
    @Override
    public void addCrossfolds(Dataset ds, int numFolds) {
        List<Dataset> folds = ds.split(numFolds);
        for (int i = 0; i < folds.size(); i++) {
            Dataset test = folds.get(i);
            List<Dataset> trains = new ArrayList<Dataset>(folds);
            trains.remove(i);
            addSplit(new Split(ds.getName() + "-fold-" + i, ds.getName(), new Dataset(trains), test));
        }
    }

    @Override
    public SimilarityEvaluationLog createResults(File path) throws IOException {
        return new SimilarityEvaluationLog(path);
    }

    @Override
    public List<String> getSummaryFields() {
        return Arrays.asList(
                "date",
                "runNumber",
                "lang",
                "metricName",
                "dataset",
                "successful",
                "missing",
                "failed",
                "pearsons",
                "spearmans",
                "resolvePhrases",
                "metricConfig",
                "disambigConfig"
        );
    }

    @Override
    protected SimilarityEvaluationLog evaluateSplit(LocalSRFactory factory, Split split, File log, File err, Map<String, String> config) throws DaoException, IOException {
        LocalSRMetric metric = factory.create();
        metric.trainSimilarity(split.getTrain());
        SimilarityEvaluationLog splitEval = new SimilarityEvaluationLog(config, log);
        BufferedWriter errFile = new BufferedWriter(new FileWriter(err));
        for (KnownSim ks : split.getTest().getData()) {
            try {
                SRResult result;
                if (shouldResolvePhrases()) {
                    LocalPage p1 = new LocalId(ks.language, ks.wpId1).asLocalPage();
                    LocalPage p2 = new LocalId(ks.language, ks.wpId2).asLocalPage();
                    result = metric.similarity(p1, p2, false);
                } else {
                    result = metric.similarity(ks.phrase1, ks.phrase2, ks.language, false);
                }
                splitEval.record(ks, result);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Similarity of " + ks + " failed. Logging error to " + err);
                splitEval.recordFailed(ks);
                errFile.write("KnownSim failed: " + ks + "\n");
                errFile.write("\t" + e.getMessage() + "\n");
                for (String frame : ExceptionUtils.getStackFrames(e)) {
                    errFile.write("\t" + frame + "\n");
                }
                errFile.write("\n");
                errFile.flush();
            }
        }
        IOUtils.closeQuietly(splitEval);
        IOUtils.closeQuietly(errFile);
        return splitEval;
    }
}
