package org.wikibrain.matrix.knn;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikibrain.matrix.DenseMatrix;
import org.wikibrain.matrix.DenseMatrixRow;

import java.io.*;
import java.util.Arrays;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Approximation cache for nearest neighbors that uses a random-projection scheme.
 * This implementation keeps two longs (16 bytes) per row in the dense matrix.
 * It constructs one random vector for each of the 128 bits in the two longs.
 * Each bit in a particular row's two longs indicates the sign (-1 = 0, +1 = 1) of the
 * dot product.
 *
 * To find neighbors, the algorithm counts how many of the 128 bits agree between
 * a query and a candidate.
 *
 * @author Shilad Sen
 */
public class RandomProjectionKNNFinder implements KNNFinder {
    private static final Logger LOG = LoggerFactory.getLogger(RandomProjectionKNNFinder.class);
    public static final int NUM_BITS = 128;

    private final DenseMatrix matrix;
    private final int dimensions;
    private long [] bits;   // two longs per matrix entry
    private int [] ids;
    private double [][] vectors;

    // Sampled mean and standard deviation
    private double [] means;
    private double[] devs;

    public RandomProjectionKNNFinder(DenseMatrix matrix) throws IOException {
        this.matrix = matrix;
        this.ids = matrix.getRowIds();
        this.dimensions = matrix.getRow(ids[0]).getNumCols();
    }

    @Override
    public void build() throws IOException {
        makeVectors();
        bits = new long[ids.length*2];
        long vbits[] = new long[2];
        for (int i = 0; i < ids.length; i++) {
            float [] v = matrix.getRow(ids[i]).getValues();
            project(v, vbits);
            bits[i*2] = vbits[0];
            bits[i*2+1] = vbits[1];
        }
    }

    private void makeVectors() throws IOException {
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
            LOG.debug("dimension " + d + " has mean " + means[d] + " and std-dev " + devs[d]);
        }


        Random random = new Random();
        vectors = new double[NUM_BITS][dimensions];
        double norms[] = new double[NUM_BITS];
        for (int d = 0; d < dimensions; d++) {
            for (int i = 0; i < vectors.length; i++) {
                vectors[i][d] = random.nextGaussian() / 2;
                norms[i] += vectors[i][d] * vectors[i][d];
            }
        }
        for (int i = 0; i < vectors.length; i++) {
            for (int d = 0; d < dimensions; d++) {
                vectors[i][d] /= (norms[i] + 0.000001);
            }
        }
    }

    private void project(float [] v, long [] result) {
        if (v.length != dimensions) {
            throw new IllegalArgumentException("Expected " + dimensions + " dimensions, found " + v.length);
        }
        double[] v2 = new double[dimensions];
        for (int d = 0; d < dimensions; d++) {
            v2[d] = (v[d] - means[d]) / devs[d];
        }
        long bits1 = 0;
        for (int i = 0; i < vectors.length/2; i++) {
            double s = dot(vectors[i], v2);
            if (s > 0) {
                bits1 |= (1l << i);
            }
        }
        long bits2 = 0;
        for (int i = vectors.length/2; i < vectors.length; i++) {
            double s = dot(vectors[i], v2);
            if (s > 0) {
                bits2 |= (1l << i);
            }
        }
        result[0] = bits1;
        result[1] = bits2;
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
        // Hack: Speed up query by reducing number of collisions
        if (validIds != null) {
            TIntSet tmp = validIds;
            validIds = new TIntHashSet(tmp.size() * 4);
            validIds.addAll(tmp);
        }
        long vbits[] = new  long[2];
        project(vector, vbits);
        long p0 = vbits[0];
        long p1 = vbits[1];

        // Pass 1: count how many things have each # of bits.
        int[] numHits = new int[NUM_BITS + 1];
        for (int i = 0; i < ids.length; i++) {
            if (validIds != null && !validIds.contains(ids[i])) continue;
            int nSet = NUM_BITS - Long.bitCount(bits[2*i] ^ p0) - Long.bitCount(bits[2*i+1] ^ p1);
            numHits[nSet]++;
        }
//        System.out.println("distribution is " + Arrays.toString(numHits));

        // Pick the threshold we need to consider.
        int threshold;
        int count = 0;
        for (threshold = NUM_BITS; threshold > 0; threshold --) {
            count += numHits[threshold];
            if (count >= maxTraversal) {
                break;
            }
        }
//        System.out.println("set threshold at at least " + threshold + " bits in common");

        NeighborhoodAccumulator accum = new NeighborhoodAccumulator(k);
        for (int i = 0; i < ids.length; i++) {
            if (validIds != null && !validIds.contains(ids[i])) continue;
            int nSet = NUM_BITS - Long.bitCount(bits[2*i] ^ p0) - Long.bitCount(bits[2*i+1] ^ p1);
            if (nSet >= threshold) {
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
        path.getParentFile().mkdirs();
        ObjectOutputStream oop = new ObjectOutputStream(new FileOutputStream(path));
        oop.writeObject(new Object[] { vectors, bits, means, devs});
        oop.close();
    }

    @Override
    public boolean load(File path) throws IOException {
        if (!path.isFile()) {
            LOG.warn("Not loading knn model. File doesn't exist: " + path);
            return false;
        } else if (path.lastModified() < matrix.getPath().lastModified()) {
            LOG.warn("Not loading knn model. File " + path + " older than matrix: " + matrix.getPath());
            return false;
        }
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(path));
        try {
            Object [] obj = (Object[]) in.readObject();
            double [][] newVectors = (double[][]) obj[0];
            long [] newBits = (long[]) obj[1];
            double [] newMeans = (double[]) obj[2];
            double [] newDevs = (double[]) obj[3];
            if (newBits.length != ids.length *2) {
                LOG.warn("Not loading knn model. Expected " + 2*ids.length + " longs, found " + newBits.length);
                return false;
            }
            if (newVectors.length != NUM_BITS || newVectors[0].length != dimensions) {
                LOG.warn("Not loading knn model. Invalid vectors dimensions.");
                return false;
            }
            if (newMeans.length != dimensions || newDevs.length != dimensions) {
                LOG.warn("Not loading knn model. Invalid mean or devs dimensions.");
                return false;
            }
            this.vectors =newVectors;
            this.bits = newBits;
            this.means = newMeans;
            this.devs = newDevs;
            return true;
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }


}
