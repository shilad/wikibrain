package org.wikibrain.matrix.knn;

/**
* @author Shilad Sen
*/
public class Neighborhood {
    private final double [] scores;
    private final int ids[];

    public Neighborhood(int[] ids, double[] scores) {
        for (int i = 1; i < scores.length; i++) {
            if (scores[i-1] < scores[i]) {
                throw new IllegalArgumentException();
            }
        }
        this.ids = ids;
        this.scores = scores;
    }

    public double[] getScores() {
        return scores;
    }

    public int[] getIds() {
        return ids;
    }

    public int getId(int i) {
        return ids[i];
    }

    public double getScore(int i) {
        return scores[i];
    }

    public int size() {
        return scores.length;
    }
}
