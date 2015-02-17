package org.wikibrain.integration;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.loader.LuceneLoader;
import org.wikibrain.lucene.LuceneSearcher;
import org.wikibrain.lucene.WikiBrainScoreDoc;

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

    @Ignore
   @Test
    public void testLoader() throws DaoException, WikiBrainException, ConfigurationException, IOException {
        LuceneLoader.main(TestUtils.getArgs("-d"));

        Env env = TestUtils.getEnv();
//        LocalPageDao lpdao = env.getConfigurator().get(LocalPageDao.class);
        LuceneSearcher searcher = env.getConfigurator().get(LuceneSearcher.class);

        MetaInfoDao metaDao = env.getConfigurator().get(MetaInfoDao.class);
        assertEquals(2, metaDao.getLoadedLanguages(LuceneSearcher.class).size());
        assertEquals(2, searcher.getLanguageSet().size());

        WikiBrainScoreDoc[] docs = searcher.getQueryBuilderByLanguage(SIMPLE)
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
