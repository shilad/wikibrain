package org.wikapidia.lucene;

import com.typesafe.config.Config;
import org.apache.lucene.util.Version;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;

import java.io.File;

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
    public final Configuration conf;
    public final Version matchVersion;
    public final String localIdFieldName;
    public final String langIdFieldName;
    public final String wikitextFieldName;
    public final String plaintextFieldName;
    public final File luceneRoot;

    public LuceneOptions() {
        this.conf = new Configuration();
        Config config = conf.get();
        matchVersion = Version.parseLeniently(config.getString("lucene.version"));
        localIdFieldName = config.getString("lucene.fieldName.localId");
        langIdFieldName = config.getString("lucene.fieldName.langId");
        wikitextFieldName = config.getString("lucene.fieldName.wikitext");
        plaintextFieldName = config.getString("lucene.fieldName.plaintext");
        luceneRoot = new File(config.getString("lucene.directory"));
    }

    private LuceneOptions(Configuration conf, String matchVersion, String localIdFieldName, String langIdFieldName, String wikitextFieldName, String plaintextFieldName, String luceneRoot) {
        this.conf = conf;
        this.matchVersion = Version.parseLeniently(matchVersion);
        this.localIdFieldName = localIdFieldName;
        this.langIdFieldName = langIdFieldName;
        this.wikitextFieldName = wikitextFieldName;
        this.plaintextFieldName = plaintextFieldName;
        this.luceneRoot = new File(luceneRoot);
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
                    config.getString("fieldName.localId"),
                    config.getString("fieldName.langId"),
                    config.getString("fieldName.wikitext"),
                    config.getString("fieldName.plaintext"),
                    config.getString("directory")
            );
        }
    }

}
