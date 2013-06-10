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
     * Creates a configuration using a specific file that overrides the standard
     * defaults listed at https://github.com/typesafehub/config. The file is loaded
     * in ADDITION to the standard files, but it takes precedence.
     *
     * @param file
     */
    public Configuration(File file) {
        if (file == null) {
            this.config = ConfigFactory.load();
        } else {
            this.config = ConfigFactory.load(ConfigFactory.parseFile(file));
        }
    }

    /**
     * Returns the sub config object.
     * @return
     */
    public Config get() {
        return config;
    }
}
