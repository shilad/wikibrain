package org.wikibrain.sr;

import edu.emory.mathcs.backport.java.util.Collections;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import org.apache.commons.collections.iterators.ArrayIterator;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class SRResultList implements  Iterable<SRResult>{
    private SRResult[] results;
    private int numDocs;
    private double missingScore;    // score for missing documents.
    private int missingRank;
    private float[] scores;         // performance optimization

    public SRResultList(int maxNumDocs) {
        this.results = new SRResult[maxNumDocs];
        for (int i = 0; i < this.results.length; i++) {
            results[i] = new SRResult();
        }
        numDocs = maxNumDocs;
    }

    public double minScore() {
        return (numDocs == 0) ? 0.0 : this.results[numDocs-1].getScore();
    }

    public double maxScore() {
        return (numDocs == 0) ? 0.0 : this.results[0].getScore();
    }

    /**
     * Returns the specified number of docs in this list.
     * Unless a call to truncate has been made, this will be
     * the max number of documents specified in the constructor.
     * @return
     */
    public int numDocs() {
        return numDocs;
    }

    /**
     * Truncates the list to the specified size.
     * @param numDocs
     */
    public void truncate(int numDocs) {
        assert(numDocs <= results.length);
        this.numDocs = numDocs;
    }

    /**
     * Returns the index of the specified ID, or -1 if not found.
     * @param id
     * @return
     */
    public int getIndexForId(int id) {
        for (int i = 0; i < numDocs(); i++) {
            if (results[i].id == id) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the score for the specified ID, or Double.NaN if not found.
     * @param id
     * @return
     */
    public double getScoreForId(int id) {
        for (int i = 0; i < numDocs(); i++) {
            if (results[i].id == id) {
                return results[i].getScore();
            }
        }
        return Double.NaN;
    }

    /**
     * Returns the ID of the specified index.
     * @param i
     * @return
     */
    public int getId(int i) {
        assert(i < numDocs);
        return results[i].id;
    }

    public void setId(int i, int id) {
        results[i].id = id;
    }

    /**
     * Returns an array of the IDs in this list.
     * @return
     */
    public int[] getIds() {
        int[] ids = new int[numDocs];
        for (int i = 0; i < numDocs; i++) {
            ids[i] = results[i].id;
        }
        return ids;
    }

    /**
     * Returns the score of the specified index.
     * @param i
     * @return
     */
    public double getScore(int i) {
        assert(i < numDocs);
        return results[i].getScore();
    }

    /**
     * Returns an array of scores in this list.
     * @return
     */
    public double[] getScores() {
        double[] scores = new double[numDocs];
        for (int i = 0; i < numDocs; i++) {
            scores[i] = results[i].score;
        }
        return scores;
    }

    /**
     * Returns an array of scores in this list as float values.
     * @return
     */
    public float[] getScoresAsFloat() {
        if (scores == null) {
            scores = new float[numDocs];
            for (int i = 0; i < numDocs; i++) {
                scores[i] = (float) results[i].getScore();
            }
        }
        return scores;
    }

    /**
     * Sets the ID and score of the SRResult at the index.
     * Note that this does not affect that result's explanations.
     * @param i
     * @param id
     * @param score
     */
    public void set(int i, int id, double score) {
        assert(i < numDocs);
        results[i].id = id;
        results[i].score = score;
    }

    /**
     * Sets the SRResut at the index to the new SRResult.
     * @param i
     * @param result
     */
    public void set(int i, SRResult result){
        assert(i < numDocs);
        results[i] = result;
    }

    /**
     * Sets the ID, score, and explanations of the SRResult at the index.
     * @param i
     * @param id
     * @param score
     * @param explanationList
     */
    public void set(int i, int id, double score, List<Explanation> explanationList){
        set(i, new SRResult(id, score, explanationList));
    }

    /**
     * Returns this list as a TIntFloatMap.
     * Note that this does not maintain any order.
     * @return
     */
    public TIntFloatMap asTroveMap() {
        TIntFloatHashMap map = new TIntFloatHashMap();
        for (int i = 0; i < numDocs; i++) {
            map.put(results[i].id, (float) results[i].getScore());
        }
        return map;
    }

    /**
     * Normalizes the score vector of this list to a unit length.
     */
    public void makeUnitLength() {
        double length = 0.0;
        for (int i = 0; i < numDocs; i++) {
            double x = results[i].getScore();
            length += x * x;
        }
        if (length != 0) {
            length = Math.sqrt(length);
            for (int i = 0; i < numDocs; i++) {
                results[i].score /= length;
            }
        }
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < numDocs(); i++) {
            if (i > 0) builder.append(" ");
            builder.append(
                    String.format("%d. %d=%.3f", (i+1), results[i].getId(), results[i].getScore())
            );
        }
        return builder.toString();
    }

    /**
     * Sorts the SRResults in this list in ascending order.
     */
    public void sortAscending() {
        Arrays.sort(results, 0, numDocs);
    }

    /**
     * Sorts the SRResults in this list in descending order.
     */
    public void sortDescending() {
        Arrays.sort(results, 0, numDocs, Collections.reverseOrder());
    }

    /**
     * Sorts by id, ascending.
     */
    public void sortById() {
        Arrays.sort(results, 0, numDocs, new Comparator<SRResult>() {
            @Override
            public int compare(SRResult o1, SRResult o2) {
                return o1.getId() - o2.getId();
            }
        });
    }

    /**
     * Returns the SRResult at the specified index.
     * @param i
     * @return
     */
    public SRResult get(int i) {
        return results[i];
    }

    /**
     * Returns the estimated similarity score for missing documents.
     * @return
     */
    public double getMissingScore() {
        return missingScore;
    }

    /**
     * Sets the estimated similarity score for missing documents
     * @param missingScore
     */
    public void setMissingScore(double missingScore) {
        this.missingScore = missingScore;
    }

    public void setMissingRank(int missingRank) {
        this.missingRank = missingRank;
    }

    @Override
    public Iterator<SRResult> iterator() {
        return new ArrayIterator(results, 0, numDocs);
    }
}
