package org.wikibrain.matrix.knn;

import java.io.IOException;

/**
 * @author Shilad Sen
 */
public interface KNNFinder {
    void build() throws IOException;

    Neighborhood query(float[] vector, int k, int maxTraversal);
}
