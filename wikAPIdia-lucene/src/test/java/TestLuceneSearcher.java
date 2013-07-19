import org.junit.Test;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.lucene.LuceneOptions;
import org.wikapidia.lucene.LuceneSearcher;

import java.io.IOException;
import java.util.Arrays;

/**
 *
 */
public class TestLuceneSearcher {

    @Test
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
}
