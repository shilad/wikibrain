package org.wikibrain.lucene.tokenizers;

import org.apache.lucene.util.Version;
import org.wikibrain.core.lang.Language;
import org.wikibrain.lucene.TokenizerOptions;

/**
 * @author Ari Weiland
 */
public class LadinoTokenizer extends SpanishTokenizer {
    protected LadinoTokenizer(Version version, TokenizerOptions options, Language language) {
        super(version, options, language);
    }
}
