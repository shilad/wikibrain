package org.wikapidia.integration;

import org.junit.BeforeClass;
import org.junit.Test;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;

import java.io.IOException;
import java.sql.SQLException;

import static org.junit.Assert.*;

/**
 * @author Shilad Sen
 */
public class RedirectIT {
    private static LocalPageDao dao;

    @BeforeClass
    public static void prepareDb() throws ConfigurationException, ClassNotFoundException, SQLException, DaoException, IOException {
        TestDB testDb = TestUtils.getTestDb();
        testDb.restoreRedirect();
        dao = testDb.getEnv().getConfigurator().get(LocalPageDao.class);
    }

    @Test
    public void testRedirect() throws DaoException {
        LocalPage page = dao.getByTitle(Language.getByLangCode("simple"), new Title("Obama", Language.getByLangCode("simple")), NameSpace.ARTICLE);
        assertEquals("Barack Obama", page.getTitle().getCanonicalTitle());
    }
}
