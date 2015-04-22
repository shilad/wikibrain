package org.wikibrain.sr.evaluation;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import org.apache.commons.lang3.ArrayUtils;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.utils.KnownSim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A most generated similar list that can be evaluated against a known human-labeled gold-standard.
 * @author Shilad Sen
 */
public class MostSimilarGuess {
    private final KnownMostSim known;
    private final int length;             // length of most similar guess list
    private final double minScore;
    private final double maxScore;
    private final List<Observation> observations = new ArrayList<Observation>();

    public MostSimilarGuess(KnownMostSim known, String str) {
        this.known = known;
        TIntDoubleMap actual = new TIntDoubleHashMap();
        for (KnownSim ks : known.getMostSimilar()) {
            actual.put(ks.wpId2, ks.similarity);
        }
        String tokens[] = str.split("[|]");
        length = Integer.valueOf(tokens[0]);
        minScore = Double.valueOf(tokens[1]);
        maxScore = Double.valueOf(tokens[2]);
        for (int i = 3; i < tokens.length; i++) {
            String tuple[] = tokens[i].split("[@]");
            int id = Integer.valueOf(tuple[1]);
            observations.add(new Observation(
                        Integer.valueOf(tuple[0]),
                        id,
                        Double.valueOf(tuple[2]),
                        actual.get(id)));
        }
    }

    public MostSimilarGuess(KnownMostSim known, SRResultList guess) {
        this.known = known;
        length = guess.numDocs();
        minScore = guess.minScore();
        maxScore = guess.maxScore();
        TIntDoubleMap actual = new TIntDoubleHashMap();
        for (KnownSim ks : known.getMostSimilar()) {
            actual.put(ks.wpId2, ks.similarity);
        }
        for (int i = 0; i < guess.numDocs(); i++) {
            SRResult sr = guess.get(i);
            if (actual.containsKey(sr.getId())) {
                observations.add(new Observation(i+1, sr.getId(), sr.getScore(), actual.get(sr.getId())));
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(length);
        sb.append("|");
        sb.append(minScore);
        sb.append("|");
        sb.append(maxScore);
        for (Observation observation : observations) {
            sb.append("|")
                .append(observation.rank)
                .append("@")
                .append(observation.id)
                .append("@")
                .append(observation.estimate);
        }
        return sb.toString();
    }

    public List<Observation> getObservations() {
        return observations;
    }

    public int getLength() {
        return length;
    }

    public KnownMostSim getKnown() {
        return known;
    }

    /**
     * @see <a href="https://en.wikipedia.org/wiki/Discounted_cumulative_gain#Normalized_DCG">Wikipedia</a>
     * @return The normalized discounted cummulative gain. Results with no known
     * entry are totally ignored.
     */
    public double getNDGC() {
        if (observations.isEmpty()) {
            return 0.0;
        }
        TIntDoubleMap actual = new TIntDoubleHashMap();
        for (KnownSim ks : known.getMostSimilar()) {
            actual.put(ks.wpId2, ks.similarity);
        }
        int ranks[] = new int[observations.size()];
        double scores[] = new double[observations.size()];
        double s = 0.0;
        for (int i = 0; i < observations.size(); i++) {
            Observation o = observations.get(i);
            double k = (o.rank == 1) ? 1 : Math.log(o.rank + 1);
            s += actual.get(o.id) / k;
            scores[i] = actual.get(o.id);
            ranks[i] = o.rank;
        }
        Arrays.sort(ranks);
        Arrays.sort(scores);
        ArrayUtils.reverse(scores);
        double t = 0;
        for (int i = 0; i < scores.length; i++) {
            double k = (ranks[i] == 1) ? 1 : Math.log(ranks[i] + 1);
            t += scores[i] / k;
        }
        return s / t;
    }

    /**
     * @see <a href="https://en.wikipedia.org/wiki/Discounted_cumulative_gain#Normalized_DCG">Wikipedia</a>
     * @return The normalized discounted cummulative gain, but assumes unobserved
     * KnownSim entries lie somewhere below the observed list.
     */
    public double getPenalizedNDGC() {
        if (observations.isEmpty()) {
            return 0.0;
        }
        TIntDoubleMap actual = new TIntDoubleHashMap();
        for (KnownSim ks : known.getMostSimilar()) {
            actual.put(ks.wpId2, ks.similarity);
        }
        double s = 0.0;
        int ranks[] = new int[observations.size()];
        for (int i = 0; i < observations.size(); i++) {
            Observation o = observations.get(i);
            double k = (o.rank == 1) ? 1 : Math.log(o.rank + 1);
            s += actual.get(o.id) / k;
            ranks[i] = o.rank;
        }
        Arrays.sort(ranks);

        // there are known.size() - observations.size() unobserved items.
        // calculate the expected rank and score of them
        // unobserved rank is 3 * length of the list (totally random!)
        // unobserved similarity is the mean between the minimum score and 0
        int unobservedCount = (known.getMostSimilar().size() - observations.size());
        int unobservedRank = 3 * length;
        double unobservedScore = minScore / 2;
        s += unobservedCount * unobservedScore / Math.log(unobservedRank + 1);

        // Calculate maximum over ALL entries - not just observed ones.
        double t = 0;
        for (int i = 0; i < known.getMostSimilar().size(); i++) {
            double k;
            if (i < ranks.length) {
                k = (ranks[i] == 1) ? 1 : Math.log(ranks[i] + 1);
            } else {
                k = Math.log(unobservedRank + 1);
            }
            t += known.getMostSimilar().get(i).similarity / k;
        }

        return s / t;
    }

    public PrecisionRecallAccumulator getPrecisionRecall(int n, double threshold) {
        PrecisionRecallAccumulator pr = new PrecisionRecallAccumulator(n, threshold);
        TIntDoubleMap actual = new TIntDoubleHashMap();
        for (KnownSim ks : known.getMostSimilar()) {
            pr.observe(ks.similarity);
            actual.put(ks.wpId2, ks.similarity);
        }
        for (Observation o : observations) {
            if (o.rank > n) {
                break;
            }
            pr.observeRetrieved(actual.get(o.id));
        }
        return pr;
    }

    /**
     * Represents an entry in the most similar result list with known similarity.
     */
    public static class Observation {
        public int rank;            // rank in result list (range is [1...length of list])
        public int id;              // wp local page id
        public double estimate;     // estimated relatedness
        public double actual;       // actual human score

        public Observation(int rank, int id, double estimate, double actual) {
            this.rank = rank;
            this.id = id;
            this.estimate = estimate;
            this.actual = actual;
        }

        @Override
        public String toString() {
            return "Observation{" +
                    "rank=" + rank +
                    ", id=" + id +
                    ", estimate=" + estimate +
                    ", actual=" + actual +
                    '}';
        }
    }
}
