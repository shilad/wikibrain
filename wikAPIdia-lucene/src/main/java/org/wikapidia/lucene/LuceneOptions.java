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
 * I had a lot of common static final parameters floating around,
 * so I am conglomerating them here.
 *
 * The easiest way to use is to just construct it as a protected
 * parameter in whatever class.
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

    public LuceneOptions() {
        this.conf = new Configuration();
        Config config = conf.get();
        matchVersion = Version.parseLeniently(config.getString("lucene.version"));
        luceneRoot = new File(config.getString("lucene.directory"));
        nameSpaces = new ArrayList<NameSpace>();
        List<String> nsStrings = config.getStringList("namespaces");
        for (String s : nsStrings) {
            nameSpaces.add(NameSpace.getNameSpaceByName(s));
        }
    }

    /**
     * Used by provider only
     * @param conf
     * @param matchVersion
     * @param luceneRoot
     * @param nameSpaces
     */
    private LuceneOptions(Configuration conf, String matchVersion, String luceneRoot, List<String> nameSpaces) {
        this.conf = conf;
        this.matchVersion = Version.parseLeniently(matchVersion);
        this.luceneRoot = new File(luceneRoot);
        this.nameSpaces = new ArrayList<NameSpace>();
        for (String s : nameSpaces) {
            this.nameSpaces.add(NameSpace.getNameSpaceByName(s));
        }
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
                    config.getStringList("namespace")
            );
        }
    }

}
