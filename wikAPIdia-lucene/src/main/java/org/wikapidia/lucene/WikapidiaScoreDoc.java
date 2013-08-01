package org.wikapidia.lucene;

/**
 * This wraps the Lucene scoreDoc with simplified variables and
 * avoids other modules to depend on Lucene core.
 */
public class WikapidiaScoreDoc {
    public float score;

    public int doc;

    public WikapidiaScoreDoc(int doc, float score) {
        this.score = score;
        this.doc = doc;
    }

    // A convenience method for debugging.
    @Override
    public String toString() {
        return "doc=" + doc + " score=" + score;
    }
}
