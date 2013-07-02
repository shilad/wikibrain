package org.wikapidia.parser.wiki;

import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.sr.lucene.LuceneIndexer;

import java.io.IOException;

/**
 *
 * @author Ari Weiland
 *
 */
public class LuceneVisitor extends ParserVisitor {

    private LuceneIndexer indexer;

    public LuceneVisitor() throws IOException {
        indexer = new LuceneIndexer();
    }

    @Override
    public void beginPage(RawPage xml) throws WikapidiaException {
        indexer.indexPage(xml);
    }
}
