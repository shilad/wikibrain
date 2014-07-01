package org.wikibrain.spatial;

import org.junit.Test;
import org.wikibrain.spatial.maxima.SpatialConcept;
import org.wikibrain.spatial.maxima.SpatialConceptPair;
import org.wikibrain.spatial.maxima.SpatialConceptPairStratifier;
import org.wikibrain.spatial.maxima.StraightlineStratifier;

/**
 * Created by horla001 on 6/27/14.
 */
public class TestStraightlineStratifier {

    private SpatialConceptPairStratifier straightlineStratifier;

    public TestStraightlineStratifier() {
        straightlineStratifier = new StraightlineStratifier();
    }

    @Test
    public void testDesiredStratification() {
        double[] strat = straightlineStratifier.getDesiredStratification();
        assert(strat.length == straightlineStratifier.getNumBuckets());

        double incr = 0;
        for(double d : strat) {
            incr += d;
        }

        assert(incr == 1.0);
    }

    @Test
    public void testStratification() {
        SpatialConcept c1 = new SpatialConcept(0, "");
        SpatialConcept c2 = new SpatialConcept(0, "");
        SpatialConceptPair pair = new SpatialConceptPair(c1, c2);

        for(int i = 0; i < 1000; i++) {
            int tb = 0;
            if(i > 99) {
                tb = i < 500 ? 1 : 2;
            }

            pair.setKmDistance(i);
            assert(tb == straightlineStratifier.getStrata(pair));
        }
    }
}
