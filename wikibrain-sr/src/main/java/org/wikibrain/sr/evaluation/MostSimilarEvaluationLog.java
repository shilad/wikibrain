package org.wikibrain.sr.evaluation;


import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.wikibrain.core.lang.Language;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.utils.KnownSim;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * @author Shilad Sen
 */
public class MostSimilarEvaluationLog extends BaseEvaluationLog<MostSimilarEvaluationLog> {

    private final List<MostSimilarGuess> guesses = new ArrayList<MostSimilarGuess>();

    /**
     * Precision and recall is measured at these ranks
     */
    private int[] precisionRecallRanks = {1, 5, 10, 20, 50, 100, 500, 1000};

    /**
     * The threshold under which items are not considered relevant.
     */
    private double relevanceThreshold = 0.6;

    public MostSimilarEvaluationLog() throws IOException {
        super();
    }
    public MostSimilarEvaluationLog(File logPath) throws IOException {
        super(logPath);
    }

    public MostSimilarEvaluationLog(Map<String, String> config, File logPath) throws IOException {
        super(config, logPath);
    }

    public MostSimilarEvaluationLog(Map<String, String> config, File logPath, Date date) throws IOException {
        super(config, logPath, date);
    }

    public synchronized void record(KnownMostSim kms, SRResultList mostSimilar) throws IOException {
        record(kms, new MostSimilarGuess(kms, mostSimilar));
    }

    public synchronized void record(KnownMostSim kms, MostSimilarGuess guess) throws IOException {
        write(kms, guess.toString());
        sucessful++;
        guesses.add(guess);
    }

    public double getNDCG() {
        double sumWeights = 0.0;
        double ndgc = 0.0;
        for (MostSimilarGuess guess : guesses) {
            double w = guess.getObservations().size() - 1;
            double v = guess.getNDGC();
            if (w >= 0.99 && !Double.isNaN(v) && !Double.isInfinite(v)) {
                ndgc += w * v;
                sumWeights += w;
            }
        }
        return ndgc / sumWeights;
    }
    public double getPenalizedNDCG() {
        double sumWeights = 0.0;
        double ndgc = 0.0;
        for (MostSimilarGuess guess : guesses) {
            double w = guess.getKnown().getMostSimilar().size() - 1;
            double v = guess.getPenalizedNDGC();
            if (w >= 0.99 && !Double.isNaN(v) && !Double.isInfinite(v)) {
                ndgc += w * v;
                sumWeights += w;
            }
        }
        return ndgc / sumWeights;
    }

    public PrecisionRecallAccumulator getPrecisionRecall(int n, double threshold) {
        PrecisionRecallAccumulator pr = new PrecisionRecallAccumulator(n, threshold);
        for (MostSimilarGuess guess : guesses) {
            pr.merge(guess.getPrecisionRecall(n, threshold));
        }
        return pr;
    }

    public synchronized void recordFailed(KnownMostSim kms) throws IOException {
        failed++;
        write(kms, "failed");
    }

    /**
     * @see BaseEvaluationLog#getSummaryAsMap()
     * @return
     */
    public Map<String, String> getSummaryAsMap() {
        Map<String, String> summary = super.getSummaryAsMap();
        summary.put("pearsons", Double.toString(getPearsonsCorrelation()));
        summary.put("spearmans", Double.toString(getSpearmansCorrelation()));
        summary.put("ndgc", Double.toString(getNDCG()));
        summary.put("penalizedNdgc", Double.toString(getPenalizedNDCG()));
        for (int n : precisionRecallRanks) {
            PrecisionRecallAccumulator pr = getPrecisionRecall(n, relevanceThreshold);
            summary.put("num-"+n, Integer.toString(pr.getRetrievedIrrelevant() + pr.getRetrievedRelevant()));
            summary.put("mean-"+n, Double.toString(pr.getMeanRelevance()));
            summary.put("precision-"+n, Double.toString(pr.getPrecision()));
            summary.put("recall-"+n, Double.toString(pr.getRecall()));
        }
        return summary;
    }


    @Override
    public void merge(MostSimilarEvaluationLog eval) throws IOException {
        super.merge(eval);
        guesses.addAll(eval.guesses);
    }


    public List<MostSimilarEvaluationLog> getChildEvaluations() throws IOException, ParseException {
        List<MostSimilarEvaluationLog> evals = new ArrayList<MostSimilarEvaluationLog>();
        for (File file : children) {
            evals.add(read(file));
        }
        return evals;
    }

    public double getSpearmansCorrelation() {
        TDoubleList actual = new TDoubleArrayList();
        TDoubleList expected = new TDoubleArrayList();
        for (MostSimilarGuess msg : guesses) {
            for (MostSimilarGuess.Observation o : msg.getObservations()) {
                if (!Double.isInfinite(o.estimate) && !Double.isNaN(o.estimate)) {
                    actual.add(o.actual);
                    expected.add(o.estimate);
                }
            }
        }
        if (actual.size() < 2) {
            return Double.NaN;
        } else {
            return new SpearmansCorrelation().correlation(actual.toArray(), expected.toArray());
        }
    }

    public double getPearsonsCorrelation() {
        TDoubleList actual = new TDoubleArrayList();
        TDoubleList expected = new TDoubleArrayList();
        for (MostSimilarGuess msg : guesses) {
            for (MostSimilarGuess.Observation o : msg.getObservations()) {
                if (!Double.isInfinite(o.estimate) && !Double.isNaN(o.estimate)) {
                    actual.add(o.actual);
                    expected.add(o.estimate);
                }
            }
        }
        if (actual.size() < 2) {
            return Double.NaN;
        } else {
            return new PearsonsCorrelation().correlation(actual.toArray(), expected.toArray());
        }
    }


    private synchronized void write(KnownMostSim kms, String result) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("entry\t")
            .append(kms.getLanguage())
            .append("\t")
            .append(cleanPhrase(kms.getPhrase()))
            .append("\t")
            .append(kms.getPageId())
            .append("\t");

        int rank = 0;
        for (KnownSim ks : kms.getMostSimilar()) {
            if (rank > 0) sb.append("|");
            sb.append(ks.wpId2)
                .append("@")
                .append(ks.similarity)
                .append("@")
                .append(cleanPhrase(ks.phrase2));
            rank++;
        }

        sb.append("\t").append(result);
        write(sb.toString());
    }

    private String cleanPhrase(String phrase) {
        return phrase.replace("|", "").replaceAll("\\s+", " ");
    }

    static public MostSimilarEvaluationLog read(File path) throws IOException, ParseException {
        Date start = null;
        Map<String, String> config = new HashMap<String, String>();
        MostSimilarEvaluationLog eval = null;

        for (String line : FileUtils.readLines(path, "utf-8")) {
            if (line.endsWith("\n")) {
                line = line.substring(0, line.length() - 1);
            }
            String tokens[] = line.split("\t");
            if (tokens[0].equals("start")) {
                start = SimilarityEvaluationLog.parseDate(tokens[1]);
            } else if (tokens[0].equals("config")) {
                config.put(tokens[1], tokens[2]);
            } else if (tokens[0].equals("merge")) {
                eval.merge(read(new File(tokens[1])));
            } else if (tokens[0].equals("entry")) {
                if (eval == null) {
                    eval = new MostSimilarEvaluationLog(config, null, start);
                }
                List<KnownSim> sims = new ArrayList<KnownSim>();
                Language lang = Language.getByLangCode(tokens[1]);
                String phrase1 = tokens[2];
                int localId1 = Integer.valueOf(tokens[3]);
                for (String ksStr : tokens[4].split("[|]")) {
                    String ksTokens[] = ksStr.split("[@]");
                    int localId2 = Integer.valueOf(ksTokens[0]);
                    double sim = Double.valueOf(ksTokens[1]);
                    String phrase2 = ksTokens[2];
                    sims.add(new KnownSim(phrase1, phrase2, localId1, localId2, sim, lang));
                }
                KnownMostSim ks = new KnownMostSim(sims);
                String val = tokens[5];
                if (val.equals("failed")) {
                    eval.recordFailed(ks);
                } else {
                    eval.record(ks, new MostSimilarGuess(ks, val));
                }
            } else {
                throw new IllegalStateException("invalid event in log " + path + ": " + line);
            }
        }

        return eval;
    }

    public void setPrecisionRecallRanks(int[] precisionRecallRanks) {
        this.precisionRecallRanks = precisionRecallRanks;
    }

    public void setRelevanceThreshold(double relevanceThreshold) {
        this.relevanceThreshold = relevanceThreshold;
    }
}
