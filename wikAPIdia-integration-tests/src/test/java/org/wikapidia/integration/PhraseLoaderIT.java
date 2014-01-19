package org.wikapidia.integration;

import org.junit.BeforeClass;
import org.junit.Test;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.MetaInfoDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.dao.load.LuceneLoader;
import org.wikapidia.dao.load.PhraseLoader;
import org.wikapidia.lucene.LuceneSearcher;
import org.wikapidia.lucene.WikapidiaScoreDoc;
import org.wikapidia.phrases.PhraseAnalyzer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Shilad Sen
 */
public class PhraseLoaderIT {
    private static final Language SIMPLE = Language.getByLangCode("simple");

    private static final int APPLE_FRUIT_ID = 39;
    private static final int APPLE_MAC_ID = 517;
    private static final int APPLE_JUICE_ID = 19351;
    private static final int APPLE_INC_ID = 7111;

    @BeforeClass
    public static void prepareDump() throws ConfigurationException, IOException, SQLException {
        TestDB db = TestUtils.getTestDb();
        db.restoreLucene();
    }

    @Test
    public void testLoader() throws ClassNotFoundException, SQLException, WikapidiaException, DaoException, ConfigurationException, IOException, InterruptedException {
        PhraseLoader.main(TestUtils.getArgs("-p", "lucene")); // lucene requires no loading!
        Env env = TestUtils.getEnv();

        // Get the configurator that creates components and a phraze analyzer from it
        Configurator configurator = env.getConfigurator();
        PhraseAnalyzer pa = configurator.get(PhraseAnalyzer.class, "lucene");

        // get the most common phrases in simple
        LinkedHashMap<LocalPage, Float> resolution = pa.resolveLocal(SIMPLE, "apple", 20);

        // get page ranks
        int fruitRank = Integer.MAX_VALUE;
        int macRank = Integer.MAX_VALUE;
        int juiceRank = Integer.MAX_VALUE;
        int incRank = Integer.MAX_VALUE;

        int rank = 0;
        for (LocalPage p : resolution.keySet()) {
            switch (p.getLocalId()) {
                case APPLE_FRUIT_ID:
                   fruitRank = rank;
                    break;
                case APPLE_MAC_ID:
                    macRank = rank;
                    break;
                case APPLE_JUICE_ID:
                    juiceRank = rank;
                    break;
                case APPLE_INC_ID:
                    incRank = rank;
                    break;
            }
            rank++;
        }
        assertTrue(fruitRank <= 10);
        assertTrue(macRank <= 10);
        assertTrue(juiceRank <= 10);
        assertTrue(incRank <= 10);
    }
}
