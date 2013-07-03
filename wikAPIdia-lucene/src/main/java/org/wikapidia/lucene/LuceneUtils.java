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
 * The easiest way to use is with the import
 * "import static org.wikapidia.lucene.LuceneUtils.*;"
 */
public class LuceneUtils {
    public static final Configuration conf = new Configuration(null);

    public static final Version MATCH_VERSION = Version.parseLeniently(conf.get().getString("lucene.version"));

    public static final String LOCAL_ID_FIELD_NAME = conf.get().getString("lucene.fieldName.localId");
    public static final String LANG_ID_FIELD_NAME = conf.get().getString("lucene.fieldName.langId");
    public static final String WIKITEXT_FIELD_NAME = conf.get().getString("lucene.fieldName.wikitext");
    public static final String PLAINTEXT_FIELD_NAME = conf.get().getString("lucene.fieldName.plaintext");

    public static final File LUCENE_ROOT = new File(conf.get().getString("lucene.directory"));
}
