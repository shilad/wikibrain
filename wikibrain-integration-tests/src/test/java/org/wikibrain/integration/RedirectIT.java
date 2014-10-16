package org.wikibrain.integration;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.*;

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

    @Ignore
    @Test
    public void testRedirect() throws DaoException {
        LocalPage page = dao.getByTitle(new Title("Obama", Language.getByLangCode("simple")), NameSpace.ARTICLE);
        assertEquals("Barack Obama", page.getTitle().getCanonicalTitle());
    }


    @Ignore
    @Test
    public void testMeta() throws DaoException {
        MetaInfo mi = metaDao.getInfo(Redirect.class);
        assertEquals(mi.getNumRecords(), dao.getCount(new DaoFilter().setRedirect(true)));
        assertEquals(mi.getNumErrors(), 0);
        assertEquals(metaDao.getLoadedLanguages(Redirect.class).size(), 2);
        assertEquals(dao.getLoadedLanguages().size(), 2);
    }
}
