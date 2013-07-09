package org.wikapidia.sr;

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

    public void testMostSimilarPages() throws WikapidiaException, DaoException {
        Language testLanguage = Language.getByLangCode("en");
        ESAMetric esaMetric = new ESAMetric(testLanguage);
        LocalPage page = new LocalPage(testLanguage, 3, new Title("United States", testLanguage), NameSpace.ARTICLE);
        SRResultList srResults= esaMetric.mostSimilar(page, 20, false);
        System.out.println(srResults);
    }
}
