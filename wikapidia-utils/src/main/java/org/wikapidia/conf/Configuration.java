package org.wikapidia.conf;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;

/**
 * A generic configuration file.
 * The format of this file is based on the typesafe config project:
 * https://github.com/typesafehub/config
 */
public class Configuration {
    private final Config config;


    /**
     * Creates a configuration based on default settings.
     */
    public Configuration() {
        this(null);
    }

    /**
     * Creates a configuration using
     * @param file
     */
    public Configuration(File file) {
        this.config = ConfigFactory.load(file.getAbsolutePath());
    }

    /**
     * Returns the sub config object.
     * @return
     */
    public Config get() {
        return config;
    }
}
