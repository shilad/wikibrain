package org.wikapidia.conf;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.impl.Parseable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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

    public Configuration(File file) {
        this(null, file);
    }

    /**
     * Creates a configuration using a specific series of overrides.
     * The order of priority from highest to lowest is:
     * - Parameter map
     * - System properties
     * - Files specified to constructor (in order)
     * - Defaults listed at https://github.com/typesafehub/config (i.e. reference.conf).
     *
     * @param params, File files
     */
    public Configuration(Map<String, String> params, File ... files) {
        Config config = ConfigFactory.empty();
        if (params != null)
            config = config.withFallback(ConfigFactory.parseMap(params));
        config = config.withFallback(ConfigFactory.defaultOverrides());
        for (File file : files) {
            if (file == null) {
                continue;
            }
            if (!file.isFile()) {
                throw new IllegalArgumentException("configuration file " + file + " does not exist");
            }
            config = config.withFallback(
                    Parseable.newFile(file, ConfigParseOptions.defaults()).parse());
        }
        config = config.withFallback(
                Parseable.newResources("reference.conf",
                        ConfigParseOptions.defaults().setClassLoader(
                                ClassLoader.getSystemClassLoader()))
                        .parse());
        this.config = config.resolve();
    }

    /**
     * Returns the sub config object.
     * @return
     */
    public Config get() {
        return config;
    }
}
