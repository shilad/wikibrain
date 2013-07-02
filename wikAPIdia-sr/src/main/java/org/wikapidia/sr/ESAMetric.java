package org.wikapidia.sr;

import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.utils.ESAAnalyzer;
import org.wikapidia.sr.utils.KnownSim;
import org.apache.lucene.util.Version;
import org.wikapidia.sr.utils.SimUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class ESAMetric extends BaseLocalSRMetric {

    public String getName() {
        return "ESA";
    }

    private IndexSearcher searcher;
    private static final Logger LOG = Logger.getLogger(ESAMetric.class.getName());
    private Analyzer analyzer = new ESAAnalyzer();

    /**
     * Get cosine similarity between two phrases.
     * @param phrase1
     * @param phrase2
     * @param language
     * @param explanations
     * @return
     * @throws DaoException
     */
    public SRResult similarity(String phrase1, String phrase2, Language language, boolean explanations) throws DaoException {
        if (phrase1 == null || phrase2 == null) {
            return new SRResult(Double.NaN);
        }
        TIntDoubleHashMap scores1 = getConceptVector(phrase1);
        TIntDoubleHashMap scores2 = getConceptVector(phrase2);
        double sim = SimUtils.cosineSimilarity(scores1, scores2);
        return new SRResult(sim); // TODO: normalize
    }

    /**
     * Get concept vector of a specified phrase.
     * @param phrase
     * @return
     */
    public TIntDoubleHashMap getConceptVector(String phrase) { // TODO: validIDs
        QueryParser parser = new QueryParser(Version.LUCENE_43, "text", analyzer);
        TopDocs docs = null;
        try {
            docs = searcher.search(parser.parse(phrase), 5000);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ParseException e) {
            LOG.log(Level.WARNING, "parsing of phrase " + phrase + " failed", e);
            return null;
        }
        // TODO: prune
        TIntDoubleHashMap result = expandScores(docs.scoreDocs);
        return result;
    }

    /**
     * Put data in a scoreDoc into a TIntDoubleHashMap
     * @param scores
     * @return
     */
    private TIntDoubleHashMap expandScores(ScoreDoc scores[]) {
        TIntDoubleHashMap expanded = new TIntDoubleHashMap();
        for (ScoreDoc sd : scores) {
            expanded.adjustOrPutValue(sd.doc, sd.score, sd.score);
        }
        return expanded;
    }

        @Override
    public SRResult similarity(LocalPage page1, LocalPage page2, boolean explanations) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SRResultList mostSimilar(LocalPage page, int maxResults, boolean explanations) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SRResultList mostSimilar(LocalPage page, int maxResults, boolean explanations, TIntSet validIds) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void write(File directory) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void read(File directory) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void trainSimilarity(List<KnownSim> labeled) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void trainMostSimilar(List<KnownSim> labeled, int numResults, TIntSet validIds) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double[][] cosimilarity(String[] phrases, Language language) {
        return new double[0][];  //To change body of implemented methods use File | Settings | File Templates.
    }
}
