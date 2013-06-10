package org.wikapidia.conf;

import com.typesafe.config.Config;

public class ConstantIntProvider extends Provider<Integer> {
    /**
     * Creates a new provider instance.
     * Concrete implementations must only use this two-argument constructor.
     *
     * @param configurator
     * @param config
     */
    public ConstantIntProvider(Configurator configurator, Configuration config) throws ConfigurationException {
        super(configurator, config);
    }

    @Override
    public Class getType() {
        return Integer.class;
    }

    @Override
    public Integer get(String name, Config config) throws ConfigurationException {
        if (!config.getString("type").equals("constant")) {
            return null;
        }
        return config.getInt("value");
    }
}
