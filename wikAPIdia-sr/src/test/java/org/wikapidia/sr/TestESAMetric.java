package org.wikapidia.sr;

import org.apache.lucene.util.Version;
import org.junit.Test;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.lucene.LuceneOptions;
import org.wikapidia.lucene.LuceneSearcher;

import java.util.Arrays;

/**
 *
 */
public class TestESAMetric {

//    @Test
//    public void testLuceneOptions() {
//        LuceneOptions options = LuceneOptions.getDefaultOptions();
//        assert (options.matchVersion == Version.LUCENE_43);
//    }

    @Test
    public void testMostSimilarPages() throws WikapidiaException, DaoException, ConfigurationException {
        Language testLanguage = Language.getByLangCode("simple");
        LuceneSearcher searcher = new LuceneSearcher(new LanguageSet(Arrays.asList(testLanguage)), LuceneOptions.getDefaultOptions());
        ESAMetric esaMetric = new ESAMetric(testLanguage, searcher);
        LocalPage page = new Configurator(new Configuration()).get(LocalPageDao.class).getById(testLanguage, 6);
        System.out.println(page);
        SRResultList srResults= esaMetric.mostSimilar(page, 100, false);
        System.out.println(Arrays.toString(srResults.getScoresAsFloat()));
    }

    @Test
    public void testPhraseSimilarity() throws DaoException {
        Language testLanguage = Language.getByLangCode("simple");
        LuceneSearcher searcher = new LuceneSearcher(new LanguageSet(Arrays.asList(testLanguage)), LuceneOptions.getDefaultOptions());
        ESAMetric esaMetric = new ESAMetric(testLanguage, searcher);
        String[] testPhrases = {"United States", "Barack Obama", "geometry", "machine learning"};
        for (int i = 0; i < testPhrases.length; i++) {
            for (int j = i; j < testPhrases.length; j++) {
                SRResult srResult = esaMetric.similarity(testPhrases[i], testPhrases[j], testLanguage, false);
                System.out.println("Similarity score between " + testPhrases[i] + " and " + testPhrases[j] + " is " + srResult.getValue());
            }
        }
    }
}
