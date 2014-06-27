package org.wikibrain.spatial;

import org.junit.Test;
import org.wikibrain.spatial.maxima.*;

/**
 * Created by horla001 on 6/27/14.
 */
public class TestTopologicalStratifier {

    private SpatialConceptPairStratifier topologicalStratifier;

    public TestTopologicalStratifier() {
        topologicalStratifier = new TopologicalStratifier();
    }

    @Test
    public void testDesiredStratification() {
        double[] strat = topologicalStratifier.getDesiredStratification();
        assert(strat.length == topologicalStratifier.getNumBuckets());

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

        for(int i = 0; i < 50; i++) {
            int tb = 0;
            if(i > 2) {
                tb = i < 6 ? 1 : 2;
            }

            pair.setTopDistance(i);
            assert(tb == topologicalStratifier.getStrata(pair));
        }
    }
}
