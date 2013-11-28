package org.wikapidia.sr.category;

import org.wikapidia.core.lang.Language;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * @author Matt Lesicko
 */
public class CategoryGraph implements Serializable{
    protected Map<Integer,Integer> catIndexes;
    protected Set<Integer> topLevelCategories;
    protected Language language;

    protected double[] catCosts;  // the cost of travelling through each category
    protected int[][] catParents;
    protected int[][] catPages;
    protected int[][] catChildren;
    protected String[] cats;
    protected double minCost = -1;

    public CategoryGraph(Language language){
        this.language=language;
    }

    public int getCategoryIndex(int catId) {
        if (catIndexes.containsKey(catId)) {
            return catIndexes.get(catId);
        } else {
            return -1;
        }
    }

    protected boolean isUsefulCat(int i) {
        return (catParents[i].length > 0 || catChildren[i].length > 0 || catPages[i].length > 1);
    }

    private static final double DAMPING_FACTOR = 0.85;
    protected double onePageRankIteration() {
        double nextRanks [] = new double[catCosts.length];
        Arrays.fill(nextRanks, (1.0 - DAMPING_FACTOR) / catCosts.length);
        for (int i = 0; i < catParents.length; i++) {
            int d = catParents[i].length;   // degree
            double pr = catCosts[i];    // current page-rank
            for (int j : catParents[i]) {
                nextRanks[j] += DAMPING_FACTOR * pr / d;
            }
        }
        double diff = 0.0;
        for (int i = 0; i < catParents.length; i++) {
            diff += Math.abs(catCosts[i] - nextRanks[i]);
        }
        catCosts = nextRanks;
        return diff;
    }

    protected String catIndexesToString(int indexes[]) {
        StringBuffer sb = new StringBuffer("[");
        for (int i : indexes) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(cats[i]);
            sb.append(" (id=");
            sb.append(""+i);
            sb.append(")");
        }
        return sb.append("]").toString();
    }
}
