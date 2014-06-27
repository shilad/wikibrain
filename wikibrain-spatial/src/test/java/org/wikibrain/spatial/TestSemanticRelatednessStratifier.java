package org.wikibrain.spatial;

import org.junit.Test;
import org.wikibrain.spatial.maxima.SemanticRelatednessStratifier;
import org.wikibrain.spatial.maxima.SpatialConcept;
import org.wikibrain.spatial.maxima.SpatialConceptPair;
import org.wikibrain.spatial.maxima.SpatialConceptPairStratifier;

/**
 * Created by horla001 on 6/27/14.
 */
public class TestSemanticRelatednessStratifier {

    private SpatialConceptPairStratifier srStratifier;

    public TestSemanticRelatednessStratifier() {
        srStratifier = new SemanticRelatednessStratifier();
    }

    public void testDesiredStratification() {
        double[] strat = srStratifier.getDesiredStratification();
        assert(strat.length == srStratifier.getNumBuckets());

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

        for(double d = 0; d < 1.0; d += 0.01) {
            int tb = d < 0.5 ? 0 : 1;

            pair.setRelatedness(d);
            assert(tb == srStratifier.getStrata(pair));
        }
    }
}
