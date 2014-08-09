package org.wikibrain.sr.evaluation;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.lang.Language;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.disambig.Disambiguator;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory that constructs an SR metric using the config file.
 *
 * @author Shilad Sen
 */
public class ConfigMonolingualSRFactory implements MonolingualSRFactory {
    private final Configurator configurator;
    private final String name;
    private final Language language;
    private Config config;
    /**
     * Constructs a new factory that creates an sr metric with a particular name from the config
     * file. The overrides take precedence over any configuration parameters, and are relative to
     * the innermost configuration block for an SR metric (i.e. the nested dictionary with key
     * the name of the metric).
     *
     * @param language
     * @param configurator
     * @param name Name of metric from configuration file
     * @throws ConfigurationException
     */
    public ConfigMonolingualSRFactory(Language language, Configurator configurator, String name) throws ConfigurationException {
        this(language, configurator, name, null);
    }

    /**
     * Constructs a new factory that creates an sr metric with a particular name from the config
     * file. The overrides take precedence over any configuration parameters, and are relative to
     * the innermost configuration block for an SR metric (i.e. the nested dictionary with key
     * the name of the metric).
     *
     * @param language
     * @param configurator
     * @param name Name of metric from configuration file
     * @param configOverrides Optional configuration overrides, or null.
     * @throws ConfigurationException
     */
    public ConfigMonolingualSRFactory(Language language, Configurator configurator, String name, Map<String, Object> configOverrides) throws ConfigurationException {
        this.config = ConfigFactory.empty();
        if (configOverrides != null) {
            config = config.withFallback(ConfigFactory.parseMap(configOverrides));
        }
        config = config.withFallback(configurator.getConfig(SRMetric.class, name));
        this.configurator = configurator;
        this.name = name;
        this.language = language;
    }

    @Override
    public SRMetric create() {
        try {
            Map<String, String> runtimeParams = new HashMap<String, String>();
            runtimeParams.put("language", language.getLangCode());
            return configurator.construct(SRMetric.class, name, config, runtimeParams);
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
            throw new IllegalStateException(e);
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
