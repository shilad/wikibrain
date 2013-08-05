package org.wikapidia.lucene;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queries.ChainedFilter;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.standard.QueryParserUtil;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * This class provides various utilities for building different types of queries.
 *
 * @author Yulun Li
 * @author Ari Weiland
 *
 */
public class QueryBuilder {

    public static final int DEFAULT_MAX_PERCENTAGE = 10;
    public static final int DEFAULT_MAX_QUERY_TERMS = 100;
    public static final int DEFAULT_MIN_TERM_FREQ = 2;
    public static final int DEFAULT_MIN_DOC_FREQ = 2;
    public static final int DEFAULT_HIT_COUNT = 1000;



    private final Language language;
    private final LuceneSearcher searcher;
    private Query query = null;
    private int numHits = DEFAULT_HIT_COUNT;

    // For more like this queries
    private int maxPercentage = DEFAULT_MAX_PERCENTAGE;
    private int maxQueryTerms = DEFAULT_MAX_QUERY_TERMS;
    private int minTermFreq = DEFAULT_MIN_TERM_FREQ;
    private int minDocFreq = DEFAULT_MIN_DOC_FREQ;


    private static final Logger LOG = Logger.getLogger(QueryBuilder.class.getName());

    private final List<Filter> filters = new ArrayList<Filter>();

    public QueryBuilder(LuceneSearcher searcher, Language language) {
        this.searcher = searcher;
        this.language = language;
    }

    /**
     * Builds a phrase query for the default text field in LuceneOptions.
     *
     * @param searchString
     * @return
     * @throws ParseException
     */
    public QueryBuilder setPhraseQuery(String searchString) {
        return setPhraseQuery(searcher.getOptions().elements, searchString);
    }

    /**
     * Builds a phrase query for the text field specified by elements.
     *
     * @param elements specifies the text field in which to search
     * @param searchString
     * @return
     */
    public QueryBuilder setPhraseQuery(TextFieldElements elements, String searchString) {
        QueryParser parser = new QueryParser(
                searcher.getOptions().matchVersion,
                elements.getTextFieldName(),
                searcher.getAnalyzerByLanguage(language));
        try {
            query = parser.parse(QueryParserUtil.escape(searchString));
            return this;
        } catch (ParseException e) {
            throw new RuntimeException(e);  // should never happen after escaping
        }
    }

    public QueryBuilder setMoreLikeThisQuery(int luceneId) throws DaoException {
        return setMoreLikeThisQuery(
                searcher.getOptions().elements,
                luceneId);
    }

    public QueryBuilder setMoreLikeThisQuery(TextFieldElements elements, int luceneId) throws DaoException {
        if (luceneId >= 0) {
            try {
                MoreLikeThis mlt = new MoreLikeThis(searcher.getReaderByLanguage(language));
                mlt.setMaxDocFreqPct(maxPercentage);
                mlt.setMaxQueryTerms(maxQueryTerms);
                mlt.setMinDocFreq(minDocFreq);
                mlt.setMinTermFreq(minTermFreq);
                mlt.setAnalyzer(searcher.getAnalyzerByLanguage(language));
                mlt.setFieldNames(new String[]{elements.getTextFieldName()});
                query = mlt.like(luceneId);
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Can't more like this query for luceneId: " + luceneId);
            }
        }  else {
            LOG.log(Level.WARNING, "Can't more like this query for luceneId: " + luceneId);
        }
        return this;
    }

    public boolean hasQuery() {
        return query != null;
    }

    public QueryBuilder setNumHits(int hits) {
        this.numHits = hits;
        return this;
    }

    public WikapidiaScoreDoc[] search() {
        return searcher.search(query, language, numHits, getFilters());
    }

    /**
     * Adds a filter to the chain of filters. DOES NOT remove existing filters.
     */
    public void addFilter(Filter filter) {
        this.filters.add(filter);
    }

    public Filter getFilters() {
        if (filters.isEmpty()) {
            return null;
        } else if (filters.size() == 1) {
            return filters.get(0);
        } else {
            return new ChainedFilter((Filter[]) filters.toArray());
        }
    }
}
