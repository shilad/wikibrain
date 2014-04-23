package org.wikibrain.sr.evaluation;

import org.junit.Before;
import org.junit.Test;
import org.wikibrain.core.lang.Language;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.utils.KnownSim;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Shilad Sen
 */
public class TestMostSimilarGuess {
    private MostSimilarGuess guess;

    @Before
    public void createGuess() {
        Language en = Language.getByLangCode("en");
        KnownMostSim sim = new KnownMostSim(Arrays.asList(
                new KnownSim("apple", "tart", 34, 99, 0.6, en),
                new KnownSim("apple", "orange", 34, 3, 0.9, en),
                new KnownSim("apple", "orange", 34, 3, 0.92, en),
                new KnownSim("apple", "black", 34, 188, 0.5, en),
                new KnownSim("apple", "shoe", 34, 39, 0.0, en),
                new KnownSim("apple", "honeycrisp", 34, 19, 0.95, en),
                new KnownSim("apple", "mac", 17, 2, 0.8, en)
        ));
        SRResultList list = new SRResultList(7);

        list.set(0, 2, 0.83);       // mac, rank 3, sim 0.8
        list.set(1, 39, 0.73);      // shoe, rank 6, sim 0.0
        list.set(2, 911, 0.70);     // unknown
        list.set(3, 19, 0.68);      // honeycrisp, rank 1, sim 0.95
        list.set(4, 13, 0.66);      // unknown
        list.set(5, 93, 0.62);      // unknown
        list.set(6, 3, 0.60);       // orange, rank 2, sim 0.91

        guess = new MostSimilarGuess(sim, list);
    }

    @Test
    public void testCreate() {
        List<MostSimilarGuess.Observation> obs = guess.getObservations();
        assertEquals(7, guess.getLength());
        assertEquals(4, obs.size());

        assertEquals(1, obs.get(0).rank);
        assertEquals(2, obs.get(0).id);
        assertEquals(0.83, obs.get(0).estimate, 0.001);

        assertEquals(7, obs.get(3).rank);
        assertEquals(3, obs.get(3).id);
        assertEquals(0.60, obs.get(3).estimate, 0.001);
    }

    @Test
    public void testSerialize() {
        String s = guess.toString();
        MostSimilarGuess guess2 = new MostSimilarGuess(guess.getKnown(), s);

        List<MostSimilarGuess.Observation> obs = guess2.getObservations();
        assertEquals(7, guess2.getLength());
        assertEquals(4, obs.size());

        assertEquals(1, obs.get(0).rank);
        assertEquals(2, obs.get(0).id);
        assertEquals(0.83, obs.get(0).estimate, 0.001);

        assertEquals(7, obs.get(3).rank);
        assertEquals(3, obs.get(3).id);
        assertEquals(0.60, obs.get(3).estimate, 0.001);

        MostSimilarGuess guess3 = new MostSimilarGuess(guess2.getKnown(), "3435|0.9|0.5");
        assertEquals(0, guess3.getObservations().size());
        assertEquals(3435, guess3.getLength());
    }

    @Test
    public void testNdgc() {
        double ndgc = (
                (0.80 + 0.00 / Math.log(2+1) + 0.95 / Math.log(4+1) + 0.91 / Math.log(7+1))
                        / (0.95 + 0.91 / Math.log(2+1) + 0.80 / Math.log(4+1) + 0.00 / Math.log(7+1)));
        assertEquals(ndgc, guess.getNDGC(), 0.001);
    }

    @Test
    public void testPenalizedNdgc() {
        int unobservedRank = guess.getLength() * 3;
        int unobservedCount = 2;
        double unobservedSim = 0.60 / 2;

        double s = (
                0.80 +
                0.00 / Math.log(2+1) +
                0.95 / Math.log(4+1) +
                0.91 / Math.log(7+1) +
                unobservedCount * unobservedSim / Math.log(unobservedRank + 1)
        );
        double t = (
                0.95 +
                0.91 / Math.log(2+1) +
                0.80 / Math.log(4+1) +
                0.60 / Math.log(7+1) +
                0.50 / Math.log(unobservedRank + 1) +
                0.0 / Math.log(unobservedRank + 1)
        );

        assertEquals(s / t, guess.getPenalizedNDGC(), 0.001);
    }

    @Test
    public void testPrecisionRecall() {
        PrecisionRecallAccumulator pr = guess.getPrecisionRecall(1, 0.7);
        assertEquals(pr.getN(), 1);
        assertEquals(1.0, pr.getPrecision(), 0.001);
        assertEquals(0.333333, pr.getRecall(), 0.001);
        pr = guess.getPrecisionRecall(2, 0.7);
        assertEquals(0.5, pr.getPrecision(), 0.001);
        assertEquals(0.333333, pr.getRecall(), 0.001);
        pr = guess.getPrecisionRecall(5, 0.7);
        assertEquals(0.6666, pr.getPrecision(), 0.001);
        assertEquals(0.6666, pr.getRecall(), 0.001);
    }
}
