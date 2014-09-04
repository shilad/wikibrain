package org.wikibrain.integration;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.Title;
import org.wikibrain.loader.RedirectLoader;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

/**
 * @author Shilad Sen
 */
public class RedirectLoaderIT {
    private static LocalPageDao dao;

    @BeforeClass
    public static void prepareDb() throws ConfigurationException, SQLException {
        TestDB testDB = TestUtils.getTestDb();
        testDB.restoreRawAndLocal();
        dao = testDB.getEnv().getConfigurator().get(LocalPageDao.class);
    }

    @Ignore
    @Test
    public void testRedirects() throws DaoException, ConfigurationException {
        RedirectLoader.main(TestUtils.getArgs("-d"));
        LocalPage page = dao.getByTitle(new Title("Obama", Language.getByLangCode("simple")), NameSpace.ARTICLE);
        assertEquals("Barack Obama", page.getTitle().getCanonicalTitle());
    }
}
