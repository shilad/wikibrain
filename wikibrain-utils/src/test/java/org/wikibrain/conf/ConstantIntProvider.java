package org.wikibrain.conf;

import com.typesafe.config.Config;

import java.util.Map;

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
    public String getPath() {
        return TestConfigurator.INTMAKER_PATH;
    }

    @Override
    public Integer get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
        if (!config.getString("type").equals("constant")) {
            return null;
        }
        if (runtimeParams != null && runtimeParams.containsKey("overrideConstant")) {
            return new Integer(Integer.valueOf(runtimeParams.get("overrideConstant")));
        } else {
            return new Integer(config.getInt("value"));
        }
    }
}
