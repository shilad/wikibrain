package org.wikibrain.utils;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathArrays;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class WbMathUtils {
    /**
     * @param nums Any descendant of Number (Integer, Short, Double, Float, etc)
     * @return The mean of the number, or Double.NaN if the list is empty.
     */
    public static double mean(Collection<? extends Number> nums) {
        if (nums == null || nums.isEmpty()) {
            return Double.NaN;
        }
        double sum = 0.0;
        for (Number n : nums) {
            sum += n.doubleValue();
        }
        return sum / nums.size();
    }

    /**
     * Returns true if the number is not NaN or infinite.
     */
    public static boolean isReal(double d) {
        return !Double.isNaN(d) && !Double.isInfinite(d);
    }

    /**
     * Returns a value that approaches yInf from y0 as xDelta increases.
     * The values for the function range from (0, y0) to (inf, yInf).
     * For each xHalfLife, the function moves 50% closer to yInf.
     *
     * @param x
     * @param xHalfLife
     * @param y0
     * @param yInf
     * @return
     */
    public static double toAsymptote(double x, double xHalfLife, double y0, double yInf) {
        assert(x > 0);
        double hl = x / xHalfLife;
        return y0 + (1.0 - FastMath.exp(-hl)) * (yInf - y0);
    }

    /**
     * Smooths the function specified by the X, Y pairs.
     * The result will be a new {X, Y} containing numPoints.
     * The new X values will be sampled uniformly from the original X (i.e. every k'th point)
     * The new Y values will be calculated using a #robustMean around each X point with window windowSize.
     * @param X
     * @param Y
     * @param windowSize
     * @param numPoints
     * @return
     */
    public static double[][] smooth(double X[], double Y[], int windowSize, int numPoints) {
        TDoubleArrayList smoothedX = new TDoubleArrayList();
        TDoubleArrayList smoothedY= new TDoubleArrayList();
        for (int i = windowSize / 2; i < X.length - windowSize / 2; i += X.length / (numPoints + 1)) {
            double subYs[] = Arrays.copyOfRange(Y, i - windowSize / 2, i + windowSize);
            smoothedX.add(X[i]);
            smoothedY.add(robustMean(subYs));   // median
        }

        // replace duplicate X points with their mean
        for (int i = 0; i < smoothedX.size();) {
            double x = smoothedX.get(i);
            int span = 0;
            while (i + span < smoothedX.size() && smoothedX.get(i + span) == x) {
                span++;
            }
            if (span > 1) {
                double mean = smoothedY.subList(i, i + span).sum() / span;
                for (int j = i; j < i + span; j++) {
                    smoothedY.set(j, mean);
                }
            }
            i += span;
        }

        return new double[][] { smoothedX.toArray(), smoothedY.toArray()};
    }

    /**
     * Calculates the weighted mean, where the weight is based on the rank difference
     * from the median and normally distributed. The std-dev for the normal distribution
     * is set to X.length / 4, so about two-thirds of the weight is contained within
     * the middle half of all points. The std-dev is bottom capped at 3.
     *
     * @param X
     * @return
     */
    public static double robustMean(double[] X) {
        X = ArrayUtils.clone(X);
        Arrays.sort(X);
        NormalDistribution dist = new NormalDistribution(
                X.length / 2,               // heaviest weight at midpoint
                FastMath.max(3, X.length / 4)); // 66% of the weight within 3 pts on either side
        double sum = 0.0;
        double weight = 0.0;
        for (int i = 0; i < X.length; i++) {
            double d = dist.density(i);
            weight += d;
            sum += X[i] * d;
        }
        return sum / weight;
    }

    /**
     * Make an array monotonically increasing by epsilon.
     * The algorithm is simple and iterative:
     * For each point x_k:
     *     x_k = max(x_k, x_{k-1} + epsilon)
     * This function is destructive.
     *
     * @param X  Input data.
     * @param epsilon The minimum increase allowed between consecutive points.
     */
    public static void makeMonotonicIncreasing(double [] X, double epsilon) {
        for (int i = 1; i < X.length; i++) {
            X[i] = FastMath.max(X[i], X[i-1] + epsilon);
        }
        MathArrays.checkOrder(X, MathArrays.OrderDirection.INCREASING, true);
    }
    public static void makeMonotonicIncreasing(TDoubleList X, double epsilon) {
        double X2[] = X.toArray();
        makeMonotonicIncreasing(X2, epsilon);
        X.set(0, X2);
    }

    /**
     * Given two parallel arrays of doubles representing x,y pairs
     * remove all pairs that are NaN or Infinite, and return the result.
     */
    public static double[][] removeNotNumberPoints(double X[], double Y[]) {
        TDoubleArrayList prunedX = new TDoubleArrayList();
        TDoubleArrayList prunedY = new TDoubleArrayList();
        for (int i = 0; i < X.length; i++) {
            double x = X[i];
            double y = Y[i];
            if (Double.isNaN(x) || Double.isNaN(y) || Double.isInfinite(x) || Double.isInfinite(y)) {
                // skip
            } else {
                prunedX.add(x);
                prunedY.add(y);
            }
        }
        return new double[][] { prunedX.toArray(), prunedY.toArray() };
    }
    public static TDoubleList[] removeNotNumberPoints(TDoubleList X, TDoubleList Y) {
        double pruned[][] = removeNotNumberPoints(X.toArray(), Y.toArray());
        return new TDoubleList[] {
                new TDoubleArrayList(pruned[0]),
                new TDoubleArrayList(pruned[1])
        };
    }

    /**
     * Find colinear columns in a matrix.
     *
     * @param X A rectangular matrix in row-major format.
     * @param epsilon the singularity threshold delta
     *
     * @return A 2-D array from columns to a list of all colinear
     * columns. Each column will appear at most once in the second
     * level of the array. So if columns 1, 4, 6, and 7 are colinear.
     * The entry for column 1 will contain [4, 6, 7] and the entries
     * for 4, 6, and 7 will be empty.
     *
     */
    public static int[][] findColinearColumns(double X[][], double epsilon) {
        if (X.length <= 1) {
            return new int[0][0];
        }
        int numCols = X[0].length;
        if (numCols <= 1) {
            return new int[0][0];
        }

        int [][] colinear = new int[numCols][];
        TIntHashSet alreadyColinear = new TIntHashSet();

        // skip last column, it can't be colinear with anybody!
        for (int col1 = 0; col1 < numCols-1; col1++) {
            if (alreadyColinear.contains(col1)) {
                colinear[col1] = new int[0];
                continue;
            }
            TIntList matches = new TIntArrayList();
            for (int col2 = col1+1; col2 < numCols; col2++) {
                boolean isColinear = true;
                double k = X[0][col1] / X[0][col2];
                for (double row[] : X) {
                    if (row.length != numCols) { throw new IllegalArgumentException(); }
                    if (FastMath.abs(row[col1] - k * row[col2]) > epsilon) {
                        isColinear = false;
                        break;
                    }
                }
                if (isColinear) {
                    matches.add(col2);
                    alreadyColinear.add(col2);
                }
            }
            colinear[col1] = matches.toArray();
        }
        colinear[colinear.length - 1] = new int[0];
        return colinear;
    }
    public static int[][] findColinearColumns(double X[][]) {
        return findColinearColumns(X, 0.000001);
    }

    public static String toString(int X[][]) {
        StringBuffer res = new StringBuffer("{");
        for (int row[] : X) {
            if (row != X[0]) { res.append(", "); }
            res.append(Arrays.toString(row));
        }
        res.append("}");
        return res.toString();
    }

    public static double dot(float[] v1, float[] v2) {
        if (v1.length != v2.length) {
            throw new IllegalArgumentException();
        }
        double dot = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
        }
        return dot;
    }

    public static double dot(double[] v1, float[] v2) {
        if (v1.length != v2.length) {
            throw new IllegalArgumentException();
        }
        double dot = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
        }
        return dot;
    }

    public static double dot(double[] v1, double[] v2) {
        if (v1.length != v2.length) {
            throw new IllegalArgumentException();
        }
        double dot = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
        }
        return dot;
    }

    public static double distance(double[] v1, double[] v2) {
        if (v1.length != v2.length) {
            throw new IllegalArgumentException();
        }
        double l2 = 0.0;
        for (int i = 0; i < v1.length; i++) {
            l2 += (v1[i] - v2[i]) * (v1[i] - v2[i]);
        }
        return FastMath.sqrt(l2);
    }

    /**
     * v1 &lt;- alpha * v2 + v3
     * @param v1
     * @param v2
     * @param alpha
     */
    public static void add(double alpha, double[] v1, double[] v2, double v3[]) {
        for (int i = 0; i < v1.length; i++) {
            v1[i] = alpha * v2[i] + v3[i];
        }
    }

    /**
     * v1 &lt;- alpha * v2 + v3
     * @param v1
     * @param v2
     * @param alpha
     */
    public static void add(double alpha, double[] v1, double[] v2, float v3[]) {
        for (int i = 0; i < v1.length; i++) {
            v1[i] = alpha * v2[i] + v3[i];
        }
    }
    /**
     * v1 &lt;- alpha * v2 + v3
     * @param v1
     * @param v2
     * @param alpha
     */
    public static void add(double alpha, double[] v1, float[] v2, float v3[]) {
        for (int i = 0; i < v1.length; i++) {
            v1[i] = alpha * v2[i] + v3[i];
        }
    }
    /**
     * v1 &lt;- alpha * v2 + v3
     * @param v1
     * @param v2
     * @param alpha
     */
    public static void add(float alpha, float[] v1, float[] v2, float v3[]) {
        for (int i = 0; i < v1.length; i++) {
            v1[i] = alpha * v2[i] + v3[i];
        }
    }

    /**
     * v1 += v2
     * @param v1
     * @param v2
     */
    public static void increment(double v1[], double[] v2) {
        add(1.0, v1, v1, v2);
    }

    /**
     * v1 += v2
     * @param v1
     * @param v2
     */
    public static void increment(double v1[], float[] v2) {
        add(1.0, v1, v1, v2);
    }

    /**
     * v1 += v2
     * @param v1
     * @param v2
     */
    public static void increment(float v1[], float[] v2) {
        add(1.0f, v1, v1, v2);
    }

    public static void normalize(float[] vector) {
        double sum = 0.0;
        for (float x : vector) {
            sum += x * x;
        }
        if (sum == 0) {
            return;
        }
        sum = FastMath.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= sum;
        }
    }



    public static float[] double2Float(double []v) {
        float f[] = new float[v.length];
        for (int i = 0; i < v.length; i++) {
            f[i] = (float) v[i];
        }
        return f;
    }

    public static void normalize(double[] vector) {
        double sum = 0.0;
        for (double x : vector) {
            sum += x * x;
        }
        if (sum == 0) {
            return;
        }
        sum = FastMath.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= sum;
        }
    }

    public static float[][] transpose(float [][]M) {
        if (M.length == 0) {
            return new float[0][0];
        }
        float T[][] = new float[M[0].length][M.length];
        for (int i = 0; i < M.length; i++) {
            for (int j = 0; j < M[0].length; j++) {
                T[j][i] = M[i][j];
            }
        }
        return T;
    }

    public static void zero(float [][] M) {
        for (int i = 0; i < M.length; i++) {
            Arrays.fill(M[i], 0);
        }
    }

    public static void zero(double [][] M) {
        for (int i = 0; i < M.length; i++) {
            Arrays.fill(M[i], 0);
        }
    }
}
