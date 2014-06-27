package org.wikibrain.spatial;

import org.junit.Test;
import org.wikibrain.spatial.maxima.ScaleStratifier;
import org.wikibrain.spatial.maxima.SpatialConcept;
import org.wikibrain.spatial.maxima.SpatialConceptPair;
import org.wikibrain.spatial.maxima.SpatialConceptPairStratifier;

/**
 * Created by horla001 on 6/27/14.
 */
public class TestScaleStratifier {

    private SpatialConceptPairStratifier scaleStratifier;

    public TestScaleStratifier() {
        scaleStratifier = new ScaleStratifier();
    }

    public void testDesiredStratification() {
        double[] strat = scaleStratifier.getDesiredStratification();
        assert(strat.length == scaleStratifier.getNumBuckets());

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

        SpatialConcept.Scale[] scales = SpatialConcept.Scale.values();

        // Testing same scale
        // ========================

        c1.setScale(SpatialConcept.Scale.COUNTRY);
        c2.setScale(SpatialConcept.Scale.COUNTRY);
        assert(scaleStratifier.getStrata(pair) == 0);

        c1.setScale(SpatialConcept.Scale.STATE);
        c2.setScale(SpatialConcept.Scale.STATE);
        assert(scaleStratifier.getStrata(pair) == 1);

        c1.setScale(SpatialConcept.Scale.CITY);
        c2.setScale(SpatialConcept.Scale.CITY);
        assert(scaleStratifier.getStrata(pair) == 2);

        c1.setScale(SpatialConcept.Scale.LANDMARK);
        c2.setScale(SpatialConcept.Scale.LANDMARK);
        assert(scaleStratifier.getStrata(pair) == 3);

        c1.setScale(SpatialConcept.Scale.NATURAL);
        c2.setScale(SpatialConcept.Scale.NATURAL);
        assert(scaleStratifier.getStrata(pair) == 4);

        // Testing different scale
        // ========================

        c1.setScale(SpatialConcept.Scale.COUNTRY);
        c2.setScale(SpatialConcept.Scale.STATE);
        assert(scaleStratifier.getStrata(pair) == 5);

        c1.setScale(SpatialConcept.Scale.COUNTRY);
        c2.setScale(SpatialConcept.Scale.CITY);
        assert(scaleStratifier.getStrata(pair) == 6);

        c1.setScale(SpatialConcept.Scale.COUNTRY);
        c2.setScale(SpatialConcept.Scale.LANDMARK);
        assert(scaleStratifier.getStrata(pair) == 7);

        c1.setScale(SpatialConcept.Scale.COUNTRY);
        c2.setScale(SpatialConcept.Scale.NATURAL);
        assert(scaleStratifier.getStrata(pair) == 8);

        c1.setScale(SpatialConcept.Scale.STATE);
        c2.setScale(SpatialConcept.Scale.COUNTRY);
        assert(scaleStratifier.getStrata(pair) == 5);

        c1.setScale(SpatialConcept.Scale.STATE);
        c2.setScale(SpatialConcept.Scale.CITY);
        assert(scaleStratifier.getStrata(pair) == 9);

        c1.setScale(SpatialConcept.Scale.STATE);
        c2.setScale(SpatialConcept.Scale.LANDMARK);
        assert(scaleStratifier.getStrata(pair) == 10);

        c1.setScale(SpatialConcept.Scale.STATE);
        c2.setScale(SpatialConcept.Scale.NATURAL);
        assert(scaleStratifier.getStrata(pair) == 11);

        c1.setScale(SpatialConcept.Scale.CITY);
        c2.setScale(SpatialConcept.Scale.COUNTRY);
        assert(scaleStratifier.getStrata(pair) == 6);

        c1.setScale(SpatialConcept.Scale.CITY);
        c2.setScale(SpatialConcept.Scale.STATE);
        assert(scaleStratifier.getStrata(pair) == 9);

        c1.setScale(SpatialConcept.Scale.CITY);
        c2.setScale(SpatialConcept.Scale.LANDMARK);
        assert(scaleStratifier.getStrata(pair) == 12);

        c1.setScale(SpatialConcept.Scale.CITY);
        c2.setScale(SpatialConcept.Scale.NATURAL);
        assert(scaleStratifier.getStrata(pair) == 13);

        c1.setScale(SpatialConcept.Scale.LANDMARK);
        c2.setScale(SpatialConcept.Scale.COUNTRY);
        assert(scaleStratifier.getStrata(pair) == 7);

        c1.setScale(SpatialConcept.Scale.LANDMARK);
        c2.setScale(SpatialConcept.Scale.STATE);
        assert(scaleStratifier.getStrata(pair) == 10);

        c1.setScale(SpatialConcept.Scale.LANDMARK);
        c2.setScale(SpatialConcept.Scale.CITY);
        assert(scaleStratifier.getStrata(pair) == 12);

        c1.setScale(SpatialConcept.Scale.LANDMARK);
        c2.setScale(SpatialConcept.Scale.NATURAL);
        assert(scaleStratifier.getStrata(pair) == 14);

        c1.setScale(SpatialConcept.Scale.NATURAL);
        c2.setScale(SpatialConcept.Scale.COUNTRY);
        assert(scaleStratifier.getStrata(pair) == 8);

        c1.setScale(SpatialConcept.Scale.NATURAL);
        c2.setScale(SpatialConcept.Scale.STATE);
        assert(scaleStratifier.getStrata(pair) == 11);

        c1.setScale(SpatialConcept.Scale.NATURAL);
        c2.setScale(SpatialConcept.Scale.CITY);
        assert(scaleStratifier.getStrata(pair) == 13);

        c1.setScale(SpatialConcept.Scale.NATURAL);
        c2.setScale(SpatialConcept.Scale.LANDMARK);
        assert(scaleStratifier.getStrata(pair) == 14);
    }
}
