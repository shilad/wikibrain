package org.wikapidia.sr.evaluation;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.wikapidia.core.lang.Language;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.utils.KnownSim;

import java.io.*;
import java.text.ParseException;
import java.util.*;

/**
 * A class that accumulates similarity() evaluation metrics.
 * The results can optionally be logged to a file.
 *
 * @author Shilad Sen
 */
public class SimilarityEvaluationLog extends BaseEvaluationLog<SimilarityEvaluationLog> {

    private final TDoubleList actual = new TDoubleArrayList();
    private final TDoubleList estimates = new TDoubleArrayList();

    public SimilarityEvaluationLog() throws IOException {
        super();
    }

    public SimilarityEvaluationLog(File logPath) throws IOException {
        super(logPath);
    }

    public SimilarityEvaluationLog(Map<String, String> config, File logPath) throws IOException {
        super(config, logPath);
    }

    public SimilarityEvaluationLog(Map<String, String> config, File logPath, Date date) throws IOException {
        super(config, logPath, date);
    }

    public synchronized void recordFailed(KnownSim ks) throws IOException {
        failed++;
        write(ks, "failed");
    }

    public synchronized void record(KnownSim ks, SRResult estimate) throws IOException {
        if (estimate == null || Double.isNaN(estimate.getScore()) || Double.isInfinite(estimate.getScore())) {
            missing++;
        } else {
            actual.add(ks.similarity);
            estimates.add(estimate.getScore());
            sucessful++;
        }
        write(ks, estimate == null ? "null" : String.valueOf(estimate.getScore()));
    }

    private synchronized void write(KnownSim ks, String result) throws IOException {
        write("entry\t" + ks.language + "\t" + ks.phrase1 + "\t" + ks.phrase2 + "\t" + ks.similarity + "\t" + result +"\n");
    }

    public double getPearsonsCorrelation() {
        return new PearsonsCorrelation().correlation(actual.toArray(), estimates.toArray());
    }

    public double getSpearmansCorrelation() {
        return new SpearmansCorrelation().correlation(actual.toArray(), estimates.toArray());
    }

    /**
     * @see BaseEvaluationLog#getSummaryAsMap()
     * @return
     */
    public Map<String, String> getSummaryAsMap() {
        Map<String, String> summary = super.getSummaryAsMap();
        summary.put("spearmans", Double.toString(getSpearmansCorrelation()));
        summary.put("pearsons", Double.toString(getPearsonsCorrelation()));
        return summary;
    }

    public List<SimilarityEvaluationLog> getChildEvaluations() throws IOException, ParseException {
        List<SimilarityEvaluationLog> evals = new ArrayList<SimilarityEvaluationLog>();
        for (File file : children) {
            evals.add(read(file));
        }
        return evals;
    }


    protected TDoubleList getActual() {
        return actual;
    }

    protected TDoubleList getEstimates() {
        return estimates;
    }

    @Override
    public void merge(SimilarityEvaluationLog eval) throws IOException {
        super.merge(eval);
        SimilarityEvaluationLog seval = (SimilarityEvaluationLog)eval;
        actual.addAll(seval.actual);
        estimates.addAll(seval.estimates);
    }

    /**
     * Reads in the similarity evaluation at a particular path.
     *
     * @param path
     * @return
     */
    public static SimilarityEvaluationLog read(File path) throws IOException, ParseException {
        Date start = null;
        Map<String, String> config = new HashMap<String, String>();
        SimilarityEvaluationLog eval = null;

        for (String line : FileUtils.readLines(path, "utf-8")) {
            if (line.endsWith("\n")) {
                line = line.substring(0, line.length() - 1);
            }
            String tokens[] = line.split("\t");
            if (tokens[0].equals("start")) {
                start = parseDate(tokens[1]);
            } else if (tokens[0].equals("config")) {
                config.put(tokens[1], tokens[2]);
            } else if (tokens[0].equals("merge")) {
                eval.merge(read(new File(tokens[1])));
            } else if (tokens[0].equals("entry")) {
                if (eval == null) {
                    eval = new SimilarityEvaluationLog(config, null, start);
                }
                KnownSim ks = new KnownSim(tokens[2], tokens[3], Double.valueOf(tokens[4]), Language.getByLangCode(tokens[1]));
                String val = tokens[5];
                if (val.equals("failed")) {
                    eval.recordFailed(ks);
                } else {
                    eval.record(ks, new SRResult(Double.valueOf(val)));
                }
            } else {
                throw new IllegalStateException("invalid event in log " + path + ": " + line);
            }
        }

        return eval;
    }

}
