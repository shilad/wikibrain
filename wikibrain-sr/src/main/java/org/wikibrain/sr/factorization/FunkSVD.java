package org.wikibrain.sr.factorization;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.io.FileUtils;
import org.wikibrain.matrix.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Logger;

public class FunkSVD {
    private static final Logger LOG = Logger.getLogger(FunkSVD.class.getName());

    /**
     * Rank of the reduced matrix.
     */
    private int rank;

    /**
     * Low rank approximation for each matrix row
     */
    private double rowApproximations[][];

    /**
     * Low rank approximation for each matrix column
     */
    private double columnApproximations[][];


    /**
     * Mapping from sparse WP ids to a dense int.
     */
    private TIntIntMap columnMap = new TIntIntHashMap();


    /**
     * Mapping from dense mapped column ids to WP ids
     */
    private int reverseColumnMap[];

    private double learningRate = 0.001;
    private double regularization = 0.02;
    private double meanVal = -1;

    private SparseMatrix matrix;

    private Random random = new Random();

    public FunkSVD(SparseMatrix matrix, int rank) {
        this.rank = rank;
        this.matrix = matrix;
    }

    public void estimate() {
        init();
        for (int i = 0; i < rank; i++) {
            LOG.info("doing iteration " + i);
            double rmse = doIteration(i);
            LOG.info("rmse for iteration " + i + " is " + rmse);
        }
    }


    public double doIteration(int dim) {
        double totalErr2 = 0.0;
        long n = 0;
        int r = 0;
        for (SparseMatrixRow row : matrix) {
            if (r % 100000 == 0) {
                LOG.info("visiting row " + r + " of " + matrix.getNumRows());
            }
            double rowV[] = rowApproximations[r++];
            for (int c = 0; c < row.getNumCols(); c++) {
                double colV[] = columnApproximations[columnMap.get(row.getColIndex(c))];
                double pred = dot(rowV, colV, dim);
                double err = row.getColValue(c) - pred;

                double colVV = colV[dim];
                double rowVV = rowV[dim];
                rowV[dim] += learningRate * (err * colVV - regularization * rowVV);
                colV[dim] += learningRate * (err * rowVV - regularization * colVV);
                totalErr2 += err * err;
                n++;
            }
        }
        return Math.sqrt(totalErr2 / n);
    }

    private static final double dot(double X[], double Y[], int maxDim) {
        assert(X.length == Y.length);
        assert(maxDim < X.length);
        double sum = 0.0;
        for (int i = 0; i <= maxDim; i++) {
            sum += X[i] * Y[i];
        }
        return sum;
    }

    private void init() {

        LOG.info("creating dense indexing for column ids");
        for (MatrixRow row : matrix) {
            for (int i = 0; i < row.getNumCols(); i++) {
                int colId = row.getColIndex(i);
                if (!columnMap.containsKey(colId)) {
                    columnMap.put(colId, columnMap.size());
                }
            }
        }
        reverseColumnMap = new int[columnMap.size()];
        for (int colId : columnMap.keys()) {
            reverseColumnMap[columnMap.get(colId)] = colId;
        }
        LOG.info("finished dense indexing for " + reverseColumnMap.length + " column ids");

        rowApproximations = new double[matrix.getNumRows()][rank];
        columnApproximations = new double[columnMap.size()][rank];
        LOG.info("randomly filling rowApproximations");
        for (double row[] : rowApproximations) { randomlyFill(row); }
        LOG.info("randomly filling columnApproximations");
        for (double col[] : columnApproximations) { randomlyFill(col); }

        calculateStats();
    }

    private void calculateStats() {
        LOG.info("calculating mean");
        this.meanVal = 0.0;
        long numCells = 0;
        for (MatrixRow row : matrix) {
            for (int i = 0; i < row.getNumCols(); i++) {
                meanVal += row.getColValue(i);
                numCells++;
            }
        }
        meanVal /= numCells;

        LOG.info("calculating std dev");
        double err2 = 0.0;
        for (MatrixRow row : matrix) {
            for (int i = 0; i < row.getNumCols(); i++) {
                err2 += Math.pow(row.getColValue(i) - meanVal, 2.0);
            }
        }
        err2 = Math.sqrt(err2 / numCells);
        LOG.info("overal std dev is " + err2);
    }

    private void randomlyFill(double X[]) {
        double sum = 0.0;
        for (int i = 0; i < X.length; i++) {
            X[i] = random.nextDouble();
            sum += X[i];
        }
        for (int i = 0; i < X.length; i++) {
            X[i] /= sum;
        }
    }

    public void write(File dir) throws IOException {
        // write columns
        BufferedWriter idFile = new BufferedWriter(new FileWriter(new File(dir, "column_ids.tsv")));
        for (int wpId : reverseColumnMap) {
            idFile.write(wpId + "\n");
        }
        idFile.close();

        // write rows
        idFile = new BufferedWriter(new FileWriter(new File(dir, "row_ids.tsv")));
        for (int wpId : matrix.getRowIds()) {
            idFile.write(wpId + "\n");
        }
        idFile.close();

        // create fake dense ids
        int colIds[] = new int[rank];
        for (int i = 0; i < rank; i++) colIds[i] = i;

        // write dense rows
        double range[] = getMaxAndMin(rowApproximations);
        ValueConf vconf = new ValueConf((float)range[0], (float)range[1]);
        DenseMatrixWriter writer = new DenseMatrixWriter(new File(dir, "row_estimates.matrix"), vconf);
        for (int i = 0; i < rowApproximations.length; i++) {
            int rowId = matrix.getRowIds()[i];
            float row[] = doubleArrayToFloats(rowApproximations[i]);
            writer.writeRow(new DenseMatrixRow(vconf, rowId, colIds, row));
        }
        writer.finish();

        // write dense cols
        range = getMaxAndMin(columnApproximations);
        vconf = new ValueConf((float)range[0], (float)range[1]);
        writer = new DenseMatrixWriter(new File(dir, "col_estimates.matrix"), vconf);
        for (int i = 0; i < columnApproximations.length; i++) {
            int rowId = reverseColumnMap[i];
            float row[] = doubleArrayToFloats(columnApproximations[i]);
            writer.writeRow(new DenseMatrixRow(vconf, rowId, colIds, row));
        }
        writer.finish();
    }

    private double[] getMaxAndMin(double X[][]) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double Y[] : X) {
            for (double x : Y) {
                min = Math.min(x, min);
                max = Math.max(x, min);
            }
        }
        return new double[] { min, max };
    }

    private float[] doubleArrayToFloats(double X[]) {
        float fX[] = new float[X.length];
        for (int i = 0; i < X.length; i++) { fX[i] = (float)X[i]; }
        return fX;
    }

    public static void main(String args[]) throws IOException {
        if (args.length != 3) {
            System.err.println("usage: java " + FunkSVD.class.getName() + " path_matrix output_dir rank");
            System.exit(1);
        }
        File pathIn = new File(args[0]);
        File pathOut = new File(args[1]);
        if (pathOut.exists()) { FileUtils.deleteDirectory(pathOut); }
        pathOut.mkdirs();
        SparseMatrix m = new SparseMatrix(pathIn);
        FunkSVD svd = new FunkSVD(m, Integer.valueOf(args[2]));
        svd.estimate();
        svd.write(pathOut);
    }
}