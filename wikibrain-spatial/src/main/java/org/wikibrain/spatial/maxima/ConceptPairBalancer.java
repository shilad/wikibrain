package org.wikibrain.spatial.maxima;

import java.util.List;

/**
 * Created by horla001 on 6/27/14.
 */
public class ConceptPairBalancer {

    private RunningStratifierInformation[] stratifierInfos;

    private class RunningStratifierInformation {
        private int[] runningTotal;
        private int absoluteTotal;
        private double[] goal;
        private SpatialConceptPairStratifier stratifier;

        public RunningStratifierInformation(Class stratifierClass, List<SpatialConceptPair> previous) {
            try {
                stratifier = (SpatialConceptPairStratifier)stratifierClass.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            runningTotal = new int[stratifier.getNumBuckets()];
            goal = stratifier.getDesiredStratification();

            for(SpatialConceptPair pair : previous) {
                addToTotal(pair);
            }
        }

        public void addToTotal(SpatialConceptPair pair) {
            int b = stratifier.getStrata(pair);
            runningTotal[b]++;
            absoluteTotal++;
        }

        public double calculateScore(SpatialConceptPair pair) {
            double distance = 0;
            for(int i = 0; i < runningTotal.length; i++) {
                double curr = (double)runningTotal[i] / (double)absoluteTotal;
                distance += Math.abs(goal[i] - curr);
            }

            return 1 - (distance / 2);
        }
    }

    /**
     * Given a list of candidate pairs and previously chosen pairs, choose candidates
     * from the given candidates that move us closer to our goal.
     * @param candidates The candidate list of pairs
     * @param chosen The previously chosen pairs
     * @param numCount The number of pairs to choose from candidate
     * @return A new list of pairs chosen from the candidates.
     */
    public List<SpatialConceptPair> choosePairs(List<SpatialConceptPair> candidates, List<SpatialConceptPair> chosen, int numCount) {
        return null;
    }

}
