package org.wikibrain.sr.normalize;

import com.typesafe.config.Config;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;

import java.util.Map;

public class IdentityNormalizer extends BaseNormalizer{
    @Override
    public double normalize(double x) { return x; }

    @Override
    public void observe(double x, double y){}

    @Override
    public void observe(double x) {}

    @Override
    public void observationsFinished() {}

    @Override
    public String dump() {
        return "identity normalizer";
    }

    @Override
    public boolean isTrained() {
        return true;
    }

    public static class Provider extends org.wikibrain.conf.Provider<IdentityNormalizer> {
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
        public IdentityNormalizer get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("identity")) {
                return null;
            }

            return new IdentityNormalizer(
            );
        }

    }
}
