package org.wikibrain.sr.normalize;

import com.typesafe.config.Config;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.stat.ranking.NaNStrategy;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.TiesStrategy;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.utils.WbMathUtils;

import java.text.DecimalFormat;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Normalizes in two steps:
 * 1. Create a smoothed weighted average defined over a sample of the observed points.
 * 2. Creates a local linear spline fitted to smoothed points.
 */

public class LoessNormalizer extends BaseNormalizer {

    private static Logger LOG = LoggerFactory.getLogger(LoessNormalizer.class);
    public static final long serialVersionUID = -34232429;

    private TDoubleList X = new TDoubleArrayList();
    private TDoubleList Y = new TDoubleArrayList();
    private boolean logTransform = false;
    private boolean monotonic = false;

    transient private double interpolatorMin;
    transient private double interpolatorMax;
    transient private UnivariateFunction interpolator = null;

    @Override
    public void reset() {
        super.reset();
        X.clear();
        Y.clear();
        interpolatorMin = 0;
        interpolatorMax = 0;
        interpolator = null;
    }

    @Override
    public void observe(double x, double y){
        if (!Double.isNaN(x) && !Double.isInfinite(x)) {
            synchronized (X) {
                X.add(x);
                Y.add(y);
            }
        }
        super.observe(x, y);
    }

    @Override
    public void observationsFinished(){
        // lazily initialized to overcome problems
        // with PolynomialSplineFunction serialization.
        super.observationsFinished();

    }

    private static final double EPSILON = 1E-10;

    @Override
    public double normalize(double x) {
        if (Double.isNaN(x) || Double.isInfinite(x)) {
            return missingMean;
        }
        init();
        x = logIfNeeded(x);
        double sMin = interpolatorMin;
        double sMax = interpolatorMax;

        double x2;
        if (sMin <= x && x <= sMax) {
            x2 = getInterpolationFunction().value(x);
        } else {
            double yMin = getInterpolationFunction().value(sMin);
            double yMax = getInterpolationFunction().value(sMax);
            double halfLife = (sMax - sMin) / 4.0;
            double yDelta = 0.1 * (yMax - yMin);
            if (x < sMin) {
                x2 =  WbMathUtils.toAsymptote(sMin - x, halfLife, yMin, yMin - yDelta);
            } else if (x > sMax) {
                x2 = WbMathUtils.toAsymptote(x - sMax, halfLife, yMax, yMax + yDelta);
            } else {
                throw new IllegalStateException("" + x + " not in [" + sMin + "," + sMax + "]");
            }
        }
        return x2;
    }


    private synchronized  UnivariateFunction getInterpolationFunction() {
        init();
        return interpolator;
    }

    private synchronized void init() {
        if (interpolator != null) {
            return;
        }

        // remove infinite or nan values
        TDoubleList pruned[] = WbMathUtils.removeNotNumberPoints(X, Y);
        X = pruned[0];
        Y = pruned[1];

        // sort points by X coordinate
        double ranks[] =  new NaturalRanking(NaNStrategy.REMOVED, TiesStrategy.SEQUENTIAL).rank(X.toArray());
        if (ranks.length != X.size()) {
            throw new IllegalStateException("invalid sizes: " + ranks.length + " and " + X.size());
        }
        // spots in these arrays will be replaced.
        TDoubleList sortedX = new TDoubleArrayList(X);
        TDoubleList sortedY = new TDoubleArrayList(Y);
        for (int i = 0; i < X.size(); i++) {
            int r = (int)Math.round(ranks[i]) - 1;
            sortedX.set(r, X.get(i));
            sortedY.set(r, Y.get(i));
        }
        X = sortedX;
        Y = sortedY;

        // create the smoothed points.
        int windowSize = Math.min(20, X.size() / 10);
        double smoothed[][] = WbMathUtils.smooth(
                logIfNeeded(X.toArray()),
                Y.toArray(),
                windowSize,
                10);
        double smoothedX[] = smoothed[0];
        double smoothedY[] = smoothed[1];

        /*System.err.print("smoothed points: ");
        for (int i = 0; i < smoothedX.length; i++) {
            System.err.print(" (" + smoothedX[i] + ", " + smoothedY[i] + ")");
        }
        System.err.println();*/
        interpolatorMin = smoothedX[0];
        interpolatorMax = smoothedX[smoothedX.length - 1];

        WbMathUtils.makeMonotonicIncreasing(smoothedX, EPSILON);
        if (monotonic) {
            WbMathUtils.makeMonotonicIncreasing(smoothedY, EPSILON);
        }

        // create the interpolator
        interpolator = new LoessInterpolator().interpolate(smoothedX, smoothedY);
    }

    private double logIfNeeded(double x) {
        if (logTransform) {
            return (x < X.get(0)) ? 0 : Math.log(1 + X.get(0) + x);
        } else {
            return x;
        }
    }

    private double[] logIfNeeded(double X[]) {
        if (logTransform) {
            double X2[] = new double[X.length];
            for (int i = 0; i < X.length; i++) {
                X2[i] = logIfNeeded(X[i]);
            }
            return X2;
        } else {
            return X;
        }
    }

    @Override
    public String dump() {
        init();
        StringBuffer buff = new StringBuffer("loess normalizer");
        if (logTransform) buff.append(" (log'ed)");
        DecimalFormat df = new DecimalFormat("#.##");
        for (int i = 0; i <= 20; i++) {
            int j = Math.min(X.size() - 1, i * X.size() / 20);
            double x = X.get(j);
            buff.append(" <" +
                    df.format(x) + "," +
                    df.format(normalize(x)) + ">");
        }
        return buff.toString();
    }

    public void setLogTransform(boolean b) {
        this.logTransform = b;
    }

    public boolean getLogTransform() {
        return logTransform;
    }

    public void setMonotonic(boolean b) {
        this.monotonic = b;
    }

    public static class Provider extends org.wikibrain.conf.Provider<LoessNormalizer> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return Normalizer.class;
        }

        @Override
        public String getPath() {
            return "sr.normalizer";
        }

        @Override
        public Scope getScope() {
            return Scope.INSTANCE;
        }

        @Override
        public LoessNormalizer get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("loess")) {
                return null;
            }
            LoessNormalizer ln = new LoessNormalizer();
            if (config.hasPath("log")) {
                ln.setLogTransform(config.getBoolean("log"));
            }
            return ln;
        }

    }
}
