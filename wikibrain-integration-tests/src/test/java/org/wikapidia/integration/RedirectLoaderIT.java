package org.wikapidia.integration;

import org.junit.BeforeClass;
import org.junit.Test;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;
import org.wikapidia.dao.load.RedirectLoader;

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

    @Test
    public void testRedirects() throws DaoException, ConfigurationException {
        RedirectLoader.main(TestUtils.getArgs("-d"));
        LocalPage page = dao.getByTitle(new Title("Obama", Language.getByLangCode("simple")), NameSpace.ARTICLE);
        assertEquals("Barack Obama", page.getTitle().getCanonicalTitle());
    }
}
