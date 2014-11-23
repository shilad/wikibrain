package org.wikibrain.lucene;

/**
 * This wraps the Lucene scoreDoc with simplified variables and
 * avoids other modules to depend on Lucene core.
 */
public class WikiBrainScoreDoc {
    public float score;

    public int luceneId;

    /**
     * If this is -1, it has not been looked up.
     */
    public int wpId = -1;

    public WikiBrainScoreDoc(int luceneId, int wpId, float score) {
        this.score = score;
        this.luceneId = luceneId;
        this.wpId = wpId;
    }

    // A convenience method for debugging.
    @Override
    public String toString() {
        return "luceneId=" + luceneId + " wpId=" + wpId + " score=" + score;
    }
}
