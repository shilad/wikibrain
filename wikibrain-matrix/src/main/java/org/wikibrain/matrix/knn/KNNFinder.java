package org.wikibrain.matrix.knn;

import gnu.trove.set.TIntSet;

import java.io.File;
import java.io.IOException;

/**
 * @author Shilad Sen
 */
public interface KNNFinder {
    public void build() throws IOException;
    public Neighborhood query(float[] vector, int k, int maxTraversal, TIntSet validIds);
    public void save(File path) throws IOException;
    public boolean load(File path) throws IOException;
}
