package org.wikibrain.conf;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.impl.Parseable;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
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
    public Configuration(Map<String, Object> params, File ... files) {
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
                                Configuration.class.getClassLoader()))
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

    public Config getConfig(String ... path) {
        return config.getConfig(StringUtils.join(path, "."));
    }

    public int getInt(String ... path) {
        return config.getInt(StringUtils.join(path, "."));
    }

    public String getString(String ... path) {
        return config.getString(StringUtils.join(path, "."));
    }

    public File getFile(String ...path) {
        return new File(getString(path));
    }

    public Double getDouble(String ... path) {
        return config.getDouble(StringUtils.join(path, "."));
    }

    public boolean getBoolean(String ... path) {
        return config.getBoolean(StringUtils.join(path, "."));
    }

    public List<String> getStringList(String ... path) {
        return config.getStringList(StringUtils.join(path, "."));
    }

    public List<Double> getDoubleList(String ... path) {
        return config.getDoubleList(StringUtils.join(path, "."));
    }
}
