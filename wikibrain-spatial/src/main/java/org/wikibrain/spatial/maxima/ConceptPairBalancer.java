package org.wikibrain.spatial.maxima;

import java.util.*;

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

        if(candidates.size() <= numCount) {
            return candidates;
        }

        stratifierInfos = new RunningStratifierInformation[3];
        stratifierInfos[0] = new RunningStratifierInformation(SemanticRelatednessStratifier.class, chosen);
        stratifierInfos[1] = new RunningStratifierInformation(ScaleStratifier.class, chosen);
        stratifierInfos[2] = new RunningStratifierInformation(Math.random() < 0.5 ? StraightlineStratifier.class : TopologicalStratifier.class, chosen);

        List<SpatialConceptPair> newConcepts = new ArrayList<SpatialConceptPair>();
        List<SpatialConceptPair> candidateTemp = new LinkedList<SpatialConceptPair>(candidates);

        while(newConcepts.size() < numCount) {
            scoreAndOrder(candidateTemp);

            SpatialConceptPair best = candidateTemp.remove(0);;
            newConcepts.add(best);
        }

        return newConcepts;
    }

    private List<SpatialConceptPair> scoreAndOrder(List<SpatialConceptPair> candidates) {
        List<SpatialConceptPair> scored = new LinkedList<SpatialConceptPair>();

        for(SpatialConceptPair pair : scored) {
            double score = 0.0;
            for(RunningStratifierInformation info : stratifierInfos) {
                score += info.calculateScore(pair);
            }

            pair.setScore(score);
        }

        Collections.sort(scored, new Comparator<SpatialConceptPair>() {
            @Override
            public int compare(SpatialConceptPair o1, SpatialConceptPair o2) {
                double a = o1.getScore();
                double b = o2.getScore();

                if(a > b) {
                    return -1;
                }

                return a == b ? 0 : 1;
            }
        });

        return scored;
    }

}
