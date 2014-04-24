package org.wikibrain.conf;

import com.typesafe.config.Config;

import java.util.Map;

public class ConsecutiveIntProvider extends Provider<Integer> {
    private int count = 0;

    /**
     * Creates a new provider instance.
     * Concrete implementations must only use this two-argument constructor.
     *
     * @param configurator
     * @param config
     */
    public ConsecutiveIntProvider(Configurator configurator, Configuration config) throws ConfigurationException {
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
        if (!config.getString("type").equals("consecutive")) {
            return null;
        }
        return new Integer(count++);
    }
}
