package org.wikibrain.lucene;

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

    public TokenizerOptions(boolean caseInsensitive, boolean useStopWords, boolean useStem) {
        this.caseInsensitive = caseInsensitive;
        this.useStopWords = useStopWords;
        this.useStem = useStem;
    }

    /**
     * Sets the option to filter out case.
     * @return
     */
    public TokenizerOptions caseInsensitive() {
        caseInsensitive = true;
        return this;
    }

    /**
     * Sets the option to filter out stop words.
     * @return
     */
    public TokenizerOptions useStopWords() {
        useStopWords = true;
        return this;
    }

    /**
     * Sets the option to filter using stemming.
     * Note that stemming generally requires case insensitivity,
     * so this also sets the option to filter out case.
     * @return
     */
    public TokenizerOptions useStem() {
        caseInsensitive = true;
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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TokenizerOptions)) return false;
        TokenizerOptions opts = (TokenizerOptions) o;
        return (this.caseInsensitive == opts.caseInsensitive &&
                this.useStopWords == opts.useStopWords &&
                this.useStem == opts.useStem);
    }
}
