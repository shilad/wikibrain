package org.wikibrain.matrix.knn;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
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
public class KmeansKNNFinder implements KNNFinder {
    private final DenseMatrix matrix;
    private int sampleSize = 50000;
    private int maxLeaf = 20;
    private int branchingFactor = 5;
    private Node root;

    public KmeansKNNFinder(DenseMatrix matrix) {
        this.matrix = matrix;
    }

    @Override
    public void build() throws IOException {
        root = new Node("R");
        root.members.addAll(getSample());
        root.build();
        for (DenseMatrixRow row : matrix) {
            root.place(row);
        }
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

    @Override
    public Neighborhood query(float[] vector, int k, int maxTraversal, TIntSet validIds) {
        if (validIds != null) {
            throw new UnsupportedOperationException();
        }
        NeighborhoodAccumulator accum = new NeighborhoodAccumulator(k);
        TreeSet<Candidate> work = new TreeSet<Candidate>();
        work.add(new Candidate(root, -1.0));
        int traversed = 0;
        while (!work.isEmpty()) {
            Node n = work.pollLast().n;
            for (int rowId : n.memberIds.toArray()) {
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
            if (n.children != null) {
                for (Node c : n.children) {
                    work.add(new Candidate(c, cosine(vector, c.delegate)));
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

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    public void setMaxLeaf(int maxLeaf) {
        this.maxLeaf = maxLeaf;
    }

    public void setBranchingFactor(int branchingFactor) {
        this.branchingFactor = branchingFactor;
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
        TIntList memberIds;
        List<DenseMatrixRow> members = new ArrayList<DenseMatrixRow>();

        Node(String path) { this.path = path; }

        void build() {
//            System.out.println("building node with " + members.size());
            if (members.size() <= maxLeaf) {
                endBuild();
                return;
            }
            initializeRandomly();
            for (Node n : children) {
                n.updateCenter();
            }
            double prevScore = 0.000000001;
            for (int i = 0; i < 5; i++) {
                double score = reallocateMembers();
//                System.out.println(path + " score at iteration " + i + " is " + score);
                if (score / prevScore - 1.0 < 0.001) {
                    break;
                }
                for (Node n : children) {
                    n.updateCenter();
                }
                prevScore = score;
            }
            endBuild();
            for (Node n : children) {
                n.build();
            }
        }


        void place(DenseMatrixRow row) {
            // If we're a leaf
            if (children == null) {
                memberIds.add(row.getRowIndex());
                return;
            }

            // Otherwise find closest child.
            findClosestChild(row).place(row);
        }

        private void endBuild() {
            members = null;
            memberIds = new TIntArrayList();
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
            for (DenseMatrixRow m : members) {
                double s = cosine(center, m);
                compactness += s;
                if (s > mostSimilar) {
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
                Node best = findClosestChild(m);
                score += best.similarity(m);
                best.members.add(m);
            }
            return score / members.size();
        }

        private Node findClosestChild(DenseMatrixRow row) {
            double bestSim = -10;
            Node best = null;
            for (Node n : children) {
                double s = n.similarity(row);
                if (s > bestSim) {
                    best = n;
                    bestSim = s;
                }
            }
            if (best == null) {
                throw new IllegalStateException();
            }
            return best;
        }

        private double similarity(DenseMatrixRow row) {
            return cosine(delegate, row);
        }

        private double similarity(float [] v) {
            return cosine(v, delegate);
        }
    }

    static double cosine(DenseMatrixRow X, DenseMatrixRow Y) {
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

    static double cosine(double [] X, DenseMatrixRow Y) {
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

    static double cosine(float [] X, DenseMatrixRow Y) {
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
