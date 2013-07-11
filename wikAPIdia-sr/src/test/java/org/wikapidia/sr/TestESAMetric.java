package org.wikapidia.sr;

import org.junit.Test;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;

/**
 *
 */
public class TestESAMetric {

    @Test
    public void testMostSimilarPages() throws WikapidiaException, DaoException {
        Language testLanguage = Language.getByLangCode("en");
        ESAMetric esaMetric = new ESAMetric(testLanguage);
        LocalPage page = new LocalPage(testLanguage, 3, new Title("United States", testLanguage), NameSpace.ARTICLE);
        SRResultList srResults= esaMetric.mostSimilar(page, 20, false);
        System.out.println(srResults);
    }

    @Test
    public void testPhraseSimilarity() throws DaoException {
        Language testLanguage = Language.getByLangCode("en");
        ESAMetric esaMetric = new ESAMetric(testLanguage);
        String[] testPhrases = {"United States", "Barack Obama", "geometry", "machine learning"};
        for (int i = 0; i < testPhrases.length; i++) {
            for (int j = i; j < testPhrases.length; j++) {
                SRResult srResult = esaMetric.similarity(testPhrases[i], testPhrases[j], testLanguage, false);
                System.out.println("Similarity score between " + testPhrases[i] + " and " + testPhrases[j] + " is " + srResult.getValue());
            }
        }
    }
}
