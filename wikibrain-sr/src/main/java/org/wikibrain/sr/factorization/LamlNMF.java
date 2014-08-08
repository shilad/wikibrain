package org.wikibrain.sr.factorization;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import ml.clustering.Clustering;
import ml.clustering.L1NMF;
import ml.clustering.NMF;
import ml.options.L1NMFOptions;
import ml.options.NMFOptions;
import org.wikibrain.matrix.SparseMatrix;
import org.wikibrain.matrix.SparseMatrixRow;

import java.io.File;
import java.io.IOException;

import static ml.utils.Matlab.full;
import static ml.utils.Printer.printMatrix;

/**
 * @author Shilad Sen
 */
public class LamlNMF {
    public void factorize(SparseMatrix input, File outputDir, int rank) {
        TIntIntMap colIds = new TIntIntHashMap();
        TIntIntMap rowIds = new TIntIntHashMap();
        TIntList rows = new TIntArrayList();
        TIntList cols = new TIntArrayList();
        TDoubleList vals = new TDoubleArrayList();
        int nonZero = 0;

        for (SparseMatrixRow row : input) {
            if (!rowIds.containsKey(row.getRowIndex())) {
                rowIds.put(row.getRowIndex(), rowIds.size());
            }
            int ri = rowIds.get(row.getRowIndex());
            if (ri >= rowIds.size()) {
                throw new IllegalStateException();
            }

            for (int i = 0; i < row.getNumCols(); i++) {
                int ci;
                if (!colIds.containsKey(row.getColIndex(i))) {
                    colIds.put(row.getColIndex(i), colIds.size());
                }
                ci = colIds.get(row.getColIndex(i));
                rows.add(ri);
                cols.add(ci);
                vals.add(row.getColValue(i));
                nonZero++;
            }
        }

        int numRows = rowIds.size();
        int numCols = colIds.size();

        System.out.println("numRows is " + numRows);
        System.out.println("nuMCols is " + numCols);

        la.matrix.SparseMatrix M = la.matrix.SparseMatrix.createSparseMatrix(rows.toArray(), cols.toArray(), vals.toArray(), numRows, numCols, nonZero);

        L1NMFOptions L1NMFOptions = new L1NMFOptions();
        L1NMFOptions.nClus = rank;
        L1NMFOptions.maxIter = 50;
        L1NMFOptions.verbose = true;
        L1NMFOptions.calc_OV = false;
        L1NMFOptions.epsilon = 1e-5;
        Clustering nmf = new L1NMF(L1NMFOptions);

        nmf.feedData(M);
        nmf.initialize(null);

        System.out.println("Basis Matrix:");
        printMatrix(full(nmf.getCenters()));

        System.out.println("Indicator Matrix:");
        printMatrix(full(nmf.getIndicatorMatrix()));
    }

    public static void main(String args[]) throws IOException {
        LamlNMF nmf = new LamlNMF();
        nmf.factorize(m, new File("/Volumes/ShiladsFastDrive/wikibrain-simple/dat/sr/topics/simple/inlink-laml"), 50);
    }
}

