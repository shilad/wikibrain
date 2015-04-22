package org.wikibrain.matrix;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * A sparse matrix implementation that can be fit in memory.
 *
 * @author Shilad Sen
 */
public class InMemorySparseMatrix implements Matrix<InMemorySparseMatrix.InMemorySparseMatrixRow> {
    public final int rowIds[];
    public final int colIds[][];
    public final double values[][];
    public TIntIntMap rowMap;
    public TIntIntMap idMap;    // raw ids -> compressed ids

    public InMemorySparseMatrix(int rowIds[], int colIds[][], double [][] values) {
        this.rowIds = rowIds;
        this.colIds = colIds;
        this.values = values;
        rowMap = new TIntIntHashMap(rowIds.length * 2);
        for (int i = 0; i < rowIds.length; i++) {
            rowMap.put(rowIds[i], i);
        }
    }

    public InMemorySparseMatrix(File file) throws IOException {
        this(new SparseMatrix(file));   // FIXME: close the matrix!
    }

    public InMemorySparseMatrix(Matrix<? extends MatrixRow> diskMatrix) {
        rowIds = diskMatrix.getRowIds();
        colIds = new int[rowIds.length][];
        values = new double[rowIds.length][];

        int i = 0;
        for (MatrixRow row : diskMatrix) {
            if (rowIds[i] != row.getRowIndex()) throw new IllegalStateException();
            colIds[i] = new int[row.getNumCols()];
            values[i] = new double[row.getNumCols()];
            for (int j = 0; j < row.getNumCols(); j++) {
                colIds[i][j] = row.getColIndex(j);
                values[i][j] = row.getColValue(j);
            }
            i++;
        }
        rowMap = new TIntIntHashMap(rowIds.length * 2);
        for (int j = 0; j < rowIds.length; j++) {
            rowMap.put(rowIds[j], j);
        }
    }

    /**
     * Replaces raw ids with a contiguous set of ids starting at 0.
     */
    public void compressIds() {
        idMap = new TIntIntHashMap();
        for (int i = 0; i < rowIds.length; i++) {
            if (idMap.size() != i) throw new IllegalStateException();
            if (idMap.containsKey(rowIds[i])) throw new IllegalStateException("duplicate row: " + rowIds[i]);
            idMap.put(rowIds[i], i);
            rowIds[i] = i;
        }

        for (int i = 0; i < colIds.length; i++) {
            for (int j = 0; j < colIds[i].length; j++) {
                if (!idMap.containsKey(colIds[i][j])) {
                    idMap.put(colIds[i][j], idMap.size());
                }
                colIds[i][j] = idMap.get(colIds[i][j]);
            }
            quickSort(colIds[i], values[i], 0, colIds[i].length - 1);
        }
    }

    public TIntIntMap getIdMap() {
        return idMap;
    }

    public void decompressIds() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InMemorySparseMatrixRow getRow(int rowId) throws IOException {
        if (rowMap.containsKey(rowId)) {
            return getMatrixRowInternal(rowMap.get(rowId));
        } else {
            return null;
        }
    }

    @Override
    public int[] getRowIds() {
        return rowIds;
    }

    @Override
    public int getNumRows() {
        return rowIds.length;
    }

    private InMemorySparseMatrixRow getMatrixRowInternal(int i) {
        return new InMemorySparseMatrixRow(rowIds[i], colIds[i], values[i]);
    }

    @Override
    public Iterator<InMemorySparseMatrixRow> iterator() {
        return new Iterator<InMemorySparseMatrixRow>() {
            private int i;

            @Override
            public boolean hasNext() {
                return i < rowIds.length;
            }

            @Override
            public InMemorySparseMatrixRow next() {
                if (i >= rowIds.length) return null;
                return getMatrixRowInternal(i++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public File getPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {

    }

    public static class InMemorySparseMatrixRow extends BaseMatrixRow implements MatrixRow {
        int rowId;
        int colIds[];
        double colVals[];

        public InMemorySparseMatrixRow(int rowId, int[] colId, double[] value) {
            this.rowId = rowId;
            this.colIds = colId;
            this.colVals = value;
        }

        @Override
        public int getColIndex(int i) {
            return colIds[i];
        }

        @Override
        public float getColValue(int i) {
            return (float)colVals[i];
        }

        @Override
        public int getRowIndex() {
            return rowId;
        }

        @Override
        public int getNumCols() {
            return colIds.length;
        }
    }


    // Adapted from http://www.programcreek.com/2012/11/quicksort-array-in-java/
    private void quickSort(int colIds[], double colVals[], int low, int high) {
        if (colIds.length == 0 || low >= high)
            return;

        // pick the pivot
        int middle = (low + high) / 2;
        int pivot = colIds[middle];

        // partition around the pivot
        int i = low, j = high;
        while (i <= j) {
            while (colIds[i] < pivot) {
                i++;
            }
            while (colIds[j] > pivot) {
                j--;
            }
            if (i <= j) {
                int temp = colIds[i];
                double tempV = colVals[i];
                colIds[i] = colIds[j];
                colVals[i] = colVals[j];
                colIds[j] = temp;
                colVals[j] = tempV;
                i++;
                j--;
            }
        }

        //recursively sort two sub parts
        quickSort(colIds, colVals, low, j);
        quickSort(colIds, colVals, i, high);
    }

}
