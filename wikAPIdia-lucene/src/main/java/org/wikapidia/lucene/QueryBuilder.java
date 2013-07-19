package org.wikapidia.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.phrases.PhraseAnalyzer;

import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashMap;

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

    private int maxPercentage = DEFAULT_MAX_PERCENTAGE;
    private int maxQueryTerms = DEFAULT_MAX_QUERY_TERMS;
    private int minTermFreq = DEFAULT_MIN_TERM_FREQ;
    private int minDocFreq = DEFAULT_MIN_DOC_FREQ;

    private static final Logger LOG = Logger.getLogger(QueryBuilder.class.getName());

    private final Language language;
    private final LuceneOptions options;
    private final PhraseAnalyzer phraseAnalyzer;

    public QueryBuilder(Language language, LuceneOptions options) {
        this.language = language;
        this.options = options;
        try {
            this.phraseAnalyzer = new Configurator(new Configuration()).get(PhraseAnalyzer.class, "anchortext");
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Builds a phrase query for the default text field in LuceneOptions.
     *
     * @param searchString
     * @return
     * @throws ParseException
     */
    public Query getPhraseQuery(String searchString) throws ParseException {
        return getPhraseQuery(options.elements, searchString);
    }

    /**
     * Builds a phrase query for the text field specified by elements.
     *
     * @param elements specifies the text field in which to search
     * @param searchString
     * @return
     */
    public Query getPhraseQuery(TextFieldElements elements, String searchString) throws ParseException {
        QueryParser parser = new QueryParser(options.matchVersion, elements.getTextFieldName(), new WikapidiaAnalyzer(language, options));
        Query query = parser.parse(searchString);
        return query;
    }

    /**
     * Builds a local page query for the default text field in LuceneOptions.
     *
     * @param localPage
     * @return
     * @throws DaoException
     */
    public Query getLocalPageConceptQuery(LocalPage localPage) throws DaoException {
        try {
            return getLocalPageConceptQuery(options.elements, localPage);
        } catch (ParseException e) {
            throw new DaoException(e);
        }
    }

    /**
     *
     * @param elements elements specifies the text field in which to search
     * @param localPage
     * @return
     * @throws DaoException
     * @throws ParseException
     */
    public Query getLocalPageConceptQuery(TextFieldElements elements, LocalPage localPage) throws DaoException, ParseException {
        LinkedHashMap<String, Float> description = phraseAnalyzer.describeLocal(language, localPage, 20);
        BooleanQuery query = new BooleanQuery();
        query.add(getPhraseQuery(localPage.getTitle().getCanonicalTitle()), BooleanClause.Occur.SHOULD);
        for (String similarTitle : description.keySet()) {
            query.add(getPhraseQuery(similarTitle), BooleanClause.Occur.SHOULD);
        }
        return query;

    }

    public Query getMoreLikeThisQuery(int luceneId, DirectoryReader directoryReader) throws DaoException {
        return getMoreLikeThisQuery(options.elements, luceneId, directoryReader);
    }

    public Query getMoreLikeThisQuery(TextFieldElements elements, int luceneId, DirectoryReader directoryReader) throws DaoException {
        if (luceneId >= 0) {
            try {
                MoreLikeThis mlt = getMoreLikeThis(directoryReader, elements);
                Query query = mlt.like(luceneId);
                return query;
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Can't more like this query for luceneId: " + luceneId);
                return null;
            }
        }  else {
            return null;
        }
    }

    private MoreLikeThis getMoreLikeThis(DirectoryReader reader, TextFieldElements elements) {
        MoreLikeThis mlt = new MoreLikeThis(reader); // Pass the reader reader
        mlt.setMaxDocFreqPct(maxPercentage);
        mlt.setMaxQueryTerms(maxQueryTerms);
        mlt.setMinDocFreq(minDocFreq);
        mlt.setMinTermFreq(minTermFreq);
        mlt.setAnalyzer(new WikapidiaAnalyzer(language, options));
        mlt.setFieldNames(new String[]{elements.getTextFieldName()}); // specify the fields for similiarity
        return mlt;
    }

}
