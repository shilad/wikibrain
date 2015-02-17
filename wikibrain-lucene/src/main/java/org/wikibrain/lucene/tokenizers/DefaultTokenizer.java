package org.wikibrain.lucene.tokenizers;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;
import org.wikibrain.core.lang.Language;
import org.wikibrain.lucene.TokenizerOptions;

/**
 *
 * This Tokenizer is designed to be able to tokenize a language that
 * does not have its own tokenizer. It will not do nearly as well a
 * job as a designated tokenizer, as it only applies standard and
 * lower case filters. Still, it should be somewhat effective, at
 * least on non-asian languages.
 *
 * @author Ari Weiland
 */
public class DefaultTokenizer extends LanguageTokenizer {

    protected DefaultTokenizer(Version version, TokenizerOptions tokenizerOptions, Language language) {
        super(version, tokenizerOptions, language);
    }

    @Override
    public TokenStream getTokenStream(Tokenizer tokenizer, CharArraySet stemExclusionSet) {
        TokenStream stream = new StandardFilter(matchVersion, tokenizer);
        if (caseInsensitive)
            stream = new LowerCaseFilter(matchVersion, stream);
        return stream;
    }
}
