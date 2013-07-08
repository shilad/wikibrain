package org.wikapidia.lucene;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.RawPage;

/**
 *
 */
public class QueryBuilder {

    private final Language language;
    private final LuceneOptions options;

    public QueryBuilder(Language language, LuceneOptions options) {
        this.language = language;
        this.options = options;
    }

    public Query getPhraseQuery(String searchString) throws ParseException {
        return getPhraseQuery(LuceneOptions.PLAINTEXT_FIELD_NAME, searchString);
    }

    public Query getPhraseQuery(String fieldName, String searchString) throws ParseException {
        QueryParser parser = new QueryParser(options.matchVersion, fieldName, new WikapidiaAnalyzer(language, options));
        return parser.parse(searchString);
    }

    public Query getPageTextQuery(RawPage rawPage) throws ParseException {
        return getPhraseQuery(LuceneOptions.PLAINTEXT_FIELD_NAME, rawPage.getPlainText());
    }
}
