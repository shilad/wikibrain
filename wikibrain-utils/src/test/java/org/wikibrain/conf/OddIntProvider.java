package org.wikibrain.conf;

import com.typesafe.config.Config;

import java.util.Map;

public class OddIntProvider extends Provider<Integer> {
    private int count = 1;

    /**
     * Creates a new provider instance.
     * Concrete implementations must only use this two-argument constructor.
     *
     * @param configurator
     * @param config
     */
    public OddIntProvider(Configurator configurator, Configuration config) throws ConfigurationException {
        super(configurator, config);
    }

    @Override
    public Class getType() {
        return Integer.class;
    }

    @Override
    public String getPath() {
        return TestConfigurator.INTMAKER_PATH;
    }

    @Override
    public Integer get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
        if (!config.getString("type").equals("odd")) {
            return null;
        }
        int result = count;
        count += 2;
        return new Integer(result);
    }
}
