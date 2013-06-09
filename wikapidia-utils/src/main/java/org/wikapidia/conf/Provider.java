package org.wikapidia.conf;

import com.typesafe.config.Config;

/**
 * Generates some type of object using a configuration file
 */
public abstract class Provider<T> {
    protected final Configurator configurator;
    protected final Configuration config;

    /**
     * Creates a new provider instance.
     * Concrete implementations must only use this two-argument constructor.
     * @param configurator
     * @param config
     */
    public Provider(Configurator configurator, Configuration config) {
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
     * Should return a configured instance of the requested class,
     * or null if it cannot be created by this provider.
     */
    public abstract T get(String name, Class klass, Config config);
}
