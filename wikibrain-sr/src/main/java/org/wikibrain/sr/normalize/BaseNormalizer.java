package org.wikibrain.sr.normalize;

import gnu.trove.list.array.TDoubleArrayList;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.wikibrain.sr.SRResultList;

import java.io.Serializable;
import java.util.Random;

/**
 * A class that supports various kinds of normalization.
 * Usage:
 * 1. Create the normalizer.
 * 2. Call observe with each observation.
 * 3. Call finalize.
 * 4. Call normalize() on a new datapoint.
 * Make sure to set the missingScore value for the SRResultList version.
 */
public abstract class BaseNormalizer implements Serializable, Normalizer {
    public static final long serialVersionUID = 4305858822325261880L;

    public final static int SAMPLE_SIZE = 50000;

    public double min = Double.MIN_VALUE;
    protected double max = -Double.MAX_VALUE;

    // After calling finalize, stats will be non-null.
    protected TDoubleArrayList sample = new TDoubleArrayList();
    protected DescriptiveStatistics stats;

    protected Integer numObservations = 0;
    protected Random random = new Random();

    // mean actual similarity for scores that are missing or infinite.
    protected double missingMean = Double.NaN;
    // accumulators for missing values
    private double missingSum = 0.0;
    private int missingCount = 0;
    private boolean trained = false;
    private int sampleSize = SAMPLE_SIZE;

    /**
     * To meet the serializable contract.
     */
    protected BaseNormalizer() {}

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    @Override
    public void reset() {
        min = Double.MIN_VALUE;
        max = -Double.MAX_VALUE;
        sample.clear();
        stats = null;
        numObservations = 0;
        missingMean = Double.NaN;
        missingSum = 0.0;
        missingCount = 0;
        trained = false;
    }

    @Override
    public void observe(SRResultList sims, int rank, double y) {
        if (rank >= 0) {
            observe(sims.get(rank).getScore(), y);
        } else {
            observe(Double.NaN, y);
        }
    }

    @Override
    public void observe(double x, double y){
        if (Double.isNaN(x) || Double.isInfinite(x)) {
            synchronized (this) {
                missingSum += y;
                missingCount++;
            }
        }
        observe(x);
    }

    @Override
    public void observe(double x) {
        synchronized (this) {
            if (!Double.isNaN(x) && !Double.isInfinite(x)) {
                if (x < min) { min = x; }
                if (x > max) { max = x; }
                if (sample.size() < sampleSize) {
                    sample.add(x);
                } else if (random.nextDouble() < 1.0 * sample.size() / (numObservations + 1)) {
                    sample.set(random.nextInt(sample.size()),  x);
                }
            }
            numObservations++;
        }
    }

    @Override
    public void observationsFinished() {
        sample.sort();
        stats = new DescriptiveStatistics(sample.toArray());

        if (missingCount > 0) {
            missingMean = missingSum / missingCount;
            missingSum = 0.0;
            missingCount = 0;
        }
        trained = true;
    }

    /**
     * A basic implementation of normalize.
     * @param list
     */
    @Override
    public SRResultList normalize(SRResultList list) {
        SRResultList dsl = new SRResultList(list.numDocs());
        list.setMissingScore(missingMean);
        for (int i = 0; i < list.numDocs(); i++) {
            dsl.set(i, list.getId(i), normalize(list.getScore(i)));
        }
        return dsl;
    }


    public String toString() { return "min=" + min + ", max=" + max; }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public abstract String dump();

    public boolean isTrained() {
        return trained;
    }
}
