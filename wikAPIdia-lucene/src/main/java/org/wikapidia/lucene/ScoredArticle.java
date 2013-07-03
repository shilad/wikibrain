package org.wikapidia.lucene;

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

    public static final String CONF_PATH = "lucene.";
    private static Configuration conf = new Configuration(null);

    public static final Version MATCH_VERSION = Version.parseLeniently(conf.get().getString("lucene.version"));
    public static final String LOCAL_ID_FIELD_NAME = conf.get().getString("lucene.localId");
    public static final String LANG_ID_FIELD_NAME = conf.get().getString("lucene.langId");
    public static final String WIKITEXT_FIELD_NAME = conf.get().getString("lucene.wikitext");
    public static final String PLAINTEXT_FIELD_NAME = conf.get().getString("lucene.plaintext");

    private final Document doc;
    private final double score;
    private final int pageId;
    private final int langId;

    public ScoredArticle(Document doc, ScoreDoc score) {
        this.doc = doc;
        this.score = score.score;
        pageId = (Integer) doc.getField(LOCAL_ID_FIELD_NAME).numericValue();
        langId = (Integer) doc.getField(LANG_ID_FIELD_NAME).numericValue();
    }

    public ScoredArticle(Document doc, double score) {
        this.doc = doc;
        this.score = score;
        pageId = (Integer) doc.getField(LOCAL_ID_FIELD_NAME).numericValue();
        langId = (Integer) doc.getField(LANG_ID_FIELD_NAME).numericValue();
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

    public int getLangId() {
        return langId;
    }
}
