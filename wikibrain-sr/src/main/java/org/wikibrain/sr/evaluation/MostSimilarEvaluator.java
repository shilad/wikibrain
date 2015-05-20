package org.wikibrain.sr.evaluation;

import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.dataset.Dataset;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpThreadUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @see Evaluator
 *
 * @author Shilad Sen
 */
public class MostSimilarEvaluator extends Evaluator<MostSimilarEvaluationLog> {
    private static final Logger LOG = LoggerFactory.getLogger(MostSimilarEvaluator.class);

    private boolean buildCosimilarityMatrix = false;

    // These arguments will be passed to calls to mostSimilar()
    private int numMostSimilarResults = 2000;
    private TIntHashSet mostSimilarIds = null;

    // These arguments are passed to the MostSimilarEvaluationLog
    private double relevanceThreshold = 0.6;
    private int precisionRecallRanks[] = new int[] {1, 5, 10, 20, 50, 100, 500, 1000 };


    public MostSimilarEvaluator(File outputDir) {
        super(outputDir, "local-mostSimilar");
    }


    /**
     * Adds a crossfold validation of a particular dataset.
     * The group of the split is set to the name of the dataset.
     * @param ds
     * @param numFolds
     */
    @Override
    public void addCrossfolds(Dataset ds, int numFolds) {
        MostSimilarDataset msd = new MostSimilarDataset(ds);
        List<Dataset> folds = msd.splitIntoDatasets(numFolds);
        for (int i = 0; i < folds.size(); i++) {
            Dataset test = folds.get(i);
            List<Dataset> trains = new ArrayList<Dataset>(folds);
            trains.remove(i);
            addSplit(new Split(ds.getName() + "-fold-" + i, ds.getName(), new Dataset(trains), test));
        }
    }

    @Override
    public MostSimilarEvaluationLog createResults(File path) throws IOException {
        MostSimilarEvaluationLog results = new MostSimilarEvaluationLog(path);
        results.setPrecisionRecallRanks(precisionRecallRanks);
        results.setRelevanceThreshold(relevanceThreshold);
        return results;
    }

    @Override
    public List<String> getSummaryFields() {
        List<String> fields = new ArrayList<String>(Arrays.asList(
                "date",
                "runNumber",
                "lang",
                "metricName",
                "dataset",
                "successful",
                "missing",
                "failed",
                "resolvePhrases",
                "pearsons",
                "spearmans",
                "ndgc",
                "penalizedNdgc"
        ));
        for (int i : precisionRecallRanks) {
            fields.add("num-" + i);
        }
        for (int i : precisionRecallRanks) {
            fields.add("mean-" + i);
        }
        for (int i : precisionRecallRanks) {
            fields.add("precision-" + i);
        }
        for (int i : precisionRecallRanks) {
            fields.add("recall-" + i);
        }
        fields.add("metricConfig");
        fields.add("disambigConfig");
        return fields;
    }


    /**
     * Evaluates a particular split for mostSimilar()
     * @param factory
     * @param split
     * @param log
     * @param err
     * @param config
     * @return
     * @throws java.io.IOException
     * @throws org.wikibrain.core.dao.DaoException
     */
    @Override
    protected MostSimilarEvaluationLog evaluateSplit(MonolingualSRFactory factory, Split split, File log, final File err, Map<String, String> config) throws IOException, DaoException, WikiBrainException {
        final SRMetric metric = factory.create();
        metric.trainMostSimilar(split.getTrain(), numMostSimilarResults, mostSimilarIds);
        final MostSimilarEvaluationLog splitEval = new MostSimilarEvaluationLog(config, log);
        final BufferedWriter errFile = new BufferedWriter(new FileWriter(err));
        final MostSimilarDataset msd = new MostSimilarDataset(split.getTest());
        ParallelForEach.iterate(msd.getPhrases().iterator(), WpThreadUtils.getMaxThreads(), 1000,  new Procedure<String>() {
            @Override
            public void call(String phrase) throws Exception {

                KnownMostSim kms = msd.getSimilarities(phrase);
                try {
                    SRResultList result;
                    if (shouldResolvePhrases()) {
                        result = metric.mostSimilar(kms.getPageId(), numMostSimilarResults, mostSimilarIds);
                    } else {
                        result = metric.mostSimilar(phrase, numMostSimilarResults, mostSimilarIds);
                    }
                    splitEval.record(kms, result);
                } catch (Exception e) {
                    LOG.warn("Similarity of " + kms.getPhrase() + ", id=" + kms.getPageId() + " failed. Logging error to " + err);
                    splitEval.recordFailed(kms);
                    synchronized (errFile) {
                        errFile.write("KnownSim failed: " + phrase + "\n");
                        errFile.write("\t" + e.getMessage() + "\n");
                        for (String frame : ExceptionUtils.getStackFrames(e)) {
                            errFile.write("\t" + frame + "\n");
                        }
                        errFile.write("\n");
                        errFile.flush();
                    }
                }
            }
        }, 100);

        IOUtils.closeQuietly(splitEval);
        IOUtils.closeQuietly(errFile);
        return splitEval;
    }

    public void setMostSimilarIds(TIntHashSet mostSimilarIds) {
        this.mostSimilarIds = mostSimilarIds;
    }

    public void setNumMostSimilarResults(int numMostSimilarResults) {
        this.numMostSimilarResults = numMostSimilarResults;
    }

    public void setPrecisionRecallRanks(int[] precisionRecallRanks) {
        this.precisionRecallRanks = precisionRecallRanks;
    }

    public void setBuildCosimilarityMatrix(boolean buildCosimilarityMatrix) {
        this.buildCosimilarityMatrix = buildCosimilarityMatrix;
    }
}
