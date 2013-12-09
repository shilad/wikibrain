package org.wikapidia.sr.evaluation;


import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.utils.KnownSim;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Shilad Sen
 */
public class MostSimilarEvaluation extends BaseEvaluation {

    private final List<MostSimilarGuess> guesses = new ArrayList<MostSimilarGuess>();

    public MostSimilarEvaluation(Map<String, String> config, File logPath, Date date) throws IOException {
        super(config, logPath, date);
    }

    public synchronized void record(KnownMostSim kms, SRResultList mostSimilar) throws IOException {
        MostSimilarGuess guess = new MostSimilarGuess(kms, mostSimilar);
        write(kms, guess.toString());
        sucessful++;
    }

    public double getNDCG() {
        double ndgc = 0.0;
        for (MostSimilarGuess guess : guesses) {

        }
        return ndgc;
    }

    public synchronized void recordFailed(KnownMostSim kms) throws IOException {
        failed++;
        write(kms, "failed");
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
}
