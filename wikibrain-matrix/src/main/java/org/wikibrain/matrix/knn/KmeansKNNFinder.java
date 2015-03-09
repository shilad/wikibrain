package org.wikibrain.matrix.knn;

import org.wikibrain.matrix.DenseMatrix;
import org.wikibrain.matrix.DenseMatrixRow;

import java.io.IOException;
import java.util.*;

/**
 * A fast neighborhood finder for dense vectors.
 *
 * @author Shilad Sen
 */
public class KmeansKNNFinder {
    private final DenseMatrix matrix;
    private int sampleSize = 50000;
    private int maxLeaf = 20;
    private int branchingFactor = 5;
    private Node root;

    public KmeansKNNFinder(DenseMatrix matrix) {
        this.matrix = matrix;
    }

    public void build() throws IOException {
        root = new Node("R");
        root.members.addAll(getSample());
        root.build();
    }

    private static class Candidate implements Comparable<Candidate> {
        Node n;
        double score;

        public Candidate(Node n, double score) {
            this.n = n;
            this.score = score;
        }

        @Override
        public int compareTo(Candidate o) {
            return Double.compare(score, o.score);
        }
    }

    public Neighborhood query(float [] vector, int k, int maxTraversal) {
        NeighborhoodAccumulator accum = new NeighborhoodAccumulator(k);
        TreeSet<Candidate> work = new TreeSet<Candidate>();
        work.add(new Candidate(root, -1.0));
        int traversed = 0;
        while (!work.isEmpty()) {
            Node n = work.pollLast().n;
            for (DenseMatrixRow row : n.members) {
                double sim = cosine(vector, row);
                accum.visit(row.getRowIndex(), sim);
                traversed++;
            }
            if (traversed >= maxTraversal) {
                break;
            }
            for (Node c : n.children) {
                if (c != null) {
                    work.add(new Candidate(c, cosine(vector, c.delegate)));
                }
            }
        }
        return accum.get();
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
        String path;
        DenseMatrixRow delegate;
        Node[] children = null;
        List<DenseMatrixRow> members = new ArrayList<DenseMatrixRow>();

        Node(String path) { this.path = path; }

        void build() {
//            System.out.println("building node with " + members.size());
            if (members.size() <= maxLeaf) {
                return;
            }
            initializeRandomly();
            for (Node n : children) {
                n.updateCenter();
            }
            double prevScore = 0.000000001;
            for (int i = 0; i < 5; i++) {
                double score = reallocateMembers();
                System.out.println(path + " score at iteration " + i + " is " + score);
                if (score / prevScore - 1.0 < 0.001) {
                    break;
                }
                for (Node n : children) {
                    n.updateCenter();
                }
                prevScore = score;
            }
            for (Node n : children) {
                n.build();
            }
        }

        private void initializeRandomly() {
            children = new Node[branchingFactor];
            for (int i = 0; i < children.length; i++) {
                children[i] = new Node(path + i);
            }
            Collections.shuffle(members);
            for (int i = 0; i < members.size(); i++) {
                children[i % branchingFactor].members.add(members.get(i));
            }
        }

        private double updateCenter() {
            if (members.isEmpty()) {
                delegate = null;
                return 0.0;
            }

            // Calculate a new centroid
            double center [] = new double[members.get(0).getNumCols()];
            for (DenseMatrixRow m : members) {
                for (int i = 0; i < center.length; i++) {
                    center[i] += m.getColValue(i);
                }
            }
            for (int i = 0; i < center.length; i++) {
                center[i] /= members.size();
            }

            // Pick the best delegate.
            double compactness = 0.0;
            double mostSimilar = -10;
//            System.out.println("picking delegate for " + path);
//            if (delegate != null) System.out.println("\told delegate was " + delegate.getRowIndex());
            for (DenseMatrixRow m : members) {
                double s = cosine(center, m);
                compactness += s;
                if (s > mostSimilar) {
//                    System.out.println("\tdelegate " + m.getRowIndex() + " has sim " + s);
                    mostSimilar = s;
                    delegate = m;
                }
            }
            return compactness / members.size();
        }

        private double reallocateMembers() {
            for (Node n : children) {
                n.members.clear();
            }
            double score = 0.0;
            for (DenseMatrixRow m : members) {
                double bestSim = -10;
                Node best = null;
                for (Node n : children) {
                    double s = cosine(n.delegate, m);
                    if (s > bestSim) {
                        best = n;
                        bestSim = s;
                    }
                }
                if (best == null) {
                    throw new IllegalStateException();
                }
//                System.out.println(m.getRowIndex() + " went to " + best.delegate.getRowIndex() + " with sim " + bestSim);
                best.members.add(m);
                score += bestSim;
            }
            return score / members.size();
        }
    }

    private static double cosine(DenseMatrixRow X, DenseMatrixRow Y) {
        if (X == null || Y == null) {
            return 0;
        }
        double xDotX = 0.0;
        double yDotY = 0.0;
        double xDotY = 0.0;
        for (int i = 0; i < X.getNumCols(); i++) {
            double x = X.getColValue(i);
            double y = Y.getColValue(i);
            xDotX += x * x;
            yDotY += y * y;
            xDotY += x * y;
        }
        if (xDotX * yDotY == 0) {
            return 0.0;
        }
        return xDotY / Math.sqrt(xDotX * yDotY);
    }

    private static double cosine(double [] X, DenseMatrixRow Y) {
        if (X == null || Y == null) {
            return 0;
        }
        double xDotX = 0.0;
        double yDotY = 0.0;
        double xDotY = 0.0;
        for (int i = 0; i < X.length; i++) {
            double x = X[i];
            double y = Y.getColValue(i);
            xDotX += x * x;
            yDotY += y * y;
            xDotY += x * y;
        }
        if (xDotX * yDotY == 0) {
            return 0.0;
        }
        return xDotY / Math.sqrt(xDotX * yDotY);
    }

    private static double cosine(float [] X, DenseMatrixRow Y) {
        if (X == null || Y == null) {
            return 0;
        }
        double xDotX = 0.0;
        double yDotY = 0.0;
        double xDotY = 0.0;
        for (int i = 0; i < X.length; i++) {
            double x = X[i];
            double y = Y.getColValue(i);
            xDotX += x * x;
            yDotY += y * y;
            xDotY += x * y;
        }
        if (xDotX * yDotY == 0) {
            return 0.0;
        }
        return xDotY / Math.sqrt(xDotX * yDotY);
    }
}
