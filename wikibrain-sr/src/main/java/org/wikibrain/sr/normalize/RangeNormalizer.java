package org.wikibrain.sr.normalize;

import com.typesafe.config.Config;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;

import java.util.Map;

/**
 * Normalizes values to fall within a particular range.
 */
public class RangeNormalizer extends BaseNormalizer {
    protected boolean truncate;
    protected double desiredMin;
    protected double desiredMax;

    //TODO:This seems like a questionable thing to do.
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

    public static class Provider extends org.wikibrain.conf.Provider<RangeNormalizer> {
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
        public RangeNormalizer get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("range")) {
                return null;
            }

            return new RangeNormalizer(
                    config.getDouble("min"),
                    config.getDouble("max"),
                    config.getBoolean("truncate")
            );
        }

    }
}
