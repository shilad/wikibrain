package org.wikibrain.sr.evaluation;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.TiesStrategy;
import org.wikibrain.core.lang.Language;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.utils.KnownSim;

import java.io.File;
import java.io.IOException;
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

    public double getMeanAbsoluteError() {
        if (actual.isEmpty()) {
            return 0.0;
        }
        double maeSum = 0.0;
        for (int i = 0; i < actual.size(); i++) {
            maeSum += Math.abs(actual.get(i) - estimates.get(i));
        }
        return maeSum / actual.size();
    }

    public double getRootMeanSquareError() {
        if (actual.isEmpty()) {
            return 0.0;
        }
        double rmsError = 0.0;
        for (int i = 0; i < actual.size(); i++) {
            rmsError += (actual.get(i) - estimates.get(i)) * (actual.get(i) - estimates.get(i));
        }
        return Math.sqrt(rmsError / actual.size());
    }

    public List<KnownSimGuess> getGuesses() throws IOException, ParseException {
        List<KnownSimGuess> guesses = new ArrayList<KnownSimGuess>();
        for (String line : FileUtils.readLines(logPath, "utf-8")) {
            if (line.endsWith("\n")) {
                line = line.substring(0, line.length() - 1);
            }
            String tokens[] = line.split("\t");
            if (tokens[0].equals("entry")) {
                KnownSim ks = new KnownSim(tokens[2], tokens[3], Double.valueOf(tokens[4]), Language.getByFullLangName(tokens[1]));
                String val = tokens[5];
                if (val.equals("failed")) {
                    guesses.add(new KnownSimGuess(ks, Double.NaN));
                } else {
                    guesses.add(new KnownSimGuess(ks, Double.valueOf(val)));
                }
            }
        }
        for (SimilarityEvaluationLog log : getChildEvaluations()) {
            guesses.addAll(log.getGuesses());
        }

        setRanks(guesses);

        return guesses;
    }

    public static void setRanks(List<KnownSimGuess> guesses) {
        NaturalRanking nr = new NaturalRanking(TiesStrategy.MAXIMUM);

        // Part 1: build up pruned lists of actual / estimates excluded NaNs, etc.
        TDoubleList prunedActual = new TDoubleArrayList();
        TDoubleList prunedEstimates = new TDoubleArrayList();

        for (KnownSimGuess g : guesses) {
            if (g.hasGuess()) {
                prunedActual.add(g.getActual());
                prunedEstimates.add((g.getGuess()));
            }
        }

        // Part 2: get ranks
        double [] actualRanks = nr.rank(prunedActual.toArray());
        double [] estimatedRanks = nr.rank(prunedEstimates.toArray());

        // Part 3: specify them
        int i = 0;
        for (KnownSimGuess g : guesses) {
            if (g.hasGuess()) {
                g.setActualRank(1.0 + actualRanks.length - actualRanks[i]);
                g.setPredictedRank(1.0 + estimatedRanks.length - estimatedRanks[i]);
                i++;
            }
        }
        if (i != prunedActual.size()) {
            throw new IllegalStateException();
        }
    }

    /**
     * @see BaseEvaluationLog#getSummaryAsMap()
     * @return
     */
    public Map<String, String> getSummaryAsMap() {
        Map<String, String> summary = super.getSummaryAsMap();
        summary.put("spearmans", Double.toString(getSpearmansCorrelation()));
        summary.put("pearsons", Double.toString(getPearsonsCorrelation()));
        summary.put("mae", Double.toString(getMeanAbsoluteError()));
        summary.put("rms", Double.toString(getRootMeanSquareError()));
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
            if (line.trim().isEmpty()) {
                continue;
            }
            String tokens[] = line.split("\t");
            if (tokens[0].equals("start")) {
                start = parseDate(tokens[1]);
            } else if (tokens[0].equals("config")) {
                config.put(tokens[1], tokens[2]);
            } else if (tokens[0].equals("merge")) {
                if (eval == null) {
                    eval = new SimilarityEvaluationLog(config, null, start);
                }
                eval.merge(read(new File(tokens[1])));
            } else if (tokens[0].equals("entry")) {
                if (eval == null) {
                    eval = new SimilarityEvaluationLog(config, null, start);
                }
                KnownSim ks = new KnownSim(tokens[2], tokens[3], Double.valueOf(tokens[4]), Language.getByFullLangName(tokens[1]));
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
        eval.logPath = path;


        return eval;
    }

}
