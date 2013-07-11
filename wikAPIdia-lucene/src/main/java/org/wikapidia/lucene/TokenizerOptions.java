package org.wikapidia.lucene;

/**
 *
 * A wrapper class that uses a simple builder pattern to select filters to use by the Analyzer.
 *
 * @author Ari Weiland
 *
 */
public class TokenizerOptions {
    private boolean caseInsensitive;
    private boolean useStopWords;
    private boolean useStem;

    public TokenizerOptions() {
        caseInsensitive = false;
        useStopWords = false;
        useStem = false;
    }

    public TokenizerOptions caseInsensitive() {
        caseInsensitive = true;
        return this;
    }

    public TokenizerOptions useStopWords() {
        useStopWords = true;
        return this;
    }

    public TokenizerOptions useStem() {
        useStem = true;
        return this;
    }

    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }

    public boolean doesUseStopWords() {
        return useStopWords;
    }

    public boolean doesUseStem() {
        return useStem;
    }
}
