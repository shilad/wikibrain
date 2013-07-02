package org.wikapidia.sr.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.util.Version;
import org.wikapidia.conf.Configuration;

/**
 *
 * @author Ari Weiland
 *
 */
public class ScoredArticle {

    public static final String CONF_PATH = "parser.lucene.";
    private static Configuration conf = new Configuration(null);

    public static final Version MATCH_VERSION = Version.parseLeniently(conf.get().getString(CONF_PATH + "version"));
    public static final String LOCAL_ID_FIELD_NAME = conf.get().getString(CONF_PATH + "localId");
    public static final String WIKITEXT_FIELD_NAME = conf.get().getString(CONF_PATH + "wikitext");
    public static final String PLAINTEXT_FIELD_NAME = conf.get().getString(CONF_PATH + "plaintext");

    private final Document doc;
    private final double score;
    private final int pageId;

    public ScoredArticle(Document doc, ScoreDoc score) {
        this.doc = doc;
        this.score = score.score;
        pageId = (Integer) doc.getField(LOCAL_ID_FIELD_NAME).numericValue();
    }

    public ScoredArticle(Document doc, double score) {
        this.doc = doc;
        this.score = score;
        pageId = (Integer) doc.getField(LOCAL_ID_FIELD_NAME).numericValue();
    }

    public Document getDoc() {
        return doc;
    }

    public double getScore() {
        return score;
    }

    public int getPageId() {
        return pageId;
    }
}
