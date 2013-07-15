package org.wikapidia.lucene;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.Query;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.phrases.BasePhraseAnalyzer;
import org.wikapidia.phrases.PhraseAnalyzer;

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

    public Query getPhraseQuery(String searchString) throws ParseException {
        return getPhraseQuery(new TextFieldElements().addPlainText(), searchString);
    }

    /**
     * Build a phrase query for the text field specified by elements
     *
     * @param elements specifies the text field in which to search
     * @param searchString
     * @return
     */
    public Query getPhraseQuery(TextFieldElements elements, String searchString) throws ParseException {
        QueryParser parser = new QueryParser(options.matchVersion, elements.getTextFieldName(), new WikapidiaAnalyzer(language, options));
        return parser.parse(searchString);
    }


    public Query getPageTextQuery(RawPage rawPage) throws ParseException {
        return getPhraseQuery(new TextFieldElements().addPlainText(), rawPage.getPlainText());
    }

    /**
     * Build a localpage query
     *
     * @param localPage
     * @return
     * @throws DaoException
     */
    public Query getLocalPageConceptQuery(LocalPage localPage) throws DaoException {
        LinkedHashMap<String, Float> description = phraseAnalyzer.describeLocal(language, localPage, 20);
        MultiPhraseQuery multiPhraseQuery = new MultiPhraseQuery();
        Term[] terms = new Term[description.keySet().size() + 1];
        terms[0] = new Term(TextFieldElements.getPlainTextFieldName(), localPage.getTitle().getCanonicalTitle());
        int i = 1;
        for (String phrase : description.keySet()) {
            terms[i] = new Term(TextFieldElements.getPlainTextFieldName(), phrase);
            i++;
        }
        multiPhraseQuery.add(terms);
        return multiPhraseQuery;
    }
}
