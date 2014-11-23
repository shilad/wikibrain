package org.wikibrain.sr.normalize;

import com.typesafe.config.Config;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;

import java.util.Map;

/**
 * A simple normalizer that returns log(1 + min-score).
 * In the case that an x is observed that is less than min-score, it returns 0.
 */
public class LogNormalizer implements Normalizer{
    private double c=Double.POSITIVE_INFINITY;
    private boolean trained = false;

    @Override
    public void reset() {
        c = Double.POSITIVE_INFINITY;
        trained = false;
    }

    @Override
    public SRResultList normalize(SRResultList list) {
        SRResultList normalized = new SRResultList(list.numDocs());
        for (int i = 0; i < list.numDocs(); i++) {
            normalized.set(i, list.getId(i), normalize(list.getScore(i)));
        }
        return normalized;
    }

    @Override
    public double normalize(double x) {
        if (Double.isNaN(x)) {
            return x;
        } else if (1 + x < c) {
            return 0;
        } else {
            return Math.log(c + x);
        }
    }

    @Override
    public void observe(SRResultList sims, int rank, double y) {
        for (SRResult sr : sims) {
            observe(sr.getScore());
        }
    }

    @Override
    public void observe(double x, double y) {
        observe(x);
    }

    @Override
    public void observe(double x) {
        if (!Double.isNaN(x) && !Double.isInfinite(x))  {
            c = Math.min(1 +x,c);
        }
    }

    @Override
    public void observationsFinished() {
        trained = true;
    }

    @Override
    public boolean isTrained() {
        return trained;
    }

    @Override
    public String dump() {
        return "log normalizer: log(" + c + " + x)";
    }

    public static class Provider extends org.wikibrain.conf.Provider<LogNormalizer> {
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
        public LogNormalizer get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("log")) {
                return null;
            }

            return new LogNormalizer(
            );
        }

    }
}
