import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.junit.Ignore;
import org.junit.Test;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.RawPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.lucene.LuceneOptions;
import org.wikibrain.lucene.LuceneSearcher;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

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
    @Ignore
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
            if (i > -1) {
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

    @Ignore
    @Test
    public void testGetDoc() throws ConfigurationException, IOException, DaoException {
        Configurator conf = new Configurator(new Configuration());
        RawPageDao rawPageDao = conf.get(RawPageDao.class);
        LuceneOptions[] luceneOptions = new LuceneOptions[] {conf.get(LuceneOptions.class)};
        Collection<NameSpace> namespaces = luceneOptions[0].namespaces;
        Language lang = Language.getByLangCode("simple");
        LuceneSearcher searcher = new LuceneSearcher(new LanguageSet(Collections.singletonList(lang)), LuceneOptions.getDefaultOptions());

        int localId = 39; // this is the last valid ID
        int luceneId = searcher.getDocIdFromLocalId(localId, lang);
        System.out.println(luceneId);
    }
}
