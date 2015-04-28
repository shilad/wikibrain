package org.wikibrain.matrix.knn;

import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikibrain.matrix.DenseMatrix;
import org.wikibrain.matrix.DenseMatrixRow;

import java.io.*;
import java.util.Arrays;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Brute force implementations of knn for a dense matrix.
 *
 * @author Shilad Sen
 */
public class BruteForceKNNFinder implements KNNFinder {
    private static final Logger LOG = LoggerFactory.getLogger(BruteForceKNNFinder.class);

    private final DenseMatrix matrix;

    public BruteForceKNNFinder(DenseMatrix matrix) throws IOException {
        this.matrix = matrix;
    }

    @Override
    public Neighborhood query(final float[] vector, int k, int maxTraversal, TIntSet validIds) {
        final NeighborhoodAccumulator accum = new NeighborhoodAccumulator(k);
        if (validIds == null) {
            for (DenseMatrixRow row : matrix) {
                double sim = row.dot(vector);
                accum.visit(row.getRowIndex(), sim);
            }
        } else {
            validIds.forEach(new TIntProcedure() {
                @Override
                public boolean execute(int id) {
                    DenseMatrixRow row = null;
                    try {
                        row = matrix.getRow(id);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    double sim = KmeansKNNFinder.cosine(vector, row);
                    accum.visit(id, sim);
                    return true;
                }
            });
        }
        return accum.get();
    }


    @Override
    public void build() throws IOException {}
    @Override
    public void save(File path) throws IOException {}
    @Override
    public boolean load(File path) throws IOException { return true; }


}
