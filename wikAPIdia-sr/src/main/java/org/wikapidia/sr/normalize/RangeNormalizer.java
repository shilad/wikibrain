package org.wikapidia.sr.normalize;

/**
 * Normalizes values to fall within a particular range.
 */
public class RangeNormalizer extends BaseNormalizer {
    protected boolean truncate;
    protected double desiredMin;
    protected double desiredMax;

    public RangeNormalizer() {
        super();
    }

    public RangeNormalizer(double desiredMin, double desiredMax, boolean truncate) {
        super();
        this.desiredMin = desiredMin;
        this.desiredMax = desiredMax;
        this.truncate = truncate;
    }

    public double normalize(double x) {
        if (truncate) {
            x = Math.max(x, min);
            x = Math.min(x, max);
        }
        return desiredMin + (desiredMax - desiredMin) * (x - min) / (max - min);
    }
    public double unnormalize(double x) {
        if (truncate) {
            x = Math.max(x, desiredMin);
            x = Math.min(x, desiredMax);
        }
        return min + (max - min) * (x - desiredMin) / (desiredMax - desiredMin);
    }

    @Override
    public String dump() {
        return ("range normalizer from [" +
                min + ", " + max + "] to [" +
                desiredMin + ", " + desiredMax + "]");
    }
}
