package org.wikapidia.sr;

import gnu.trove.map.hash.TIntFloatHashMap;
import org.apache.commons.collections.iterators.ArrayIterator;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class SRResultList implements  Iterable<SRResult>{
    private SRResult[] results;
    private int numDocs;
    private double missingScore;    // score for missing documents.
    private float scores[];         // performance optimization

    public SRResultList(int maxNumDocs) {
        this.results = new SRResult[maxNumDocs];
        for (int i = 0; i < this.results.length; i++) {
            results[i] = new SRResult();
        }
        numDocs = maxNumDocs;
    }

    public int numDocs() {
        return numDocs;
    }

    public int getIndexForId(int id) {
        for (int i = 0; i < numDocs(); i++) {
            if (results[i].id == id) {
                return i;
            }
        }
        return -1;
    }

    public double getScoreForId(int id) {
        for (int i = 0; i < numDocs(); i++) {
            if (results[i].id == id) {
                return results[i].getValue();
            }
        }
        return Double.NaN;
    }
    public int getId(int i) {
        assert(i < numDocs);
        return results[i].id;
    }

    public double getScore(int i) {
        assert(i < numDocs);
        return results[i].getValue();
    }

    public void truncate(int numDocs) {
        assert(numDocs <= results.length);
        this.numDocs = numDocs;
    }

    public void set(int i, int id, double score) {
        assert(i < numDocs);
        results[i].id = id;
        results[i].value = score;
    }

    public void set (int i, int id, double score, List<Explanation> explanationList){
        assert (i<numDocs);
        results[i].id = id;
        results[i].value = score;
        results[i].explanations = explanationList;
    }

    public int[] getIds() {
        int ids[] = new int[numDocs];
        for (int i = 0; i < numDocs; i++) {
            ids[i] = results[i].id;
        }
        return ids;
    }

    public float[] getScoresAsFloat() {
        if (scores == null) {
            scores = new float[numDocs];
            for (int i = 0; i < numDocs; i++) {
                scores[i] = (float) results[i].getValue();
            }
        }
        return scores;
    }

    public TIntFloatHashMap asTroveMap() {
        TIntFloatHashMap map = new TIntFloatHashMap();
        for (int i = 0; i < numDocs; i++) {
            map.put(results[i].id, (float) results[i].getValue());
        }
        return map;
    }

    public void makeUnitLength() {
        double length = 0.0;
        for (int i = 0; i < numDocs; i++) {
            double x = results[i].getValue();
            length += x * x;
        }
        if (length != 0) {
            length = Math.sqrt(length);
            for (int i = 0; i < numDocs; i++) {
                results[i].value /= length;
            }
        }
    }

    public void sort() {
        Arrays.sort(results, 0, numDocs);
    }

    @Override
    public Iterator<SRResult> iterator() {
        return new ArrayIterator(results, 0, numDocs);
    }

    public SRResult get(int i) {
        return results[i];
    }

    /**
     * @return Estimated similarity score for documents that are missing.
     */
    public double getMissingScore() {
        return missingScore;
    }

    public void setMissingScore(double missingScore) {
        this.missingScore = missingScore;
    }
}
