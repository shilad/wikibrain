package org.wikibrain.matrix.knn;

import gnu.trove.set.TIntSet;

import java.io.File;
import java.io.IOException;

/**
 * @author Shilad Sen
 */
public class LSHForestKNNFinder implements KNNFinder {
    @Override
    public void build() throws IOException {

    }

    @Override
    public Neighborhood query(float[] vector, int k, int maxTraversal, TIntSet validIds) {
        return null;
    }

    @Override
    public void save(File path) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean load(File path) throws IOException {
        throw new UnsupportedOperationException();
    }
}
