package org.wikibrain.sr.factorization;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import no.uib.cipr.matrix.AbstractMatrix;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;
import no.uib.cipr.matrix.sparse.SparseVector;
import org.wikibrain.matrix.SparseMatrix;
import org.wikibrain.matrix.SparseMatrixRow;

import java.io.IOException;
import java.util.Iterator;

/**
 * @author Shilad Sen
 */
public class WBSparseMatrix extends FlexCompRowMatrix {
    // Dense to sparse mapping of row / column ids
    private int dense2SparseRow[];
    private int dense2SparseCol[];

    // Sparse to dense mapping of row / column ids
    private TIntIntMap sparse2DenseRow;
    private TIntIntMap sparse2DenseCol;

    private final SparseMatrix transpose;
    private final SparseMatrix matrix;

    public WBSparseMatrix(SparseMatrix matrix, SparseMatrix transpose) {
        super(matrix.getNumRows(), transpose.getNumRows());
//        System.out.println("dimensions are " + numRows + ", " + numColumns);
        this.matrix = matrix;
        this.transpose = transpose;

        dense2SparseRow = matrix.getRowIds();
        dense2SparseCol = transpose.getRowIds();

        sparse2DenseRow = makeSparseToDenseMap(matrix.getRowIds());
        sparse2DenseCol = makeSparseToDenseMap(transpose.getRowIds());

        for (int i = 0; i < dense2SparseRow.length; i++) {
            SparseMatrixRow row = null;
            try {
                row = matrix.getRow(dense2SparseRow[i]);
            } catch (IOException e) {
                throw new IllegalArgumentException();
            }

            int ids[] = new int[row.getNumCols()];
            double vals[] = new double[row.getNumCols()];
            for (int j = 0; j < row.getNumCols(); j++) {
                if (!sparse2DenseCol.containsKey(row.getColIndex(j))) {
                    throw new IllegalStateException();
                }
                ids[j] = sparse2DenseCol.get(row.getColIndex(j));
                vals[j] = row.getColValue(j);
                if (ids[j] >= numColumns) {
                    throw new IllegalStateException();
                }
            }
            setRow(i, new SparseVector(dense2SparseCol.length, ids, vals, false));
        }
    }

    public int rowSparse2Dense(int sparseId) {
        return sparse2DenseRow.get(sparseId);
    }
    public int colSparse2Dense(int sparseId) {
        return sparse2DenseCol.get(sparseId);
    }
    public int rowDense2Sparse(int denseId) {
        return dense2SparseRow[denseId];
    }
    public int colDense2Sparse(int denseId) {
        return dense2SparseCol[denseId];
    }

    private static TIntIntMap makeSparseToDenseMap(int []ids) {
        TIntIntMap map = new TIntIntHashMap();
        for (int i = 0; i < ids.length; i++) {
            map.put(ids[i], i);
        }
        return map;
    }
}
