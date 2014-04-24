package org.wikibrain.sr.evaluation;

import org.junit.Before;
import org.junit.Test;
import org.wikibrain.core.lang.Language;
import org.wikibrain.sr.utils.KnownSim;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author Shilad Sen
 */
public class TestKnownMostSim {
    private KnownMostSim mostSim;

    @Before
    public void makeMostSim() {
        Language en = Language.getByLangCode("en");
        mostSim = new KnownMostSim(Arrays.asList(
                new KnownSim("apple", "tart", 34, 99, 0.6, en),
                new KnownSim("apple", "orange", 34, 3, 0.9, en),
                new KnownSim("apple", "orange", 34, 3, 0.92, en),
                new KnownSim("apple", "black", 34, 188, 0.5, en),
                new KnownSim("apple", "shoe", 34, 39, 0.0, en),
                new KnownSim("apple", "honeycrisp", 34, 19, 0.95, en),
                new KnownSim("apple", "mac", 17, 3, 0.8, en)
        ));
    }

    @Test
    public void testCreate() {
        assertEquals(6, mostSim.getMostSimilar().size());

        assertEquals(34, mostSim.getPageId());
        assertEquals(34, mostSim.getMostSimilar().get(0).wpId1);
        assertEquals("apple", mostSim.getMostSimilar().get(0).phrase1);

        assertEquals("honeycrisp", mostSim.getMostSimilar().get(0).phrase2);
        assertEquals(19, mostSim.getMostSimilar().get(0).wpId2);
        assertEquals(0.95, mostSim.getMostSimilar().get(0).similarity, 0.001);

        assertEquals("orange", mostSim.getMostSimilar().get(1).phrase2);
        assertEquals(3, mostSim.getMostSimilar().get(1).wpId2);
        assertEquals(0.91, mostSim.getMostSimilar().get(1).similarity, 0.001);

        assertEquals("mac", mostSim.getMostSimilar().get(2).phrase2);
        assertEquals(3, mostSim.getMostSimilar().get(2).wpId2);
        assertEquals(0.8, mostSim.getMostSimilar().get(2).similarity, 0.001);

        assertEquals("shoe", mostSim.getMostSimilar().get(5).phrase2);
        assertEquals(39, mostSim.getMostSimilar().get(5).wpId2);
        assertEquals(0.0, mostSim.getMostSimilar().get(5).similarity, 0.001);
    }


    @Test
    public void testThreshold() {
        KnownMostSim highest = mostSim.getAboveThreshold(0.6);
        assertEquals(4, highest.getMostSimilar().size());

        assertEquals("honeycrisp", mostSim.getMostSimilar().get(0).phrase2);
        assertEquals(19, mostSim.getMostSimilar().get(0).wpId2);
        assertEquals(0.95, mostSim.getMostSimilar().get(0).similarity, 0.001);

        assertEquals("tart", mostSim.getMostSimilar().get(3).phrase2);
        assertEquals(99, mostSim.getMostSimilar().get(3).wpId2);
        assertEquals(0.6, mostSim.getMostSimilar().get(3).similarity, 0.001);
    }
}
