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
 * baseDir/local-similarity/                Or local-mostSimilar, universal-similarity, etc.
 *      summary.tsv                         Tab separated spreadsheet of sr metric results
 *      lang/split-group/run#-metric/
 *              overall.summary             Human-readable summary of metric results
 *              overall.log
 *              splitname1.summary          Human-readable summary of splitname1 within group
 *              splitname2.summary
 *              splitname3.summary
 *              splitname1.log              Log of results from splitname1 within group
 *              splitname2.log
 *              splitname3.log
 *              splitname1.err              Error logs for splitname1, within group
 *              splitname2.err
 *              splitname3.err
 *
 *
 * TODO: Refactor to abstract BaseEvaluator, concrete SimilarityEvaluator and MostSimilarEvaluator.
 *
 * @author Shilad Sen
 */
public abstract class Evaluator <T extends BaseEvaluationResults<T>> {
    private static final Logger LOG = Logger.getLogger(Evaluator.class.getName());
    private final File baseDir;
    private final String modeName;
    private final File modeDir;
    private boolean writeToStdout = true;

    private List<Split> splits = new ArrayList<Split>();

    /**
     * @param baseDir baseDir in structure shown above
     * @param modeName  "local-similarity", etc
     */
    public Evaluator(File baseDir, String modeName) {
        this.baseDir = baseDir;
        this.modeName = modeName;
        this.modeDir = new File(baseDir, modeName);
        ensureIsDirectory(modeDir);
    }

    public void setWriteToStdout(boolean writeToStdout) {
        this.writeToStdout = writeToStdout;
    }

    public abstract void addCrossfolds(Dataset ds, int numFolds);

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
            LOG.info("making " + dirPath);
        }
    }

    Pattern MATCH_RUN = Pattern.compile("^(\\d+)-.*");

    /**
     * @return One more than the max run number across all modes, splits, and splits and metrics.
     */
    private int getNextRunNumber() {
        int runNum = 0;
        FileFilter dirFilter = DirectoryFileFilter.INSTANCE;
        for (File modeFile : baseDir.listFiles(dirFilter)) {
            for (File langFile : modeFile.listFiles(dirFilter)) {
                for (File groupFile : langFile.listFiles(dirFilter)) {
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

    private File getLocalDir(Split split) {
        return FileUtils.getFile(
                modeDir,
                split.getTest().getLanguage().getLangCode(),
                split.getGroup());
    }

    private File getLocalDir(Split split, int runNumber, String metricName) {
        return new File(getLocalDir(split), runNumber + "-" + metricName);
    }

    public abstract T createResults(File path) throws IOException;
    public abstract List<String> getSummaryFields();

    public synchronized void evaluate(LocalSRFactory factory) throws IOException, DaoException {
        T overall = createResults(null);
        overall.setConfig("dataset", "overall");
        int runNumber = getNextRunNumber();

        Map<String, T> groupEvals = new HashMap<String, T>();
        String metricName = factory.getName();

        for (Split split : splits) {
            T splitEval = evaluateSplitInternal(factory, split, runNumber);
            overall.merge(splitEval);
            if (!groupEvals.containsKey(split.getGroup())) {
                File gfile = new File(getLocalDir(split, runNumber, metricName), "overall.log");
                groupEvals.put(split.getGroup(), createResults(gfile));
            }
            groupEvals.get(split.getGroup()).merge(splitEval);
            IOUtils.closeQuietly(splitEval);
        }

        for (String group : groupEvals.keySet()) {
            Split gsplit = getSplitWithGroup(group);
            File gfile = getLocalDir(gsplit, runNumber, metricName);
            BaseEvaluationResults geval = groupEvals.get(group);
            geval.summarize(new File(gfile, "overall.summary"));
            maybeWriteToStdout("Split " + group + ", " + metricName + ", " + runNumber, geval);
            if (writeToStdout) geval.summarize();
            updateOverallTsv(geval);
            IOUtils.closeQuietly(geval);
        }
        maybeWriteToStdout("Overall for run " + runNumber, overall);
        updateOverallTsv(overall);
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
    private void updateOverallTsv(BaseEvaluationResults eval) throws IOException {
        File tsv = FileUtils.getFile(baseDir, "local", "summary.tsv");
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
    private T evaluateSplitInternal(LocalSRFactory factory, Split split, int runNumber) throws IOException, DaoException {
        File dir = getLocalDir(split, runNumber, factory.getName());
        ensureIsDirectory(dir);
        File log = new File(dir, split.getName() + ".log");
        File err = new File(dir, split.getName() + ".err");
        File summary = new File(dir, split.getName() + ".summary");

        Map<String, String> config = new LinkedHashMap<String, String>();
        config.put("lang", split.getTest().getLanguage().getLangCode());
        config.put("dataset", split.getGroup());
        config.put("mode", modeName.toString().toLowerCase());
        config.put("metricName", factory.getName());
        config.put("runNumber", "" + runNumber);
        config.put("metricConfig", factory.describeMetric());
        config.put("disambigConfig", factory.describeDisambiguator());

        T splitEval = evaluateSplit(factory, split, log, err, config);
        splitEval.summarize(summary);
        maybeWriteToStdout(
                "Split " + modeName + ", " + split.getGroup() + ", " + split.getName() + ", " + factory.getName() + ", " + runNumber,
                splitEval);
        return splitEval;
    }

    protected abstract T evaluateSplit(LocalSRFactory factory, Split split, File log, File err, Map<String, String> conf) throws DaoException, IOException;

    private void maybeWriteToStdout(String caption, BaseEvaluationResults eval) throws IOException {
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
