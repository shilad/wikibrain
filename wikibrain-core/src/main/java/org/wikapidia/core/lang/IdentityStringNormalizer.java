package org.wikapidia.core.lang;

import com.typesafe.config.Config;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;

import java.util.Map;

/**
 * A string normalizer that does nothing.
 * Fancier string normalizers are in the lucene package.
 *
 * @author Shilad Sen
 */
public class IdentityStringNormalizer implements StringNormalizer {
    @Override
    public String normalize(Language language, String text) {
        return text;
    }

    @Override
    public String normalize(LocalString text) {
        return text.getString();
    }

    public static class Provider extends org.wikapidia.conf.Provider<StringNormalizer> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class<StringNormalizer> getType() {
            return StringNormalizer.class;
        }

        @Override
        public String getPath() {
            return "stringnormalizers";
        }

        @Override
        public StringNormalizer get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (config.getString("type").equals("identity")) {
                return new IdentityStringNormalizer();
            } else {
                return null;
            }
        }
    }
}
