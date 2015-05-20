package org.wikibrain.sr.phrasesim;

import gnu.trove.list.TIntList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.utils.Leaderboard;

import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A cosimilarity matrix that is dense, but can be expanded.
 *
 * @author Shilad Sen
 */
public class CosimilarityMatrix implements Serializable {
    private static final double EXPANSION_FRACTION = 1.3;

    private static final Logger LOGGER = LoggerFactory.getLogger(CosimilarityMatrix.class);

    private float[][] matrix = new float[0][0];
    private TIntIntMap sparse2Dense = new TIntIntHashMap();
    private int[] dense2Sparse = new int[0];
    private boolean [] completed = new boolean[0];

    public synchronized void update(int sparseId, SRResultList neighbors) {
        int denseId;
        if (sparse2Dense.containsKey(sparseId)) {
            denseId = sparse2Dense.get(sparseId);
        } else {
            expandIfNecessary();
            denseId = sparse2Dense.size();
            sparse2Dense.put(sparseId, denseId);
            dense2Sparse[denseId] = sparseId;
        }

        for (SRResult r : neighbors) {
            if (!sparse2Dense.containsKey(r.getId())) continue;
            int denseId2 = sparse2Dense.get(r.getId());
            matrix[denseId][denseId2] = (float) r.getScore();
            matrix[denseId2][denseId] = (float) r.getScore();
        }
        completed[denseId] = true;
    }

    public float[] getVector(int id) {
        if (sparse2Dense.containsKey(id)) {
            return matrix[sparse2Dense.get(id)];
        } else {
            return null;
        }
    }

    public synchronized int size() {
        return sparse2Dense.size();
    }

    public SRResultList mostSimilar(int id, int maxResults, TIntSet candidateIds) {
        int n;
        int denseId;
        boolean denseCandidateIds[] = null;
        synchronized (this) {
            n = sparse2Dense.size();    // this can be lock free because of trove's implementation
            denseId = sparse2Dense.get(id);
            if (candidateIds != null) {
                denseCandidateIds = new boolean[candidateIds.size()];
                final boolean[] finalDenseCandidateIds = denseCandidateIds;
                candidateIds.forEach(new TIntProcedure() {
                    @Override
                    public boolean execute(int id2) {
                        finalDenseCandidateIds[sparse2Dense.get(id2)] = true;
                        return true;
                    }
                });
            }
        }
        Leaderboard top = new Leaderboard(maxResults);
        for (int i = 0; i < n; i++) {
            if (denseCandidateIds == null || denseCandidateIds[i]) {
                top.tallyScore(i, matrix[denseId][i]);
            }
        }
        SRResultList results = top.getTop();
        for (int i = 0; i < results.numDocs(); i++) {
            results.setId(i, dense2Sparse[results.getId(i)]);
        }
        return results;
    }


    public double[][] cosimilarity(int rows[], int columns[]) {
        double cosims[][] = new double[rows.length][columns.length];
        int denseRowIds[] = new int[rows.length];
        int denseColIds[] = new int[columns.length];
        synchronized (this) {
            for (int i = 0; i < rows.length; i++) {
                int rowId = rows[i];
                denseRowIds[i] = sparse2Dense.containsKey(rowId) ? sparse2Dense.get(rowId) : -1;
            }
            for (int i = 0; i < columns.length; i++) {
                int colId = columns[i];
                denseColIds[i] = sparse2Dense.containsKey(colId) ? sparse2Dense.get(colId) : -1;
            }
        }
        for (int i = 0; i < denseRowIds.length; i++) {
            for (int j = 0; j < denseColIds.length; j++) {
                if (denseRowIds[i] >= 0 && denseColIds[j] >= 0) {
                    cosims[i][j] = matrix[denseRowIds[i]][denseColIds[j]];
                }
            }
        }
        return cosims;
    }


    private synchronized void expandIfNecessary() {
        if (sparse2Dense.size() < dense2Sparse.length) {
            return;
        }
        if (dense2Sparse.length != sparse2Dense.size()) {
            throw new IllegalStateException();
        }
        if (dense2Sparse.length != matrix.length) {
            throw new IllegalStateException();
        }
        if (dense2Sparse.length > 0 && dense2Sparse.length != matrix[0].length) {
            throw new IllegalStateException();
        }
        int oldn = sparse2Dense.size();
        final int n = (int) (Math.max(500, oldn) * EXPANSION_FRACTION);
        LOGGER.info("expanding cosimilarity matrix to length " + n);
        float newCosims[][] = new float[n][n];
        for (int i = 0; i < oldn; i++) {
            System.arraycopy(matrix[i], 0, newCosims[i], 0, oldn);
        }
        int newDense2Sparse[] = new int[n];
        boolean newCompleted[] = new boolean[n];
        System.arraycopy(dense2Sparse, 0, newDense2Sparse, 0, oldn);
        System.arraycopy(newCompleted, 0, newCompleted, 0, oldn);
        matrix = newCosims;
        dense2Sparse = newDense2Sparse;
        completed = newCompleted;

        LOGGER.info("finished expanding cosimilarity matrix");
    }

    public float similarity(int id1, int id2) {
        if (sparse2Dense.containsKey(id1) && sparse2Dense.containsKey(id2)) {
            return matrix[sparse2Dense.get(id1)][sparse2Dense.get(id2)];
        } else {
            return 0f;
        }
    }

    /**
     * Returns all ids whose "update" method has successfully completed.
     * @return
     */
    public TIntSet getCompleted() {
        TIntSet result = new TIntHashSet();
        for (int i = 0; i < completed.length; i++) {
            if (completed[i]) {
                result.add(dense2Sparse[i]);
            }
        }
        return result;
    }

}
