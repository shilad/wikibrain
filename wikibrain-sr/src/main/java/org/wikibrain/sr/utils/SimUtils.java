package org.wikibrain.sr.utils;

import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import org.apache.commons.lang3.ArrayUtils;
import org.wikibrain.lucene.WikiBrainScoreDoc;

import java.util.*;

/**
 *
 *
 */
public class SimUtils {

    public static double cosineSimilarity(TIntDoubleMap X, TIntDoubleMap Y) {
        double xDotX = 0.0;
        double yDotY = 0.0;
        double xDotY = 0.0;

        for (int id : X.keys()) {
            double x = X.get(id);
            xDotX += x * x;
            if (Y.containsKey(id)) {
                xDotY += x * Y.get(id);
            }
        }
        for (double y : Y.values()) {
            yDotY += y * y;
        }
        return xDotX * yDotY != 0 ? xDotY / Math.sqrt(xDotX * yDotY): 0.0;
    }
    public static double cosineSimilarity(TIntFloatMap X, TIntFloatMap Y) {
        double xDotX = 0.0;
        double yDotY = 0.0;
        double xDotY = 0.0;

        for (int id : X.keys()) {
            double x = X.get(id);
            xDotX += x * x;
            if (Y.containsKey(id)) {
                xDotY += x * Y.get(id);
            }
        }
        for (double y : Y.values()) {
            yDotY += y * y;
        }
        return xDotX * yDotY != 0 ? xDotY / Math.sqrt(xDotX * yDotY): 0.0;
    }

    public static double googleSimilarity(int sizeA, int sizeB, int intersection, int numTotal) {
        return 1.0 - (Math.log(Math.max(sizeA,sizeB))-Math.log(intersection))
                        / (Math.log(numTotal)-Math.log(Math.min(sizeA,sizeB)));
    }

    /**
     * Normalize a vector to unit length.
     * @param X
     * @return
     */
    public static TIntDoubleMap normalizeVector(TIntDoubleMap X) {
        TIntDoubleHashMap Y = new TIntDoubleHashMap();
        double sumSquares = 0.0;
        for (double x : X.values()) {
            sumSquares += x * x;
        }
        if (sumSquares != 0.0) {
            double norm = Math.sqrt(sumSquares);
            for (int id : X.keys()) {
                Y.put(id, X.get(id) / norm);
            }
            return Y;
        }
        return X;
    }
    /**
     * Normalize a vector to unit length.
     * @param X
     * @return
     */
    public static TIntFloatMap normalizeVector(TIntFloatMap X) {
        TIntFloatHashMap Y = new TIntFloatHashMap();
        double sumSquares = 0.0;
        for (double x : X.values()) {
            sumSquares += x * x;
        }
        if (sumSquares != 0.0) {
            double norm = Math.sqrt(sumSquares);
            for (int id : X.keys()) {
                Y.put(id, (float) (X.get(id) / norm));
            }
            return Y;
        }
        return X;
    }

    public static Map sortByValue(TIntDoubleHashMap unsortMap) {
        if (unsortMap.isEmpty()) {
            return new HashMap();
        }
        HashMap<Integer, Double> tempMap = new HashMap<Integer, Double>();
        TIntDoubleIterator iterator = unsortMap.iterator();
        for ( int i = unsortMap.size(); i-- > 0; ) {
            iterator.advance();
            tempMap.put( iterator.key(), iterator.value() );
        }
        List<Map.Entry> list = new LinkedList<Map.Entry>(tempMap.entrySet());

        // sort list based on comparator
        Collections.sort(list, Collections.reverseOrder(new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o1)).getValue())
                        .compareTo(((Map.Entry) (o2)).getValue());
            }
        }));

        Map sortedMap = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    /**
     * Prune a WikiBrainScoreDoc array.
     * @param wikibrainScoreDocs array of WikiBrainScoreDoc
     */
    public static WikiBrainScoreDoc[] pruneSimilar(WikiBrainScoreDoc[] wikibrainScoreDocs) {
        if (wikibrainScoreDocs.length == 0) {
            return wikibrainScoreDocs;
        }
        int cutoff = wikibrainScoreDocs.length;
        double threshold = 0.005 * wikibrainScoreDocs[0].score;
        for (int i = 0, j = 100; j < wikibrainScoreDocs.length; i++, j++) {
            float delta = wikibrainScoreDocs[i].score - wikibrainScoreDocs[j].score;
            if (delta < threshold) {
                cutoff = j;
                break;
            }
        }
        if (cutoff < wikibrainScoreDocs.length) {
//            LOG.info("pruned results from " + docs.scoreDocs.length + " to " + cutoff);
            wikibrainScoreDocs = ArrayUtils.subarray(wikibrainScoreDocs, 0, cutoff);
        }
        return wikibrainScoreDocs;
    }
}
