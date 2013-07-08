package org.wikapidia.lucene.tokenizers;

import org.apache.lucene.util.Version;
import org.wikapidia.lucene.TokenizerOptions;

/**
 * @author Ari Weiland
 */
public class UkrainianTokenizer extends RussianTokenizer {
    public UkrainianTokenizer(Version version, TokenizerOptions options) {
        super(version, options);
    }
}
