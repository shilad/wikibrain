package org.wikibrain.lucene;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queries.ChainedFilter;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.standard.QueryParserUtil;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * This class provides various utilities for building different types of queries.
 *
 * @author Yulun Li
 * @author Ari Weiland
 *
 */
public class QueryBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(QueryBuilder.class);

    public static final int DEFAULT_MAX_PERCENTAGE = 5;
    public static final int DEFAULT_MAX_QUERY_TERMS = 20;
    public static final int DEFAULT_MIN_TERM_FREQ = 2;
    public static final int DEFAULT_MIN_DOC_FREQ = 5;
    public static final int DEFAULT_HIT_COUNT = 1000;

    private final Language language;
    private final LuceneSearcher searcher;
    private final List<Filter> filters = new ArrayList<Filter>();

    private Query query = null;
    private int numHits = DEFAULT_HIT_COUNT;

    // For more like this queries
    private int maxPercentage = DEFAULT_MAX_PERCENTAGE;
    private int maxQueryTerms = DEFAULT_MAX_QUERY_TERMS;
    private int minTermFreq = DEFAULT_MIN_TERM_FREQ;
    private int minDocFreq = DEFAULT_MIN_DOC_FREQ;

    // If true, lookup upwikipedia ids for lucene ids.
    private boolean resolveWikipediaIds = true;

    public QueryBuilder(LuceneSearcher searcher, Language language) {
        this.searcher = searcher;
        this.language = language;
    }

    /**
     * Builds a phrase query over the default text field in LuceneOptions.
     *
     * @param searchString
     * @return
     */
    public QueryBuilder setPhraseQuery(String searchString) {
        return setPhraseQuery(searcher.getOptions().elements, searchString);
    }

    /**
     * Builds a phrase query over the text field specified by elements.
     *
     * @param elements specifies the text field in which to search
     * @param searchString
     * @return
     */
    public QueryBuilder setPhraseQuery(TextFieldElements elements, String searchString) {
        return setPhraseQuery(elements.getTextFieldName(), searchString);
    }

    /**
     * Builds a phrase query over the specified field.
     *
     * @param fieldName the name of the field on which to search
     * @param searchString
     * @return
     */
    public QueryBuilder setPhraseQuery(String fieldName, String searchString) {
        QueryParser parser = new QueryParser(
                searcher.getOptions().matchVersion,
                fieldName,
                searcher.getAnalyzerByLanguage(language));
        try {
            searchString = QueryParserUtil.escape(searchString);
            // Lucene doesn't escape forward slash, but it needs to
            searchString = StringUtils.replace(searchString, "/", "\\/");
            query = parser.parse(searchString);
            return this;
        } catch (ParseException e) {
            throw new RuntimeException(e);  // should never happen after escaping
        }
    }

    /**
     * Builds a MoreLikeThis query for the specified luceneId over the
     * default text field in LuceneOptions.
     *
     * @param luceneId
     * @return
     * @throws DaoException
     */
    public QueryBuilder setMoreLikeThisQuery(int luceneId) throws DaoException {
        return setMoreLikeThisQuery(
                searcher.getOptions().elements,
                luceneId);
    }

    /**
     * Builds a MoreLikeThis query for the specified luceneId over the
     * text field specified by the TextFieldElements.
     *
     * @param elements
     * @param luceneId
     * @return
     * @throws DaoException
     */
    public QueryBuilder setMoreLikeThisQuery(TextFieldElements elements, int luceneId) throws DaoException {
        return setMoreLikeThisQuery(elements.getTextFieldName(), luceneId);
    }

    /**
     * Builds a MoreLikeThis query for the specified luceneId over the
     * specified text field.
     *
     * @param fieldName
     * @param luceneId
     * @return
     * @throws DaoException
     */
    public QueryBuilder setMoreLikeThisQuery(String fieldName, int luceneId) throws DaoException {
        if (luceneId >= 0) {
            try {
                MoreLikeThis mlt = new MoreLikeThis(searcher.getReaderByLanguage(language));
                mlt.setMaxDocFreqPct(maxPercentage);
                mlt.setMaxQueryTerms(maxQueryTerms);
                mlt.setMinDocFreq(minDocFreq);
                mlt.setMinTermFreq(minTermFreq);
                mlt.setAnalyzer(searcher.getAnalyzerByLanguage(language));
                mlt.setFieldNames(new String[]{ fieldName });
                query = mlt.like(luceneId);
            } catch (IOException e) {
                LOG.warn("Can't more like this query for luceneId: " + luceneId);
            }
        }  else {
            throw new IllegalArgumentException("Illegal Lucene ID: " + luceneId);
        }
        return this;
    }

    public boolean hasQuery() {
        return query != null;
    }

    public void setResolveWikipediaIds(boolean resolve) {
        this.resolveWikipediaIds = resolve;
    }

    public WikiBrainScoreDoc[] search() {
        if (!hasQuery()) {
            throw new IllegalArgumentException("no query specified. call one of the QueryBuilder.set* methods to specify a query");
        }
        return searcher.search(query, language, numHits, getFilters(), resolveWikipediaIds);
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

    public int getNumHits() {
        return numHits;
    }

    public QueryBuilder setNumHits(int hits) {
        this.numHits = hits;
        return this;
    }

    public int getMaxPercentage() {
        return maxPercentage;
    }

    public void setMaxPercentage(int maxPercentage) {
        this.maxPercentage = maxPercentage;
    }

    public int getMaxQueryTerms() {
        return maxQueryTerms;
    }

    public void setMaxQueryTerms(int maxQueryTerms) {
        this.maxQueryTerms = maxQueryTerms;
    }

    public int getMinTermFreq() {
        return minTermFreq;
    }

    public void setMinTermFreq(int minTermFreq) {
        this.minTermFreq = minTermFreq;
    }

    public int getMinDocFreq() {
        return minDocFreq;
    }

    public void setMinDocFreq(int minDocFreq) {
        this.minDocFreq = minDocFreq;
    }
}
