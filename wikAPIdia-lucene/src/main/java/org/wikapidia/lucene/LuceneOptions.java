package org.wikapidia.lucene;

import org.apache.lucene.util.Version;
import org.wikapidia.conf.Configuration;

import java.io.File;

/**
 *
 * @author Ari Weiland
 *
 * I had a lot of common static final parameters floating around,
 * so I am conglomerating them here.
 *
 * The easiest way to use is to just construct it as a protected
 * parameter in whatever class.
 */
public class LuceneOptions {
    public final Configuration conf;
    public final Version MATCH_VERSION;
    public final String LOCAL_ID_FIELD_NAME;
    public final String LANG_ID_FIELD_NAME;
    public final String WIKITEXT_FIELD_NAME;
    public final String PLAINTEXT_FIELD_NAME;
    public final File LUCENE_ROOT;

    public LuceneOptions(Configuration conf) {
        this.conf = conf;
        MATCH_VERSION = Version.parseLeniently(conf.get().getString("lucene.version"));
        LOCAL_ID_FIELD_NAME = conf.get().getString("lucene.fieldName.localId");
        LANG_ID_FIELD_NAME = conf.get().getString("lucene.fieldName.langId");
        WIKITEXT_FIELD_NAME = conf.get().getString("lucene.fieldName.wikitext");
        PLAINTEXT_FIELD_NAME = conf.get().getString("lucene.fieldName.plaintext");
        LUCENE_ROOT = new File(conf.get().getString("lucene.directory"));
    }
}
