import org.junit.Ignore;
import org.junit.Test;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.RawPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.lucene.LuceneOptions;
import org.wikapidia.lucene.LuceneSearcher;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;

/**
 *
 */
public class TestLuceneSearcher {

    @Test
    @Ignore
    public void testGetDocIdFromLocalId() throws IOException, DaoException {
        int[] testIds = {368995, 144220, 363993, 134311, 315324, 219587, 86956, 69758, 33638, 294524};
        Language lang = Language.getByLangCode("simple");
        LuceneSearcher searcher = new LuceneSearcher(new LanguageSet(Arrays.asList(lang)), LuceneOptions.getDefaultOptions());
        for (int testId : testIds) {
            int docID = searcher.getDocIdFromLocalId(testId, lang);
            int returnedId = searcher.getLocalIdFromDocId(docID, lang);
            assert(testId == returnedId);
        }
    }

    @Test
    public void testResolveLocalId() throws ConfigurationException, DaoException {
        Configurator conf = new Configurator(new Configuration());
        RawPageDao rawPageDao = conf.get(RawPageDao.class);
        LuceneOptions[] luceneOptions = new LuceneOptions[] {conf.get(LuceneOptions.class)};
        Collection<NameSpace> namespaces = luceneOptions[0].namespaces;
        Language lang = Language.getByLangCode("simple");
        LuceneSearcher searcher = new LuceneSearcher(new LanguageSet(Arrays.asList(lang)), LuceneOptions.getDefaultOptions());
        int i = 0;
        Iterable<RawPage> rawPages = rawPageDao.get(new DaoFilter()
                .setLanguages(lang)
                .setNameSpaces(namespaces)
                .setRedirect(false));
        for (RawPage rawPage : rawPages) {
            if (i > -1
                    ) {
                int testId = rawPage.getLocalId();
                int docID = searcher.getDocIdFromLocalId(testId, lang);
                int returnedId = searcher.getLocalIdFromDocId(docID, lang);
                if (testId != returnedId) {
                    System.out.println("test = " + testId + "; lucene = " + docID + "; returned: " + returnedId);
//                assert(testId == returnedId);
                }
                i++;
            } else {
                break;
            }
        }
    }
}
