package org.wikapidia.lucene;

/**
 *
 * @author Ari Weiland
 *
 * A wrapper class that uses a simple builder pattern to select filters to use by the Analyzer.
 *
 */
public class FilterSelect {
    private boolean caseInsensitive;
    private boolean useStopWords;
    private boolean useStem;

    public FilterSelect() {
        caseInsensitive = false;
        useStopWords = false;
        useStem = false;
    }

    public FilterSelect caseInsensitive() {
        caseInsensitive = true;
        return this;
    }

    public FilterSelect useStopWords() {
        useStopWords = true;
        return this;
    }

    public FilterSelect useStem() {
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
