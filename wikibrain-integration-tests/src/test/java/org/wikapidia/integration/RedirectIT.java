package org.wikapidia.integration;

import org.junit.BeforeClass;
import org.junit.Test;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.MetaInfoDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.*;

import java.io.IOException;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

/**
 * @author Shilad Sen
 */
public class RedirectIT {
    private static LocalPageDao dao;
    private static MetaInfoDao metaDao;

    @BeforeClass
    public static void prepareDb() throws ConfigurationException, ClassNotFoundException, SQLException, DaoException, IOException {
        TestDB testDb = TestUtils.getTestDb();
        testDb.restoreRedirect();
        dao = testDb.getEnv().getConfigurator().get(LocalPageDao.class);
        metaDao = testDb.getEnv().getConfigurator().get(MetaInfoDao.class);
    }

    @Test
    public void testRedirect() throws DaoException {
        LocalPage page = dao.getByTitle(new Title("Obama", Language.getByLangCode("simple")), NameSpace.ARTICLE);
        assertEquals("Barack Obama", page.getTitle().getCanonicalTitle());
    }


    @Test
    public void testMeta() throws DaoException {
        MetaInfo mi = metaDao.getInfo(Redirect.class);
        assertEquals(mi.getNumRecords(), dao.getCount(new DaoFilter().setRedirect(true)));
        assertEquals(mi.getNumErrors(), 0);
        assertEquals(metaDao.getLoadedLanguages(Redirect.class).size(), 2);
        assertEquals(dao.getLoadedLanguages().size(), 2);
    }
}
