package org.wikapidia.sr.evaluation;

import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * An evaluator for SR metrics. Writes a directory structure of evaluation results like:
 *
 * baseDir/local/
 *      similarity.tsv                     Tab separated spreadsheet of sr metric results
 *      lang/split-group/run#-metric/
 *              overall.summary          Human-readable summary of metric results
 *              overall.log
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
            System.out.println("making " + dirPath);
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
            ensureIsDirectory(getLocalDir(split));
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
        overall.setConfig("dataset", "overall");
        int runNumber = getNextRunNumber();

        Map<String, SimilarityEvaluation> groupEvals = new HashMap<String, SimilarityEvaluation>();
        String metricName = null;

        for (Split split : splits) {
            LocalSRMetric metric = factory.create();
            metricName = metric.getName();
            SimilarityEvaluation splitEval = evaluateSplit(factory, split, metric, runNumber);
            overall.merge(splitEval);
            if (!groupEvals.containsKey(split.getGroup())) {
                File gfile = new File(getLocalDir(split, metricName, runNumber), "overall.log");
                groupEvals.put(split.getGroup(), new SimilarityEvaluation(gfile));
            }
            groupEvals.get(split.getGroup()).merge(splitEval);
            IOUtils.closeQuietly(splitEval);
        }

        for (String group : groupEvals.keySet()) {
            Split gsplit = getSplitWithGroup(group);
            File gfile = getLocalDir(gsplit, metricName, runNumber);
            SimilarityEvaluation geval = groupEvals.get(group);
            geval.summarize(new File(gfile, "overall.summary"));
            maybeWriteToStdout("Split " + group + ", " + metricName + ", " + runNumber, geval);
            if (writeToStdout) geval.summarize();
            updateOverallTsv(geval);
            IOUtils.closeQuietly(geval);
        }
        maybeWriteToStdout("Overall for run " + runNumber, overall);

        updateOverallTsv(overall);

        return overall;
    }

    private Split getSplitWithGroup(String group) {
        for (Split s : splits) {
            if (s.getGroup().equals(group)) {
                return s;
            }
        }
        return null;
    }

    private static final List<String> TSV_FIELDS = Arrays.asList(
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

    /**
     * Updates the overall tsv file for a particular group
     * @param eval
     */
    private void updateOverallTsv(SimilarityEvaluation eval) throws IOException {
        File tsv = FileUtils.getFile(outputDir, "local", "similarity.tsv");
        String toWrite = "";
        if (!tsv.isFile()) {
            toWrite += StringUtils.join(TSV_FIELDS, "\t") + "\n";
        }
        Map<String, String> summary = eval.getSummaryAsMap();
        for (int i = 0; i < TSV_FIELDS.size(); i++) {
            String field = TSV_FIELDS.get(i);
            String value = summary.get(field);
            if (i > 0) {
                toWrite += "\t";
            }
            toWrite += value.replace('\t', ' ');
        }
        toWrite += "\n";
        FileUtils.write(tsv, toWrite, true);
    }

    /**
     * Evaluates an sr metric against a single split and writes log, error, and summary files.
     *
     *
     * @param factory
     * @param split
     * @param metric
     * @param runNumber
     * @return
     * @throws IOException
     * @throws DaoException
     */
    private SimilarityEvaluation evaluateSplit(LocalSRFactory factory, Split split, LocalSRMetric metric, int runNumber) throws IOException, DaoException {
        File dir = getLocalDir(split, metric, runNumber);
        ensureIsDirectory(dir);
        File log = new File(dir, split.getName() + ".log");
        File err = new File(dir, split.getName() + ".err");
        File summary = new File(dir, split.getName() + ".summary");

        BufferedWriter errFile = new BufferedWriter(new FileWriter(err));

        metric.trainSimilarity(split.getTrain());

        Map<String, String> config = new LinkedHashMap<String, String>();
        config.put("lang", split.getTest().getLanguage().getLangCode());
        config.put("dataset", split.getGroup());
        config.put("metricName", metric.getName());
        config.put("runNumber", "" + runNumber);
        config.put("metricConfig", factory.describeMetric());
        config.put("disambigConfig", factory.describeDisambiguator());
        SimilarityEvaluation splitEval = new SimilarityEvaluation(config, log);

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
        splitEval.summarize(summary);
        maybeWriteToStdout(
                "Split " + split.getGroup() + ", " + split.getName() + ", " + metric.getName() + ", " + runNumber,
                splitEval);
        return splitEval;
    }

    private void maybeWriteToStdout(String caption, BaseEvaluation eval) throws IOException {
        if (!writeToStdout) {
            return;
        }
        System.out.println("Similarity evaluation for " + caption);
        eval.summarize(System.out);
    }

    public List<Split> getSplits() {
        return Collections.unmodifiableList(splits);
    }
}
