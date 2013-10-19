package org.wikapidia.integration;

import org.junit.BeforeClass;
import org.junit.Test;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.MetaInfoDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.dao.load.LuceneLoader;
import org.wikapidia.lucene.LuceneSearcher;
import org.wikapidia.lucene.WikapidiaScoreDoc;

import java.io.IOException;
import java.sql.SQLException;

import static org.junit.Assert.*;

/**
 * @author Shilad Sen
 */
public class LuceneLoaderIT {
    private static final Language SIMPLE = Language.getByLangCode("simple");

    @BeforeClass
    public static void prepareDump() throws ConfigurationException, IOException, SQLException {
        TestDB db = TestUtils.getTestDb();
        db.restoreWikiText();
    }

    @Test
    public void testLoader() throws DaoException, WikapidiaException, ConfigurationException, IOException {
        LuceneLoader.main(TestUtils.getArgs("-d"));

        Env env = TestUtils.getEnv();
//        LocalPageDao lpdao = env.getConfigurator().get(LocalPageDao.class);
        LuceneSearcher searcher = env.getConfigurator().get(LuceneSearcher.class);

        MetaInfoDao metaDao = env.getConfigurator().get(MetaInfoDao.class);
        assertEquals(2, metaDao.getLoadedLanguages(LuceneSearcher.class).size());
        assertEquals(2, searcher.getLanguageSet().size());

        WikapidiaScoreDoc[] docs = searcher.getQueryBuilderByLanguage(SIMPLE)
                        .setPhraseQuery("Barack Obama")
                        .search();
        int baracksRank = Integer.MAX_VALUE;
        for (int i = 0; i < docs.length; i++) {
            if (docs[i].wpId == 223430) {
                baracksRank = i;
            }
        }
        assertTrue(baracksRank < 20);
        docs = searcher.getQueryBuilderByLanguage(SIMPLE)
                .setPhraseQuery("Kenya harvard president 44th white house election")
                .search();
        baracksRank = Integer.MAX_VALUE;
        for (int i = 0; i < docs.length; i++) {
            if (docs[i].wpId == 223430) {
                baracksRank = i;
            }
        }
        assertEquals(0, baracksRank);
    }
}
