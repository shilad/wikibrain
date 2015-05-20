package org.wikibrain.matrix.knn;

import gnu.trove.set.TIntSet;
import org.wikibrain.matrix.DenseMatrix;
import org.wikibrain.matrix.DenseMatrixRow;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Shilad Sen
 */
public class LSHForestKNNFinder implements KNNFinder {
    private static final Logger LOG = LoggerFactory.getLogger(LSHForestKNNFinder.class);
    private static final int NUM_BITS = 16; // Number of bits in each mask (size of short)

    private int numTrees = 5;
    private short [][] bits;
    private final DenseMatrix matrix;
    private final int dimensions;
    private final int [] ids;
    private double [][][] vectors;

    // Sampled mean and standard deviation
    private double [] means;
    private double[] devs;

    public LSHForestKNNFinder(DenseMatrix matrix) throws IOException {
        this.matrix = matrix;
        this.ids = matrix.getRowIds();
        this.dimensions = matrix.getRow(ids[0]).getNumCols();
    }

    @Override
    public synchronized void build() throws IOException {
        analyzeSample();
        bits = new short[numTrees][];
        vectors = new double[numTrees][][];
        for (int i = 0; i < numTrees; i++) {
            buildTree(i);
        }
    }

    private void analyzeSample() throws IOException {
        // Sample the mean of each dimension
        means = new double[dimensions];
        int n = Math.min(5000, ids.length);
        for (int i = 0; i < n; i++) {
            float vals[] = matrix.getRow(ids[i]).getValues();
            if (vals.length != dimensions) throw new IllegalStateException();
            for (int d = 0; d < vals.length; d++) {
                means[d] += vals[d];
            }
        }
        for (int j= 0; j < dimensions; j++) {
            means[j] /= n;
        }

        // Sample the standard deviation of each dimension
        devs = new double[dimensions];
        Arrays.fill(devs, 0.0001);   // avoid divide by zero in normalization procedure
        for (int i = 0; i < n; i++) {
            float vals[] = matrix.getRow(ids[i]).getValues();
            for (int d = 0; d < vals.length; d++) {
                devs[d] += (vals[d] - means[d]) * (vals[d] - means[d]);
            }
        }
        for (int d= 0; d < dimensions; d++) {
            devs[d] = Math.sqrt(devs[d] / n);
            LOG.info("dimension " + d + " has mean " + means[d] + " and std-dev " + devs[d]);
        }
    }

    private void buildTree(int treeNum) throws IOException {
        double [][] V = new double[NUM_BITS][dimensions];
        vectors[treeNum] = V;

        // Make vectors for this tree
        Random random = new Random();
        double norms[] = new double[NUM_BITS];
        for (int d = 0; d < dimensions; d++) {
            for (int i = 0; i < V.length; i++) {
                V[i][d] = random.nextGaussian() / 2;
                norms[i] += V[i][d] * V[i][d];
            }
        }
        for (int i = 0; i < V.length; i++) {
            for (int d = 0; d < dimensions; d++) {
                V[i][d] /= (norms[i] + 0.000001);
            }
        }

        short [] B = new short[ids.length];
        bits[treeNum] = B;
        for (int i = 0; i < ids.length; i++) {
            B[i] = project(treeNum, matrix.getRow(ids[i]).getValues());
        }
    }


    private short project(int treeNum, float [] v) {
        if (v.length != dimensions) {
            throw new IllegalArgumentException("Expected " + dimensions + " dimensions, found " + v.length);
        }
        double [][] V = vectors[treeNum];
        double[] v2 = new double[dimensions];
        for (int d = 0; d < dimensions; d++) {
            v2[d] = (v[d] - means[d]) / devs[d];
        }
        short result = 0;
        for (int i = 0; i < V.length; i++) {
            double s = dot(V[i], v2);
            if (s > 0) {
                result |= (((short)1) << i);
            }
        }
        return result;
    }


    private double dot(double [] v1, double [] v2) {
        double sum = 0.0;
        for (int i = 0; i < v1.length; i++) {
            sum += v1[i] * v2[i];
        }
        return sum;
    }

    @Override
    public Neighborhood query(float[] vector, int k, int maxTraversal, TIntSet validIds) {
        short [] P = new short[numTrees];   // projections
        for (int i = 0; i < numTrees; i++) {
            P[i] = project(i, vector);
        }

        Random rand = new Random();
        byte [] idMatchLens = new byte[ids.length];
        Arrays.fill(idMatchLens, (byte) -1);
        int hist[] = new int[NUM_BITS+1];
        for (int i = 0; i < ids.length; i++) {
            if (validIds != null && !validIds.contains(ids[i])) continue;
            int maxMatch = -1; // max bitwise prefix match
            for (int t = 0; t < numTrees; t++) {
                int m = Integer.numberOfLeadingZeros((P[t] ^ bits[t][i]) & 0xffff) - (32 - NUM_BITS);
//                System.out.format("Leading zeros for %d and %d with %s and %s with XOR %s is %d\n",
//                        P[t], bits[t][i],
//                        paddedShortBinary(P[t]),
//                        paddedShortBinary(bits[t][i]),
//                        paddedShortBinary((P[t] ^ bits[t][i])), m);
                maxMatch = Math.max(m, maxMatch);
            }
//            maxMatch = rand.nextInt(17);
            if (maxMatch < 0 || maxMatch > Byte.MAX_VALUE) throw new IllegalStateException();
            idMatchLens[i] = (byte) maxMatch;
            hist[maxMatch] += 1;
        }

        // Pick the threshold we need to consider.
        int threshold;
        int count = 0;
        for (threshold = NUM_BITS; threshold > 0; threshold --) {
            count += hist[threshold];
            if (count >= maxTraversal) {
                break;
            }
        }
        System.out.println("threshold is " + threshold + " for " + Arrays.toString(hist));

        // Find all candidates within the threshold
        NeighborhoodAccumulator accum = new NeighborhoodAccumulator(k);
        for (int i = 0; i < ids.length; i++) {
            if (idMatchLens[i] >= threshold) {
                try {
                    DenseMatrixRow row = matrix.getRow(ids[i]);
                    double sim = KmeansKNNFinder.cosine(vector, row);
                    accum.visit(ids[i], sim);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return accum.get();
    }

    @Override
    public void save(File path) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean load(File path) throws IOException {
        throw new UnsupportedOperationException();
    }

    private String paddedShortBinary(int s) {
        return String.format("%16s", Integer.toBinaryString(s & 0xffff)).replace(' ', '0');
    }
}
