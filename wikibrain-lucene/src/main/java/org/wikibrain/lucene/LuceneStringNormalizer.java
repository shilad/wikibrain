package org.wikibrain.lucene;

import com.typesafe.config.Config;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalString;
import org.wikibrain.core.lang.StringNormalizer;
import org.wikibrain.lucene.tokenizers.LanguageTokenizer;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Normalizes a string into its canonical form using language-specific tokenizers.
 *
 * @author Shilad Sen
 */
public class LuceneStringNormalizer implements StringNormalizer {
    private final Version version;
    private final TokenizerOptions options;
    private final Map<Language, LanguageTokenizer> tokenizers = new ConcurrentHashMap<Language, LanguageTokenizer>();

    public LuceneStringNormalizer(TokenizerOptions options, Version version) {
        this.options = options;
        this.version = version;
    }

    public LanguageTokenizer getTokenizer(Language language) {
        if (!tokenizers.containsKey(language)) {
            tokenizers.put(language, LanguageTokenizer.getLanguageTokenizer(language, options, version));
        }
        return tokenizers.get(language);
    }

    @Override
    public String normalize(LocalString string) {
        return normalize(string.getLanguage(), string.getString());
    }

    @Override
    public String normalize(Language language, String text) {
        StringBuilder normalized = new StringBuilder();
        try {
            TokenStream stream = getTokenizer(language).getTokenStream(new StringReader(text));
            CharTermAttribute cattr = stream.addAttribute(CharTermAttribute.class);
            stream.reset();
            while (stream.incrementToken()) {
                if (normalized.length() > 0) {
                    normalized.append(' ');
                }
                normalized.append(cattr.toString());
            }
            stream.end();
            stream.close();
            return normalized.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Provider extends org.wikibrain.conf.Provider<StringNormalizer> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class<StringNormalizer> getType() {
            return StringNormalizer.class;
        }

        @Override
        public String getPath() {
            return "stringnormalizers";
        }

        @Override
        public StringNormalizer get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("lucene")) {
                return null;
            }
            Version version = Version.parseLeniently(config.getString("version"));
            TokenizerOptions opts = new TokenizerOptions(
                                        config.getBoolean("caseInsensitive"),
                                        config.getBoolean("useStopWords"),
                                        config.getBoolean("useStem")
                                );

            return new LuceneStringNormalizer(opts, version);
        }
    }
}
