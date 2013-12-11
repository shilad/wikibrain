package org.wikapidia.sr.evaluation;


import org.apache.commons.io.FileUtils;
import org.wikapidia.core.lang.Language;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.utils.KnownSim;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * @author Shilad Sen
 */
public class MostSimilarEvaluationResults extends BaseEvaluationResults<MostSimilarEvaluationResults> {

    private final List<MostSimilarGuess> guesses = new ArrayList<MostSimilarGuess>();

    public MostSimilarEvaluationResults() throws IOException {
        super();
    }
    public MostSimilarEvaluationResults(File logPath) throws IOException {
        super(logPath);
    }

    public MostSimilarEvaluationResults(Map<String, String> config, File logPath) throws IOException {
        super(config, logPath);
    }

    public MostSimilarEvaluationResults(Map<String, String> config, File logPath, Date date) throws IOException {
        super(config, logPath, date);
    }

    public synchronized void record(KnownMostSim kms, SRResultList mostSimilar) throws IOException {
        record(kms, new MostSimilarGuess(kms, mostSimilar));
    }

    public synchronized void record(KnownMostSim kms, MostSimilarGuess guess) throws IOException {
        write(kms, guess.toString());
        sucessful++;
    }

    public double getNDCG() {
        double ndgc = 0.0;
        for (MostSimilarGuess guess : guesses) {
            ndgc += guess.getNDGC();
        }
        return ndgc / guesses.size();
    }

    public synchronized void recordFailed(KnownMostSim kms) throws IOException {
        failed++;
        write(kms, "failed");
    }

    /**
     * @see BaseEvaluationResults#getSummaryAsMap()
     * @return
     */
    public Map<String, String> getSummaryAsMap() {
        Map<String, String> summary = super.getSummaryAsMap();
        summary.put("ndgc", Double.toString(getNDCG()));
        return summary;
    }


    @Override
    public void merge(MostSimilarEvaluationResults eval) throws IOException {
        super.merge(eval);
        MostSimilarEvaluationResults mseval = (MostSimilarEvaluationResults)eval;
        guesses.addAll(mseval.guesses);
    }


    public List<MostSimilarEvaluationResults> getChildEvaluations() throws IOException, ParseException {
        List<MostSimilarEvaluationResults> evals = new ArrayList<MostSimilarEvaluationResults>();
        for (File file : children) {
            evals.add(read(file));
        }
        return evals;
    }


    private synchronized void write(KnownMostSim kms, String result) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("entry\t")
            .append(kms.getLanguage())
            .append("\t")
            .append(cleanPhrase(kms.getPhrase()))
            .append("\t")
            .append(kms.getPageId());

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

        sb.append("\t").append(result).append("\n");
        write(sb.toString());
    }

    private String cleanPhrase(String phrase) {
        return phrase.replace("|", "").replaceAll("\\s+", " ");
    }

    static public MostSimilarEvaluationResults read(File path) throws IOException, ParseException {
        Date start = null;
        Map<String, String> config = new HashMap<String, String>();
        MostSimilarEvaluationResults eval = null;

        for (String line : FileUtils.readLines(path, "utf-8")) {
            if (line.endsWith("\n")) {
                line = line.substring(0, line.length() - 1);
            }
            String tokens[] = line.split("\t");
            if (tokens[0].equals("start")) {
                start = SimilarityEvaluationResults.parseDate(tokens[1]);
            } else if (tokens[0].equals("config")) {
                config.put(tokens[1], tokens[2]);
            } else if (tokens[0].equals("merge")) {
                eval.merge(read(new File(tokens[1])));
            } else if (tokens[0].equals("entry")) {
                if (eval == null) {
                    eval = new MostSimilarEvaluationResults(config, null, start);
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
}
