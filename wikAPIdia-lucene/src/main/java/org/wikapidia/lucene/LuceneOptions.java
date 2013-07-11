package org.wikapidia.lucene;

import com.typesafe.config.Config;
import org.apache.lucene.util.Version;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.model.NameSpace;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
    public static final String WIKITEXT_FIELD_NAME = "wikitext";
    public static final String PLAINTEXT_FIELD_NAME = "plaintext";

    public final Configuration conf;
    public final Version matchVersion;
    public final File luceneRoot;
    public final Collection<NameSpace> namespaces;
    public final TokenizerOptions options;

    /**
     * Used by provider only
     */
    private LuceneOptions(Configuration conf, String matchVersion, String luceneRoot, List<String> namespaces, TokenizerOptions options) {
        this.conf = conf;
        this.matchVersion = Version.parseLeniently(matchVersion);
        this.luceneRoot = new File(luceneRoot);
        this.namespaces = new ArrayList<NameSpace>();
        for (String s : namespaces) {
            this.namespaces.add(NameSpace.getNameSpaceByName(s));
        }
        this.options = options;
    }

    /**
     * Returns a default set of LuceneOptions
     * @return a default set of LuceneOptions
     */
    public static LuceneOptions getDefaultOptions() {
        try {
            return new Configurator(new Configuration(null)).get(LuceneOptions.class, "options");
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

    public static class Provider extends org.wikapidia.conf.Provider<LuceneOptions> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return LuceneOptions.class;
        }

        @Override
        public String getPath() {
            return "lucene";
        }

        @Override
        public LuceneOptions get(String name, Config config) throws ConfigurationException {
            return new LuceneOptions(
                    getConfig(),
                    config.getString("version"),
                    config.getString("directory"),
                    config.getStringList("namespaces"),
                    buildOptions(
                            config.getBoolean("caseInsensitive"),
                            config.getBoolean("useStopWords"),
                            config.getBoolean("useStem")
                    )
            );
        }
    }
}
