package org.wikibrain.sr.factorized;

import gnu.trove.impl.Constants;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.matrix.*;
import org.wikibrain.utils.WbMathUtils;

import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * @author Shilad Sen
 */
public class Factorizer {
    private Random random = new Random();
    private final int rank;
    private double vectors[][];


    public Factorizer(int rank) {
        this.rank = rank;
    }

    /**
     *
     * Loss function is
     *
     * L(x', y') = alpha * (x'y' - xy)^2 + beta * (x')^2
     *
     * Derivative for d/dx' is:
     *
     * d/dx'(L) = 2 * alpha * (x'y' - xy) * y' + 2 * beta * x'
     * d/dx'(L) = 2 * alpha * err * y' + 2 * beta * x'
     *
     * d/dy' just swaps x and y.
     *
     *
     * @param similarity
     * @param output
     */
    public synchronized void factorize(SparseMatrix similarity, File output) throws IOException {
        TIntIntMap idToIndex = new TIntIntHashMap(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1, -1);
        for (int id : similarity.getRowIds()) {
            idToIndex.put(id, idToIndex.size());
        }

        vectors = new double[similarity.getNumRows()][rank];
        for (int i = 0; i < vectors.length; i++) {
            for (int j = 0; j < rank; j++) {
                vectors[i][j] = random.nextDouble() / 10;
            }
        }
        int rowIds[] = similarity.getRowIds();

        TIntSet found = new TIntHashSet(100000);
        double tmp[] = new double[rank];
        for (int iter = 0; iter < 5; iter++) {
            double error = 0.0;
            int counter = 0;
            for (SparseMatrixRow row : similarity) {
                int index1 = idToIndex.get(row.getRowIndex());
                if (index1 < 0) {
                    continue;
                }
                found.clear();
                for (int i = 0; i < row.getNumCols() + 1000; i++) {
                    double val;
                    int index2;
                    if (i < row.getNumCols()) {
                        index2 = idToIndex.get(row.getColIndex(i));
                        if (index2 < 0) {
                            continue;
                        }
                        val = row.getColValue(i);
                    } else {
                        index2 = random.nextInt(vectors.length);
                        val = 0.0;
                    }
                    if (found.contains(index2) || index2 == index1) {
                        continue;
                    }
                    found.add(index2);
                    System.arraycopy(vectors[index1], 0, tmp, 0, rank);
                    double dot = WbMathUtils.dot(vectors[index1], vectors[index2]);
                    double err = val - dot;
                    err *= 0.1 + val * val;
                    error += err * err;
                    WbMathUtils.add(0.001 * err, vectors[index1], vectors[index2], vectors[index1]);
                    WbMathUtils.add(0.001 * err, vectors[index2], tmp, vectors[index2]);
                    if (counter % 10000000 == 0) {
                        System.err.format("iter %d, error is %.3f on %d\n",
                                iter, (error / 10000000.0), counter);
                        error = 0;
                    }
                    counter ++;
                }
            }
        }
        float max = -1000000.0f;
        float min = 1000000.0f;
        for (double v[] : vectors) {
            for (double x : v) {
                max = Math.max((float)x, max);
                min = Math.min((float)x, min);
            }
        }
        System.err.println("range is " + min + " to " + max);
        ValueConf vconf = new ValueConf(min - 0.000001f, max + 0.00001f);
        DenseMatrixWriter writer = new DenseMatrixWriter(output, vconf);
        int colIds[] = new int[rank];
        for (int i = 0; i < rank; i++) colIds[i] = i;
        int row = 0;
        for (int id : similarity.getRowIds()) {
            float v[] = WbMathUtils.double2Float(vectors[row++]);
            writer.writeRow(new DenseMatrixRow(vconf, id, colIds, v));
        }
        writer.finish();
    }

    public static void main (String args[]) throws ConfigurationException, IOException {
        Factorizer f = new Factorizer(100);
        File dir = new File("/Volumes/ShiladsFastDrive/wikibrain-simple/dat/sr/ensemble/simple/");
        File input = FileUtils.getFile(dir, "mostSimilar.matrix");
        File output = FileUtils.getFile(dir, "factorized.matrix");
        f.factorize(new SparseMatrix(input), output);
    }
}
