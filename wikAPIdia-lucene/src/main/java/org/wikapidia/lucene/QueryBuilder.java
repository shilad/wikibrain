package org.wikapidia.lucene;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.wikapidia.core.model.RawPage;

/**
 *
 */
public class QueryBuilder {

    protected LuceneOptions opts;

    private final WikapidiaAnalyzer analyzer;

    public QueryBuilder(LuceneOptions opts, WikapidiaAnalyzer analyzer) {
        this.opts = opts;
        this.analyzer = analyzer;
    }

    public Query getPhraseQuery(String searchString) throws ParseException {
        return getPhraseQuery(LuceneOptions.PLAINTEXT_FIELD_NAME, searchString);
    }

    public Query getPhraseQuery(String fieldName, String searchString) throws ParseException {
        QueryParser parser = new QueryParser(opts.matchVersion, fieldName, analyzer);
        return parser.parse(searchString);
    }

    public Query getPageTextQuery(RawPage rawPage) throws ParseException {
        String rawPageText = rawPage.getPlainText();
        return getPhraseQuery(LuceneOptions.PLAINTEXT_FIELD_NAME, rawPageText);
    }
}
