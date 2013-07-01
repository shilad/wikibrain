package org.wikapidia.sr;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.util.Version;

/**
 *
 * @author Ari Weiland
 *
 */
public class LuceneIndexer {

    private static final Logger LOG = Logger.getLogger(LuceneIndexer.class);

    private Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_43);

    public LuceneIndexer() {
    }


}
