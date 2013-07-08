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
 * @author Ari Weiland
 *
 * This class can be instantiated through a configurator or by default configuration.
 * It provides access to all configurable options relevant to Lucene, such as version,
 * directory, and namespaces to index.  It also contains static final variables for
 * different field names. It should be passed to all classes in the lucene package.
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
    public final Collection<NameSpace> nameSpaces;
    public final TokenizerOptions options;

//    public LuceneOptions() {
//        this.conf = new Configuration(null);
//        Config config = conf.get();
//        matchVersion = Version.parseLeniently(config.getString("lucene.options.version"));
//        luceneRoot = new File(config.getString("lucene.options.directory"));
//        nameSpaces = new ArrayList<NameSpace>();
//        List<String> nsStrings = config.getStringList("lucene.options.namespaces");
//        for (String s : nsStrings) {
//            nameSpaces.add(NameSpace.getNameSpaceByName(s));
//        }
//        options = buildOptions(
//                config.getBoolean("lucene.options.caseInsensitive"),
//                config.getBoolean("lucene.options.useStopWords"),
//                config.getBoolean("lucene.options.useStem")
//        );
//    }

    /**
     * Used by provider only
     */
    private LuceneOptions(Configuration conf, String matchVersion, String luceneRoot, List<String> nameSpaces, TokenizerOptions options) {
        this.conf = conf;
        this.matchVersion = Version.parseLeniently(matchVersion);
        this.luceneRoot = new File(luceneRoot);
        this.nameSpaces = new ArrayList<NameSpace>();
        for (String s : nameSpaces) {
            this.nameSpaces.add(NameSpace.getNameSpaceByName(s));
        }
        this.options = options;
    }

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
