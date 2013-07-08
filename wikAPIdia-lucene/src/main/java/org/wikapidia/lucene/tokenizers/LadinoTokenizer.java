package org.wikapidia.lucene.tokenizers;

import org.apache.lucene.util.Version;
import org.wikapidia.core.lang.Language;
import org.wikapidia.lucene.TokenizerOptions;

/**
 * @author Ari Weiland
 */
public class LadinoTokenizer extends SpanishTokenizer {
    protected LadinoTokenizer(Version version, TokenizerOptions options, Language language) {
        super(version, options, language);
    }
}
