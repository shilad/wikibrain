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
 *
 * @author Shilad Sen
 */
public class MostSimilarEvaluator extends Evaluator<MostSimilarEvaluationResults> {
    private static final Logger LOG = Logger.getLogger(MostSimilarEvaluator.class.getName());

    // These arguments will be passed to calls to mostSimilar()
    private int numMostSimilarResults = 500;
    private TIntHashSet mostSimilarIds = null;


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
    public MostSimilarEvaluationResults createResults(File path) throws IOException {
        return new MostSimilarEvaluationResults(path);
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


    /**
     * Evaluates a particular split for mostSimilar()
     * @param factory
     * @param split
     * @param log
     * @param err
     * @param config
     * @return
     * @throws java.io.IOException
     * @throws org.wikapidia.core.dao.DaoException
     */
    @Override
    protected MostSimilarEvaluationResults evaluateSplit(LocalSRFactory factory, Split split, File log, File err, Map<String, String> config) throws IOException, DaoException {
        LocalSRMetric metric = factory.create();
        metric.trainMostSimilar(split.getTrain(), numMostSimilarResults, mostSimilarIds);
        MostSimilarEvaluationResults splitEval = new MostSimilarEvaluationResults(config, log);
        BufferedWriter errFile = new BufferedWriter(new FileWriter(err));
        MostSimilarDataset msd = new MostSimilarDataset(split.getTest());
        for (String phrase : msd.getPhrases()) {
            KnownMostSim kms = msd.getSimilarities(phrase);
            try {
                SRResultList result = metric.mostSimilar(new LocalString(msd.getLanguage(), phrase), numMostSimilarResults, mostSimilarIds);
                splitEval.record(kms, result);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Similarity of " + phrase + " failed. Logging error to " + err);
                splitEval.recordFailed(kms);
                errFile.write("KnownSim failed: " + phrase + "\n");
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

    public void setMostSimilarIds(TIntHashSet mostSimilarIds) {
        this.mostSimilarIds = mostSimilarIds;
    }

    public void setNumMostSimilarResults(int numMostSimilarResults) {
        this.numMostSimilarResults = numMostSimilarResults;
    }
}
