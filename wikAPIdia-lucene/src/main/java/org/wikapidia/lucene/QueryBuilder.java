package org.wikapidia.lucene;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.Query;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.phrases.BasePhraseAnalyzer;

import java.util.LinkedHashMap;

/**
 *
 */
public class QueryBuilder {

    private final Language language;
    private final LuceneOptions options;
    private BasePhraseAnalyzer basePhraseAnalyzer;

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

    public Query getLocalPageConceptQuery(LocalPage localPage) throws DaoException {
        LinkedHashMap<String, Float> description = basePhraseAnalyzer.describeLocal(language, localPage, 20);
        MultiPhraseQuery multiPhraseQuery = new MultiPhraseQuery();
        for (String phrase : description.keySet()) {
            multiPhraseQuery.add(new Term(LuceneOptions.PLAINTEXT_FIELD_NAME, phrase));
        }
        return multiPhraseQuery;
    }
}
