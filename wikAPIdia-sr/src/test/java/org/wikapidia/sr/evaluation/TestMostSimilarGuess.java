package org.wikapidia.sr.evaluation;

import org.junit.Before;
import org.junit.Test;
import org.wikapidia.core.lang.Language;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.utils.KnownSim;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

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
        assertEquals(0.83, obs.get(0).score, 0.001);

        assertEquals(7, obs.get(3).rank);
        assertEquals(3, obs.get(3).id);
        assertEquals(0.60, obs.get(3).score, 0.001);
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
        assertEquals(0.83, obs.get(0).score, 0.001);

        assertEquals(7, obs.get(3).rank);
        assertEquals(3, obs.get(3).id);
        assertEquals(0.60, obs.get(3).score, 0.001);

        MostSimilarGuess guess3 = new MostSimilarGuess(guess2.getKnown(), "3435|");
        assertEquals(0, guess3.getObservations().size());
        assertEquals(3435, guess3.getLength());
    }
}
