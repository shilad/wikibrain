package org.wikibrain.spatial.maxima;

import java.util.*;

/**
 * Created by horla001 on 6/27/14.
 */
public class ScaleStratifier extends SpatialConceptPairStratifier {

    private static final int numBuckets = 15;
    private static final List<SpatialConcept.Scale> scaleOrder;
    private static final Map<String, Integer> bucketMap;

    static {
        scaleOrder = new ArrayList<SpatialConcept.Scale>();
        scaleOrder.add(SpatialConcept.Scale.COUNTRY);
        scaleOrder.add(SpatialConcept.Scale.STATE);
        scaleOrder.add(SpatialConcept.Scale.CITY);
        scaleOrder.add(SpatialConcept.Scale.LANDMARK);
        scaleOrder.add(SpatialConcept.Scale.NATURAL);

        bucketMap = new HashMap<String, Integer>();
        bucketMap.put("COUNTRY.COUNTRY", 0);
        bucketMap.put("STATE.STATE", 1);
        bucketMap.put("CITY.CITY", 2);
        bucketMap.put("LANDMARK.LANDMARK", 3);
        bucketMap.put("NATURAL.NATURAL", 4);
        bucketMap.put("COUNTRY.STATE", 5);
        bucketMap.put("COUNTRY.CITY", 6);
        bucketMap.put("COUNTRY.LANDMARK", 7);
        bucketMap.put("COUNTRY.NATURAL", 8);
        bucketMap.put("STATE.CITY", 9);
        bucketMap.put("STATE.LANDMARK", 10);
        bucketMap.put("STATE.NATURAL", 11);
        bucketMap.put("CITY.LANDMARK", 12);
        bucketMap.put("CITY.NATURAL", 13);
        bucketMap.put("LANDMARK.NATURAL", 14);

    }

    @Override
    public int getStrata(SpatialConceptPair conceptPair) {
        String key = keyForSet(conceptPair);
        return bucketMap.get(key);
    }

    @Override
    public double[] getDesiredStratification() {
        double[] strats = new double[numBuckets];
        for(int i = 0; i < 15; i++) {
            double val = i < 5 ? 0.1 : 0.05;
            strats[i] = val;
        }

        return strats;
    }

    @Override
    public String getName() {
        return "Scale";
    }

    @Override
    public int getNumBuckets() {
        return numBuckets;
    }

    private List<SpatialConcept.Scale> listFromConceptPair(SpatialConceptPair spatialConceptPair) {
        List<SpatialConcept.Scale> list = new ArrayList<SpatialConcept.Scale>();
        list.add(spatialConceptPair.getFirstConcept().getScale());
        list.add(spatialConceptPair.getSecondConcept().getScale());
        return list;
    }

    private String stringForScale(SpatialConcept.Scale scale) {
        switch(scale) {
            case COUNTRY: return "COUNTRY";
            case STATE: return "STATE";
            case CITY: return "CITY";
            case LANDMARK: return "LANDMARK";
            case NATURAL: return "NATURAL";
            default: return null;
        }
    }

    private String keyForSet(SpatialConceptPair spatialConceptPair) {
        // COUNTRY STATE CITY LANDMARK NATURAL

        List<SpatialConcept.Scale> scales = listFromConceptPair(spatialConceptPair);


        Collections.sort(scales, new Comparator<SpatialConcept.Scale>() {
            public int compare(SpatialConcept.Scale scale1, SpatialConcept.Scale scale2) {
                if(scaleOrder.indexOf(scale1) > scaleOrder.indexOf(scale2)) {
                    return 1;
                }

                return scale1 == scale2 ? 0 : -1;
            }
        });

        return stringForScale(scales.get(0)) + "." + stringForScale(scales.get(1));
    }

    public static void main(String[] args) {
        SpatialConceptPair p = new SpatialConceptPair(new SpatialConcept(0, "N"), new SpatialConcept(0, "YO"));
        p.getFirstConcept().setScale(SpatialConcept.Scale.CITY);
        p.getSecondConcept().setScale(SpatialConcept.Scale.NATURAL);

        SpatialConceptPairStratifier strat = new ScaleStratifier();
        System.out.println(strat.getStrata(p));
    }
}
