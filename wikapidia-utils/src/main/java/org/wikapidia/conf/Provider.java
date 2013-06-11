package org.wikapidia.conf;

import com.typesafe.config.Config;

/**
 * Generates some type of object using a configuration file
 */
public abstract class Provider<T> {
    private final Configurator configurator;
    private final Configuration config;

    /**
     * Creates a new provider instance.
     * Concrete implementations must only use this two-argument constructor.
     * @param configurator
     * @param config
     */
    public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
        this.configurator = configurator;
        this.config = config;
    }

    /**
     * Returns the base class or interface that the class provides.
     * This should return, e.g LocalPageDao.class, not SqlLocalPageDao.class.
     * There may be other providers that provide the same interface.
     * @return class
     */
    public abstract Class getType();

    /**
     * Returns the path in the configuration file for the components.
     * For example: 'dao.dataSource'.
     *
     * The configuration at this location is actually a dictionary whose
     * keys are different names for the options for this component and
     * values are the configuration for that option.
     *
     * For example, in the previous example, dao.dataSource could
     * have configuration { h2 : {..}, mysql: {..} }, and it would thus
     * have two different named options.
     */
    public abstract String getPath();

    /**
     * Should return a configured instance of the requested class,
     * or null if it cannot be created by this provider.
     */
    public abstract T get(String name, Config config) throws ConfigurationException;

    public Configurator getConfigurator() {
        return configurator;
    }

    public Configuration getConfig() {
        return config;
    }
}
