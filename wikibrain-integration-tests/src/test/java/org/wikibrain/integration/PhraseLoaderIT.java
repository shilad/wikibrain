package org.wikibrain.integration;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.loader.PhraseLoader;
import org.wikibrain.phrases.PhraseAnalyzer;

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

    @Ignore
    @Test
    public void testLoader() throws ClassNotFoundException, SQLException, WikiBrainException, DaoException, ConfigurationException, IOException, InterruptedException {
        PhraseLoader.main(TestUtils.getArgs("-p", "lucene")); // lucene requires no loading!
        Env env = TestUtils.getEnv();

        // Get the configurator that creates components and a phraze analyzer from it
        Configurator configurator = env.getConfigurator();
        PhraseAnalyzer pa = configurator.get(PhraseAnalyzer.class, "lucene");

        // get the most common phrases in simple
        LinkedHashMap<LocalId, Float> resolution = pa.resolve(SIMPLE, "apple", 20);

        // get page ranks
        int fruitRank = Integer.MAX_VALUE;
        int macRank = Integer.MAX_VALUE;
        int juiceRank = Integer.MAX_VALUE;
        int incRank = Integer.MAX_VALUE;

        int rank = 0;
        for (LocalId p : resolution.keySet()) {
            switch (p.getId()) {
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
