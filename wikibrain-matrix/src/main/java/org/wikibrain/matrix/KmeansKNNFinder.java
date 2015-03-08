package org.wikibrain.matrix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A fast neighborhood finder for dense vectors.
 *
 * @author Shilad Sen
 */
public class KmeansKNNFinder {
    private final DenseMatrix matrix;
    private int sampleSize = 50000;
    private int branchingFactor = 5;

    public KmeansKNNFinder(DenseMatrix matrix) {
        this.matrix = matrix;
    }

    public void train() throws IOException {
    }

    private List<DenseMatrixRow> getSample() throws IOException {
        List<Integer> ids = new ArrayList<Integer>();
        for (int id : matrix.getRowIds()) {
            ids.add(id);
        }
        Collections.shuffle(ids);
        if (ids.size() > sampleSize) {
            ids = ids.subList(0, sampleSize);
        }

        List<DenseMatrixRow> sample = new ArrayList<DenseMatrixRow>();
        for (int id : ids) {
            sample.add(matrix.getRow(id));
        }
        return sample;
    }

    class Node {
        float [] center;
        Node[] children = new Node[branchingFactor];
        List<DenseMatrixRow> members = new ArrayList<DenseMatrixRow>();
    }
}
