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
 *
 * An evaluator for SR metrics. Writes a directory structure of evaluation results like:
 *
 * baseDir/local/
 *      similarity.tsv                     Tab separated spreadsheet of sr metric results
 *      mostSimilar.tsv
 *      lang/similarity/split-group/run#-metric/
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
 *
 * TODO: Refactor to abstract BaseEvaluator, concrete SimilarityEvaluator and MostSimilarEvaluator.
 *
 * @author Shilad Sen
 */
public class Evaluator {
    public enum Mode {
        SIMILARITY,
        MOSTSIMILAR
    };

    private static final Logger LOG = Logger.getLogger(Evaluator.class.getName());
    private final File outputDir;
    private boolean writeToStdout = true;

    // These arguments will be passed to calls to mostSimilar()
    private int numMostSimilarResults = 500;
    private TIntHashSet mostSimilarIds = null;

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

    public void addCrossfolds(Mode mode, Dataset ds, int numFolds) {
        if (mode == Mode.SIMILARITY) {
            addSimilarityCrossfolds(ds, numFolds);
        } else if (mode == Mode.MOSTSIMILAR) {
            addMostSimilarCrossfolds(ds, numFolds);
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Adds a crossfold validation of a particular dataset.
     * The group of the split is set to the name of the dataset.
     * @param ds
     * @param numFolds
     */
    public void addSimilarityCrossfolds(Dataset ds, int numFolds) {
        List<Dataset> folds = ds.split(numFolds);
        for (int i = 0; i < folds.size(); i++) {
            Dataset test = folds.get(i);
            List<Dataset> trains = new ArrayList<Dataset>(folds);
            trains.remove(i);
            splits.add(new Split(ds.getName() + "-fold-" + i, ds.getName(), new Dataset(trains), test));
        }
    }

    /**
     * Adds a crossfold validation of a particular dataset.
     * The group of the split is set to the name of the dataset.
     * @param ds
     * @param numFolds
     */
    public void addMostSimilarCrossfolds(Dataset ds, int numFolds) {
        MostSimilarDataset msd = new MostSimilarDataset(ds);
        List<Dataset> folds = msd.splitIntoDatasets(numFolds);
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

    /**
     * Creates a directory if it does not exist already
     * @param dirPath
     */
    private void ensureIsDirectory(File dirPath) {
        if (!dirPath.isDirectory()) {
            FileUtils.deleteQuietly(dirPath);
            dirPath.mkdirs();
            System.out.println("making " + dirPath);
        }
    }

    Pattern MATCH_RUN = Pattern.compile("^(\\d+)-.*");

    /**
     * @return One more than the max run number across all modes, splits, and splits and metrics.
     */
    private int getNextRunNumber() {
        int runNum = 0;
        FileFilter dirFilter = DirectoryFileFilter.INSTANCE;
        for (File langFile : new File(outputDir, "local").listFiles(dirFilter)) {
            for (File modeFile : langFile.listFiles(dirFilter)) {
                for (File groupFile : modeFile.listFiles(dirFilter)) {
                    for (File runFile : groupFile.listFiles(dirFilter)) {
                        String name = runFile.getName();
                        Matcher matcher = MATCH_RUN.matcher(name);
                        if (matcher.matches()) {
                            runNum = Integer.valueOf(matcher.group(1)) + 1;
                        }
                    }
                }
            }
        }
        return runNum;
    }

    private File getLocalDir(Split split, Mode mode) {
        return FileUtils.getFile(
                outputDir,
                "local",
                split.getTest().getLanguage().getLangCode(),
                mode.toString().toLowerCase(),
                split.getGroup());
    }

    private File getLocalDir(Split split, Mode mode, int runNumber, String metricName) {
        return new File(getLocalDir(split, mode), runNumber + "-" + metricName);
    }

    public BaseEvaluation evaluate(Mode mode, LocalSRFactory factory) throws DaoException, ConfigurationException, IOException {
        if (mode == Mode.SIMILARITY) {
            return evaluateSimilarity(factory);
        } else if (mode == Mode.MOSTSIMILAR) {
            return evaluateMostSimilar(factory);
        } else {
            throw new IllegalStateException();
        }
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
        evaluate(overall, Mode.SIMILARITY, factory);
        return overall;
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
    public synchronized MostSimilarEvaluation evaluateMostSimilar(LocalSRFactory factory) throws DaoException, IOException, ConfigurationException {
        MostSimilarEvaluation overall = new MostSimilarEvaluation();
        evaluate(overall, Mode.MOSTSIMILAR, factory);
        return overall;
    }

    private synchronized void evaluate(BaseEvaluation overall, Mode mode, LocalSRFactory factory) throws IOException, DaoException {
        overall.setConfig("dataset", "overall");
        int runNumber = getNextRunNumber();

        Map<String, BaseEvaluation> groupEvals = new HashMap<String, BaseEvaluation>();
        String metricName = factory.getName();

        for (Split split : splits) {
            BaseEvaluation splitEval = evaluateSplit(mode, factory, split, runNumber);
            overall.merge(splitEval);
            if (!groupEvals.containsKey(split.getGroup())) {
                File gfile = new File(getLocalDir(split, mode, runNumber, metricName), "overall.log");
                BaseEvaluation eval = (mode == Mode.SIMILARITY) ? new SimilarityEvaluation(gfile) : new MostSimilarEvaluation(gfile);
                groupEvals.put(split.getGroup(), eval);
            }
            groupEvals.get(split.getGroup()).merge(splitEval);
            IOUtils.closeQuietly(splitEval);
        }

        for (String group : groupEvals.keySet()) {
            Split gsplit = getSplitWithGroup(group);
            File gfile = getLocalDir(gsplit, mode, runNumber, metricName);
            BaseEvaluation geval = groupEvals.get(group);
            geval.summarize(new File(gfile, "overall.summary"));
            maybeWriteToStdout("Split " + group + ", " + metricName + ", " + runNumber, geval);
            if (writeToStdout) geval.summarize();
            updateOverallTsv(mode, geval);
            IOUtils.closeQuietly(geval);
        }
        maybeWriteToStdout("Overall for run " + runNumber, overall);
        updateOverallTsv(mode, overall);
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
    private void updateOverallTsv(Mode mode, BaseEvaluation eval) throws IOException {
        File tsv = FileUtils.getFile(outputDir, "local", mode.toString().toLowerCase() + ".tsv");
        String toWrite = "";
        if (!tsv.isFile()) {
            toWrite += StringUtils.join(TSV_FIELDS, "\t") + "\n";
        }
        Map<String, String> summary = eval.getSummaryAsMap();
        for (int i = 0; i < TSV_FIELDS.size(); i++) {
            String field = TSV_FIELDS.get(i);
            String value = summary.get(field);
            if (value == null) value = "";
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
     *
     * @param factory
     * @param split
     * @param runNumber
     * @return
     * @throws IOException
     * @throws DaoException
     */
    private BaseEvaluation evaluateSplit(Mode mode, LocalSRFactory factory, Split split, int runNumber) throws IOException, DaoException {
        File dir = getLocalDir(split, mode, runNumber, factory.getName());
        ensureIsDirectory(dir);
        File log = new File(dir, split.getName() + ".log");
        File err = new File(dir, split.getName() + ".err");
        File summary = new File(dir, split.getName() + ".summary");

        Map<String, String> config = new LinkedHashMap<String, String>();
        config.put("lang", split.getTest().getLanguage().getLangCode());
        config.put("dataset", split.getGroup());
        config.put("mode", mode.toString().toLowerCase());
        config.put("metricName", factory.getName());
        config.put("runNumber", "" + runNumber);
        config.put("metricConfig", factory.describeMetric());
        config.put("disambigConfig", factory.describeDisambiguator());

        BaseEvaluation splitEval;
        if (mode == Mode.SIMILARITY) {
            splitEval = evaluateSimilarityForSplit(factory, split, log, err, config);
        } else {
            splitEval = evaluateMostSimilarForSplit(factory, split, log, err, config);
        }

        splitEval.summarize(summary);
        maybeWriteToStdout(
                "Split " + mode + ", " + split.getGroup() + ", " + split.getName() + ", " + factory.getName() + ", " + runNumber,
                splitEval);
        return splitEval;
    }

    /**
     * Evaluate a particular split for similarity()
     * @param factory
     * @param split
     * @param log
     * @param err
     * @param config
     * @return
     * @throws IOException
     * @throws DaoException
     */
    private SimilarityEvaluation evaluateSimilarityForSplit(LocalSRFactory factory, Split split, File log, File err, Map<String, String> config) throws IOException, DaoException {
        LocalSRMetric metric = factory.create();
        metric.trainSimilarity(split.getTrain());
        SimilarityEvaluation splitEval = new SimilarityEvaluation(config, log);
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

    /**
     * Evaluates a particular split for mostSimilar()
     * @param factory
     * @param split
     * @param log
     * @param err
     * @param config
     * @return
     * @throws IOException
     * @throws DaoException
     */
    private MostSimilarEvaluation evaluateMostSimilarForSplit(LocalSRFactory factory, Split split, File log, File err, Map<String, String> config) throws IOException, DaoException {
        LocalSRMetric metric = factory.create();
        metric.trainMostSimilar(split.getTrain(), numMostSimilarResults, mostSimilarIds);
        MostSimilarEvaluation splitEval = new MostSimilarEvaluation(config, log);
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

    public void setMostSimilarIds(TIntHashSet mostSimilarIds) {
        this.mostSimilarIds = mostSimilarIds;
    }

    public void setNumMostSimilarResults(int numMostSimilarResults) {
        this.numMostSimilarResults = numMostSimilarResults;
    }
}
