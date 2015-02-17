package org.wikibrain.integration;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.RawPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.loader.DumpLoader;

import java.io.IOException;
import java.sql.SQLException;

import static org.junit.Assert.*;

/**
 * @author Shilad Sen
 */
public class DumpIT {
    @BeforeClass
    public static void prepareDump() throws ConfigurationException, IOException {
        TestDB db = TestUtils.getTestDb();
        db.restoreDownloads();
    }
    @Ignore
    @Test
    public void testDump() throws ClassNotFoundException, SQLException, DaoException, ConfigurationException, IOException {
        DumpLoader.main(TestUtils.getArgs("-d"));
        Env env = TestUtils.getEnv();
        RawPageDao dao = env.getConfigurator().get(RawPageDao.class);
        DaoFilter filter = new DaoFilter().setNameSpaces(NameSpace.ARTICLE);
        filter.setRedirect(false);
        filter.setDisambig(false);

        int nLatin = dao.getCount(filter.setLanguages(Language.getByLangCode("la")));
        System.out.println("nLatin is " + nLatin);
        assertTrue(nLatin > 100000);
        assertTrue(nLatin < 140000);

        int nSimple = dao.getCount(filter.setLanguages(Language.getByLangCode("simple")));
        System.out.println("nSimple is " + nSimple);
        int i = 0;
        for (RawPage page : dao.get(filter)) {
            i++;
        }
        assertEquals(i, nSimple);
        assertTrue(nSimple > 70000);
        assertTrue(nSimple < 160000);


        filter.setRedirect(true);
        System.out.println("num redirects is " + dao.getCount(filter));
        assertTrue(dao.getCount(filter) > 20000);

        filter.setRedirect(false);
        filter.setDisambig(true);
        System.out.println("num disambigs is " + dao.getCount(filter));
        assertTrue(dao.getCount(filter) == 0);  // TODO: FIXME
    }
}
