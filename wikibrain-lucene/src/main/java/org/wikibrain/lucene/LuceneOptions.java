package org.wikibrain.lucene;

import com.typesafe.config.Config;
import org.apache.commons.collections.CollectionUtils;
import org.apache.lucene.util.Version;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.model.NameSpace;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 *
 * This class can be instantiated through a configurator or by default configuration.
 * It provides access to all configurable options relevant to Lucene, such as version,
 * directory, and namespaces to index.  It also contains static final variables for
 * different field names. It should be passed to all classes in the lucene package.
 *
 * @author Ari Weiland
 *
 */
public class LuceneOptions {

    public static final String LOCAL_ID_FIELD_NAME = "local_id";
    public static final String LANG_ID_FIELD_NAME = "lang_id";

    public final String name;
    public final Configurator configurator;
    public final Version matchVersion;
    public final File luceneRoot;
    public final Collection<NameSpace> namespaces;
    public final TokenizerOptions options;
    public final TextFieldElements elements;

    /**
     * Used by provider only.
     */
    private LuceneOptions(String name, Configurator configurator, String matchVersion, String luceneRoot, List<String> namespaces, TokenizerOptions options, TextFieldElements elements) {
        this.name = name;
        this.configurator = configurator;
        this.matchVersion = Version.parseLeniently(matchVersion);
        this.luceneRoot = new File(luceneRoot);
        this.namespaces = new ArrayList<NameSpace>();
        for (String s : namespaces) {
            this.namespaces.add(NameSpace.getNameSpaceByName(s));
        }
        this.options = options;
        this.elements = elements;
    }

    /**
     * Returns a default set of LuceneOptions.
     *
     * @return a default set of LuceneOptions
     */
    public static LuceneOptions getDefaultOptions() {
        try {
            return new Configurator(new Configuration()).get(LuceneOptions.class, "plaintext");
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private static TokenizerOptions buildOptions(boolean caseInsensitive, boolean useStopWords, boolean useStem) {
        TokenizerOptions options = new TokenizerOptions();
        if (caseInsensitive) options.caseInsensitive();
        if (useStopWords) options.useStopWords();
        if (useStem) options.useStem();
        return options;
    }

    private static TextFieldElements buildElements(int title, boolean redirects, boolean plainText) {
        TextFieldElements elements = new TextFieldElements();
        elements.addTitle(title);
        if (redirects) elements.addRedirects();
        if (plainText) elements.addPlainText();
        return elements;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LuceneOptions)) return false;
        LuceneOptions opts = (LuceneOptions) o;
        return (this.name.equalsIgnoreCase(opts.name) &&
                this.matchVersion == opts.matchVersion &&
                this.luceneRoot.equals(opts.luceneRoot) &&
                CollectionUtils.isEqualCollection(this.namespaces, opts.namespaces) &&
                this.options.equals(opts.options) &&
                this.elements.equals(opts.elements));
    }

    public static class Provider extends org.wikibrain.conf.Provider<LuceneOptions> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return LuceneOptions.class;
        }

        @Override
        public String getPath() {
            return "lucene.options";
        }

        @Override
        public LuceneOptions get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!name.equalsIgnoreCase(config.getString("type"))) {
                throw new ConfigurationException("Could not find configuration " + name);
            }
            return new LuceneOptions(
                    name,
                    getConfigurator(),
                    config.getString("version"),
                    config.getString("directory"),
                    config.getStringList("namespaces"),
                    buildOptions(
                            config.getBoolean("caseInsensitive"),
                            config.getBoolean("useStopWords"),
                            config.getBoolean("useStem")),
                    buildElements(
                            config.getInt("title"),
                            config.getBoolean("redirects"),
                            config.getBoolean("plaintext"))
            );
        }
    }
}
