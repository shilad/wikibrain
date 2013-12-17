package org.wikapidia.sr.evaluation;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.sr.LocalSRMetric;
import org.wikapidia.sr.disambig.Disambiguator;

import java.util.Map;

/**
 * Factory that constructs an SR metric using the config file.
 *
 * @author Shilad Sen
 */
public class ConfigLocalSRFactory implements LocalSRFactory {
    private final Configurator configurator;
    private final String name;
    private Config config;
    /**
     * Constructs a new factory that creates an sr metric with a particular name from the config
     * file. The overrides take precedence over any configuration parameters, and are relative to
     * the innermost configuration block for an SR metric (i.e. the nested dictionary with key
     * the name of the metric).
     *
     * @param configurator
     * @param name Name of metric from configuration file
     * @throws ConfigurationException
     */
    public ConfigLocalSRFactory(Configurator configurator, String name) throws ConfigurationException {
        this(configurator, name, null);
    }

    /**
     * Constructs a new factory that creates an sr metric with a particular name from the config
     * file. The overrides take precedence over any configuration parameters, and are relative to
     * the innermost configuration block for an SR metric (i.e. the nested dictionary with key
     * the name of the metric).
     *
     * @param configurator
     * @param name Name of metric from configuration file
     * @param configOverrides Optional configuration overrides, or null.
     * @throws ConfigurationException
     */
    public ConfigLocalSRFactory(Configurator configurator, String name, Map<String, Object> configOverrides) throws ConfigurationException {
        this.config = ConfigFactory.empty();
        if (configOverrides != null) {
            config = config.withFallback(ConfigFactory.parseMap(configOverrides));
        }
        config = config.withFallback(configurator.getConfig(LocalSRMetric.class, name));
        this.configurator = configurator;
        this.name = name;
    }

    @Override
    public LocalSRMetric create() {
        try {
            return configurator.construct(LocalSRMetric.class, name, config, null);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String describeDisambiguator() {
        if (!config.hasPath("disambiguator")){
            return "none";
        }
        String disambigName = config.getString("disambiguator");
        try {
            Map dc = configurator.getConfig(Disambiguator.class, disambigName).root().unwrapped();
            String phraseName = null;
            if (dc.containsKey("phraseAnalyzer")) {
                phraseName = (String) dc.get("phraseAnalyzer");
            }
            if (phraseName == null || phraseName.equals("default")) {
                phraseName = configurator.getConf().get().getString("phrases.analyzer.default");
            }
            dc.put("phraseAnalyzer", phraseName);
            return disambigName + "=" + dc.toString();
        } catch (ConfigurationException e) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public String describeMetric() {
        return name + "=" + config.root().unwrapped();
    }

    @Override
    public String getName() {
        return name;
    }
}
