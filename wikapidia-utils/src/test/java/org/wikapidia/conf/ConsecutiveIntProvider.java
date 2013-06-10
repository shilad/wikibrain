package org.wikapidia.conf;

import com.typesafe.config.Config;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.Provider;

import static org.junit.Assert.assertEquals;

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
    public Integer get(String name, Class klass, Config config) throws ConfigurationException {
        if (!config.getString("type").equals("consecutive")) {
            return null;
        }
        assertEquals(klass, Integer.class);
        return count++;
    }
}
