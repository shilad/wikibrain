package org.wikibrain.pageview;

import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Random;
import java.util.SortedSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Shilad Sen
 */
public class TestPageViewUtils {
    @Test
    public void testTstampsInRange() {

        long now = System.currentTimeMillis();
        Random random = new Random();
        for (int i = 0; i < 1000; i++) {
            long tstamp = (long) (random.nextDouble() * now);
            DateTime beg = new DateTime(tstamp);
            DateTime end = beg.plusHours(1);
            SortedSet<DateTime> tstamps = PageViewUtils.timestampsInInterval(beg, end);
            assertEquals(tstamps.size(), 1);
            DateTime dt = tstamps.first();
            assertTrue(beg.isBefore(dt));
            assertTrue(end.isAfter(dt));
        }
    }
}
