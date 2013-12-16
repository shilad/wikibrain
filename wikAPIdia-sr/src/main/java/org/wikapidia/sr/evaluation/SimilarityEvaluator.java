package org.wikapidia.sr.evaluation;

import edu.emory.mathcs.backport.java.util.Collections;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.sr.LocalSRMetric;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.dataset.Dataset;
import org.wikapidia.sr.utils.KnownSim;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @see Evaluator
 * @author Shilad Sen
 */
public class SimilarityEvaluator extends Evaluator<SimilarityEvaluationResults> {
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
    public SimilarityEvaluationResults createResults(File path) throws IOException {
        return new SimilarityEvaluationResults(path);
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
                "metricConfig",
                "disambigConfig"
        );
    }

    @Override
    protected SimilarityEvaluationResults evaluateSplit(LocalSRFactory factory, Split split, File log, File err, Map<String, String> config) throws DaoException, IOException {
        LocalSRMetric metric = factory.create();
        metric.trainSimilarity(split.getTrain());
        SimilarityEvaluationResults splitEval = new SimilarityEvaluationResults(config, log);
        BufferedWriter errFile = new BufferedWriter(new FileWriter(err));
        for (KnownSim ks : split.getTest().getData()) {
            try {
                SRResult result = metric.similarity(ks.phrase1, ks.phrase2, ks.language, false);
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
