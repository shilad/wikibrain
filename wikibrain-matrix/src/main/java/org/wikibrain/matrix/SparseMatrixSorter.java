package org.wikibrain.matrix;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Writes an output matrix with the same rows, but sorted by row ids
 */
public class SparseMatrixSorter {
    public void sort(SparseMatrix matrix, File file) throws IOException {
        int rowIds[] = matrix.getRowIds();
        rowIds = Arrays.copyOf(rowIds, rowIds.length);
        Arrays.sort(rowIds);
        SparseMatrixWriter writer = new SparseMatrixWriter(file, matrix.getValueConf());
        for (int id : rowIds) {
            writer.writeRow(matrix.getRow(id));

        }
        writer.finish();
    }

    public static void main(String args[]) throws IOException {
        if (args.length != 2) {
            System.err.println("usage: java " +
                    SparseMatrixSorter.class +
                    " input_path output_path");
            System.exit(1);
        }
        SparseMatrix src = new SparseMatrix(new File(args[0]));
        new SparseMatrixSorter().sort(src, new File(args[1]));
    }
}
