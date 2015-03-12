package org.wikibrain.matrix.knn;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikibrain.matrix.DenseMatrix;
import org.wikibrain.matrix.DenseMatrixRow;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * A fast neighborhood finder for dense vectors.
 *
 * @author Shilad Sen
 */
public class KDTreeKNN implements KNNFinder {
    private final DenseMatrix matrix;
    private final int[] allIds;
    private final int dimensions;
    private int maxSampleSize = 5000;
    private int maxLeaf = 100;

    List<float []> centroids;
    List<int []> members;

    public KDTreeKNN(DenseMatrix matrix) throws IOException {
        this.matrix = matrix;
        this.allIds = matrix.getRowIds();
        this.dimensions = matrix.getRow(allIds[0]).getNumCols();
    }

    @Override
    public void build() throws IOException {
        Node root = new Node("R");

        // shuffle ids to ensure random partition intialization forever
        root.memberIds = new int[allIds.length];
        System.arraycopy(allIds, 0, root.memberIds, 0, allIds.length);
        shuffle(root.memberIds);

        centroids = new ArrayList<float[]>();
        members = new ArrayList<int[]>();
        build(root);
    }

    private void build(Node node) throws IOException {
        if (node.memberIds.length < maxLeaf) {
            centroids.add(node.centroid);
            members.add(node.memberIds);
            return;
        }

        double [] laccum = new double[dimensions];
        double [] raccum = new double[dimensions];

        node.left = new Node(node.path + "L");
        node.right = new Node(node.path + "R");
        node.left.centroid = new float[dimensions];
        node.right.centroid = new float[dimensions];
        int n = Math.min(node.memberIds.length, maxSampleSize);

        // Calculate centroids
        int lcount = 0;
        int rcount = 0;

        for (int iter = 0; iter < 5; iter++) {
            lcount = 0;
            rcount = 0;
            Arrays.fill(laccum, 0.0);
            Arrays.fill(raccum, 0.0);

            double obj = 0.0;
            for (int m = 0; m < n; m++) {
                DenseMatrixRow row = matrix.getRow(node.memberIds[m]);

                double lsim;
                double rsim;

                if (iter == 0) {
                    lsim = (m < n/2) ? 1.0 : 0.0;
                    rsim = 1.0 - lsim;
                } else {
                    lsim = row.dot(node.left.centroid);
                    rsim = row.dot(node.right.centroid);
                }

                if (lsim >= rsim) {
                    for (int j = 0; j < dimensions; j++) {
                        laccum[j] += row.getColValue(j);
                    }
                    lcount++;
                } else {
                    for (int j = 0; j < dimensions; j++) {
                        raccum[j] += row.getColValue(j);
                    }
                    rcount++;
                }
                obj += Math.max(lsim, rsim);
            }
            obj = (iter == 0) ? 0.0 : obj / n;

//            System.out.format("Node %s iter=%d obj=%.3f left-size=%d right-size=%d\n",
//                    node.path, iter, obj, lcount, rcount);

            normalize(laccum);
            normalize(raccum);

            for (int i = 0; i < dimensions; i++) node.left.centroid[i] = (float) laccum[i];
            for (int i = 0; i < dimensions; i++) node.right.centroid[i] = (float) raccum[i];
        }

        // Final placement
        TIntList leftIds = new TIntArrayList();
        TIntList rightIds = new TIntArrayList();
        for (int id : node.memberIds) {
            DenseMatrixRow row = matrix.getRow(id);
            double lsim = row.dot(node.left.centroid);
            double rsim = row.dot(node.right.centroid);
            if (lsim >= rsim) {
                leftIds.add(id);
            } else {
                rightIds.add(id);
            }
        }
        node.left.memberIds = leftIds.toArray();
        node.right.memberIds = rightIds.toArray();
        if (node.left.memberIds.length + node.right.memberIds.length != node.memberIds.length) {
            throw new IllegalStateException();
        }

        // Recurse
        build(node.left);
        build(node.right);

    }

    private static class Candidate implements Comparable<Candidate> {
        final int clusterNum;
        final double score;

        public Candidate(int clusterNum, double score) {
            this.clusterNum = clusterNum;
            this.score = score;
        }

        @Override
        public int compareTo(Candidate o) {
            return Double.compare(score, o.score);
        }
    }

    @Override
    public Neighborhood query(float[] vector, int k, int maxTraversal, TIntSet validIds) {
        TreeSet<Candidate> clusters = new TreeSet<Candidate>();
        for (int i = 0; i < centroids.size(); i++) {
            clusters.add(new Candidate(i, dot(centroids.get(i), vector)));
        }
        NeighborhoodAccumulator accum = new NeighborhoodAccumulator(k);
        int traversed = 0;
        while (!clusters.isEmpty()) {
            int clusterNum = clusters.pollLast().clusterNum;
            for (int rowId : members.get(clusterNum)) {
                if (validIds != null && !validIds.contains(rowId)) continue;
                DenseMatrixRow row = null;
                try {
                    row = matrix.getRow(rowId);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
                double sim = cosine(vector, row);
                accum.visit(row.getRowIndex(), sim);
                traversed++;
            }
            if (traversed >= maxTraversal) {
                break;
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

    public void setMaxSampleSize(int sampleSize) {
        this.maxSampleSize = sampleSize;
    }

    public void setMaxLeaf(int maxLeaf) {
        this.maxLeaf = maxLeaf;
    }


    static class Node {
        String path;
        float [] centroid;
        Node left;
        Node right;
        int [] memberIds;

        public Node(String path) {
            this.path = path;
        }
    }

    static double cosine(DenseMatrixRow X, DenseMatrixRow Y) {
        if (X == null || Y == null) {
            return 0;
        }
        return X.dot(Y);
    }

    static double cosine(float [] X, DenseMatrixRow Y) {
        if (X == null || Y == null) {
            return 0;
        }
        return Y.dot(X);
    }

    private static void shuffle(int [] array) {
        Random rand = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            int index = rand.nextInt(i + 1);
            // Simple swap
            int a = array[index];
            array[index] = array[i];
            array[i] = a;
        }
    }

    private double dot(float [] v1, float [] v2) {
        double sum = 0.0;
        for (int i = 0; i < v1.length; i++) {
            sum += v1[i] * v2[i];
        }
        return sum;
    }


    private static void normalize(double [] X) {
        double norm = 0.0;
        for (int i = 0; i < X.length; i++) norm += X[i] * X[i];
        norm = Math.sqrt(norm) + 0.00001;
        for (int i = 0; i < X.length; i++) X[i] /= norm;
    }
}
