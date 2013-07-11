package org.wikapidia.lucene;

/**
 * @author Ari Weiland
 */
public class LuceneException extends Exception { // TODO: extend WikapidiaException instead?

    public LuceneException() {
    }

    public LuceneException(String s) {
        super(s);
    }

    public LuceneException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public LuceneException(Throwable throwable) {
        super(throwable);
    }
}
