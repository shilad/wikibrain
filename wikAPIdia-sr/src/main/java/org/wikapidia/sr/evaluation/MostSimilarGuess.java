package org.wikapidia.sr.evaluation;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.utils.KnownSim;

import java.util.ArrayList;
import java.util.List;

/**
* @author Shilad Sen
*/
public class MostSimilarGuess {
    private final KnownMostSim known;
    private final int length;             // length of most similar guess list
    private final List<Observation> observations = new ArrayList<Observation>();

    public MostSimilarGuess(KnownMostSim known, String str) {
        this.known = known;
        String tokens[] = str.split("[|]");
        length = Integer.valueOf(tokens[0]);
        for (int i = 1; i < tokens.length; i++) {
            String tuple[] = tokens[i].split("[@]");
            observations.add(new Observation(Integer.valueOf(tuple[0]), Integer.valueOf(tuple[1]), Double.valueOf(tuple[2])));
        }
    }

    public MostSimilarGuess(KnownMostSim known, SRResultList guess) {
        this.known = known;
        length = guess.numDocs();
        TIntSet knownIds = new TIntHashSet();
        for (KnownSim ks : known.getMostSimilar()) {
            knownIds.add(ks.wpId2);
        }
        for (int i = 0; i < guess.numDocs(); i++) {
            SRResult sr = guess.get(i);
            if (knownIds.contains(sr.getId())) {
                observations.add(new Observation(i+1, sr.getId(), sr.getScore()));
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(length);
        for (Observation observation : observations) {
            sb.append("|")
                .append(observation.rank)
                .append("@")
                .append(observation.id)
                .append("@")
                .append(observation.score);
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
     * @see https://en.wikipedia.org/wiki/Discounted_cumulative_gain#Normalized_DCG
     * @return The normalized discounted cummulative gain.
     */
    public double getNDGC() {
        if (observations.isEmpty()) {
            return 0.0;
        }
        TDoubleList scores = new TDoubleArrayList();
        double s = observations.get(0).score;
        for (int i = 1; i < observations.size(); i++) {
            Observation o = observations.get(i);
            s += o.score / Math.log(i + 1);
            scores.add(o.score);
        }
        scores.sort();
        scores.reverse();
        double t = scores.get(0);
        for (int i = 1; i < scores.size(); i++) {
            t += scores.get(i) / Math.log(i + 1);
        }
        return s / t;
    }

    /**
     * Represents an entry in the most similar result list with known similarity.
     */
    public static class Observation {
        public int rank;        // rank in result list (range is [1...length of list])
        public int id;          // wp local page id
        public double score;    // similarity score

        public Observation(int rank, int id, double score) {
            this.rank = rank;
            this.id = id;
            this.score = score;
        }

        @Override
        public String toString() {
            return "Observation{" +
                    "rank=" + rank +
                    ", id=" + id +
                    ", score=" + score +
                    '}';
        }
    }
}
