package org.wikapidia.sr.normalize;

import gnu.trove.list.array.TDoubleArrayList;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.wikapidia.utils.MathUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 * This class is called percentile normalizer, but it returns normalized values in [0,1].
 */
public class PercentileNormalizer extends BaseNormalizer {
    protected transient PolynomialSplineFunction interpolator;

    @Override
    public void observationsFinished() {
        super.observationsFinished();
        makeInterpolater();
    }

    protected void makeInterpolater() {
        TDoubleArrayList X = new TDoubleArrayList();
        TDoubleArrayList Y = new TDoubleArrayList();

        // save two "fake" sample observations worth of wiggle room for low and high out of range values.
        for (int i = 0; i < sample.size(); i++) {
            double fudge = max * 10E-8 * i;    // ensures monotonic increasing
            X.add(sample.get(i) + fudge);
            Y.add((i + 1.0) / (sample.size() + 1));
        }

        interpolator = new LinearInterpolator().interpolate(X.toArray(), Y.toArray());
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        makeInterpolater();
    }

    @Override
    public double normalize(double x) {
        double sMin = sample.get(0);
        double sMax = sample.get(sample.size() - 1);
        double halfLife = (sMax - sMin) / 4.0;
        double yDelta = 1.0 / (sample.size() + 1);

        if (x < sample.get(0)) {
            return MathUtils.toAsymptote(sMin - x, halfLife, yDelta, 0.0);
        } else if (x > sample.get(sample.size() - 1)) {
            return MathUtils.toAsymptote(x - sMax, halfLife, 1.0 - yDelta, 1.0);
        } else {
            return interpolator.value(x);
        }
    }

    @Override
    public String dump() {
        StringBuffer buff = new StringBuffer("percentile normalizer: ");
        for (int i = 0; i <= 20; i++) {
            int p = i * 100 / 20;
            int index = p * sample.size() / 100;
            index = Math.min(index, sample.size() - 1);
            buff.append(p + "%: ");
            buff.append(sample.get(index));
            buff.append(", ");
        }
        return buff.toString();
    }
}
