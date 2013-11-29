package org.wikapidia.sr.evaluation;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.sr.LocalSRMetric;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.dataset.Dataset;
import org.wikapidia.sr.utils.KnownSim;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * An evaluator for SR metrics. Writes a directory structure of evaluation results like:
 *
 * baseDir/local/
 *      results.tsv                     Tab separated spreadsheet of sr metric results
 *      lang/split-group/run#-metric/
 *              summary.txt             Human-readable summary of metric results
 *              splitname1.summary      Human-readable summary of splitname1 within group
 *              splitname2.summary
 *              splitname3.summary
 *              splitname1.log          Log of results from splitname1 within group
 *              splitname2.log
 *              splitname3.log
 *              splitname1.err          Error logs for splitname1, within group
 *              splitname2.err
 *              splitname3.err
 *
 * @author Shilad Sen
 */
public class Evaluator {
    private static final Logger LOG = Logger.getLogger(Evaluator.class.getName());
    private final File outputDir;
    private boolean writeToStdout = true;

    private List<Split> splits = new ArrayList<Split>();

    public Evaluator(File outputDir) {
        this.outputDir = outputDir;
        ensureIsDirectory(outputDir);
        ensureIsDirectory(new File(outputDir, "local"));
        ensureIsDirectory(new File(outputDir, "universal"));
    }

    public void setWriteToStdout(boolean writeToStdout) {
        this.writeToStdout = writeToStdout;
    }

    /**
     * Adds a crossfold validation of a particular dataset.
     * The group of the split is set to the name of the dataset.
     * @param ds
     * @param numFolds
     */
    public void addCrossfolds(Dataset ds, int numFolds) {
        List<Dataset> folds = ds.split(numFolds);
        for (int i = 0; i < folds.size(); i++) {
            Dataset test = folds.get(i);
            List<Dataset> trains = new ArrayList<Dataset>(folds);
            trains.remove(i);
            splits.add(new Split(ds.getName() + "-fold-" + i, ds.getName(), new Dataset(trains), test));
        }
    }

    /**
     * Adds a single split.
     * @param split
     */
    public void addSplit(Split split) {
        this.splits.add(split);
    }

    private void ensureIsDirectory(File dirPath) {
        if (!dirPath.isDirectory()) {
            FileUtils.deleteQuietly(dirPath);
            dirPath.mkdirs();
        }
    }

    Pattern MATCH_RUN = Pattern.compile("^(\\d+)-.*");
    /**
     * TODO: make runs nums increase across all groups - not just the ones in the current splits.
     * @return The next unused run number across all splits and metrics.
     */
    private int getNextRunNumber() {
        int runNum = 0;
        for (Split split : splits) {
            for (String filename : getLocalDir(split).list()) {
                Matcher matcher = MATCH_RUN.matcher(filename);
                if (matcher.matches()) {
                    runNum = Integer.valueOf(matcher.group(1)) + 1;
                }
            }
        }
        return runNum;
    }

    private File getLocalDir(Split split) {
        return FileUtils.getFile(outputDir, "local", split.getTest().getLanguage().getLangCode(), split.getGroup());
    }

    private File getLocalDir(Split split, LocalSRMetric metric, int runNumber) {
        return getLocalDir(split, metric.getName(), runNumber);
    }

    private File getLocalDir(Split split, String metricName, int runNumber) {
        return new File(getLocalDir(split), runNumber + "-" + metricName);
    }

    /**
     * Evaluates a single SR metric. Returns a single SimilarityEvaluation across all
     * splits for the metric.
     *
     * @param factory
     * @return The overall evaluation across all splits for the metric.
     *
     * @throws DaoException
     * @throws IOException
     * @throws ConfigurationException
     */
    public synchronized SimilarityEvaluation evaluateSimilarity(LocalSRFactory factory) throws DaoException, IOException, ConfigurationException {
        SimilarityEvaluation overall = new SimilarityEvaluation();
        int runNumber = getNextRunNumber();

        Map<String, SimilarityEvaluation> groupEvals = new HashMap<String, SimilarityEvaluation>();
        Map<String, File> groupFiles = new HashMap<String, File>();
        String metricName = null;

        for (Split split : splits) {
            LocalSRMetric metric = factory.create();
            metricName = metric.getName();
            SimilarityEvaluation splitEval = evaluateSplit(split, metric, runNumber);
            overall.merge(splitEval);
            if (!groupEvals.containsKey(split.getGroup())) {
                groupEvals.put(split.getGroup(), new SimilarityEvaluation());
                groupFiles.put(split.getGroup(), getLocalDir(split, metric, runNumber));
            }
            groupEvals.get(split.getGroup()).merge(splitEval);
            IOUtils.closeQuietly(splitEval);
        }

        for (String group : groupEvals.keySet()) {
            SimilarityEvaluation geval = groupEvals.get(group);
            geval.summarize(groupFiles.get(group));
            maybeWriteToStdout("Split " + group + ", " + metricName + ", " + runNumber, geval);
            if (writeToStdout) geval.summarize();
        }
        maybeWriteToStdout("Overall for run " + runNumber, overall);

        return overall;
    }

    /**
     * Evaluates an sr metric against a single split and writes log, error, and summary files.
     *
     *
     * @param split
     * @param metric
     * @param runNumber
     * @return
     * @throws IOException
     * @throws DaoException
     */
    private SimilarityEvaluation evaluateSplit(Split split, LocalSRMetric metric, int runNumber) throws IOException, DaoException {
        File dir = getLocalDir(split, metric, runNumber);
        ensureIsDirectory(dir);
        File log = new File(dir, split.getName() + ".log");
        File err = new File(dir, split.getName() + ".err");
        File summary = new File(dir, split.getName() + ".summary");

        BufferedWriter errFile = new BufferedWriter(new FileWriter(err));

        metric.trainSimilarity(split.getTrain());
        SimilarityEvaluation splitEval = new SimilarityEvaluation(log);

        for (KnownSim ks : split.getTest().getData()) {
            try {
                SRResult result = metric.similarity(ks.phrase1, ks.phrase2, ks.language, false);
                splitEval.record(ks, result.getScore());
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Similarity of " + ks + " failed:", e);
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
        splitEval.summarize(summary);
        maybeWriteToStdout(
                "Split " + split.getGroup() + ", " + split.getName() + ", " + metric.getName() + ", " + runNumber,
                splitEval);
        return splitEval;
    }

    private void maybeWriteToStdout(String caption, SimilarityEvaluation eval) throws IOException {
        if (!writeToStdout) {
            return;
        }
        System.out.println("Similarity evaluation for " + caption);
        eval.summarize();
    }


}
