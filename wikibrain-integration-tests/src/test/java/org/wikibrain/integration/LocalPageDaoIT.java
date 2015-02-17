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

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Shilad Sen
 */
public class LocalPageDaoIT {
    private static LocalPageDao dao;
    private static MetaInfoDao metaDao;

    @BeforeClass
    public static void prepareDb() throws ConfigurationException, ClassNotFoundException, SQLException, DaoException, IOException {
        TestDB testDb = TestUtils.getTestDb();
        testDb.restoreRawAndLocal();
        dao = testDb.getEnv().getConfigurator().get(LocalPageDao.class);
        metaDao = testDb.getEnv().getConfigurator().get(MetaInfoDao.class);
    }

    @Ignore
    @Test
    public void testIteration() throws DaoException {

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
        for (Object page : dao.get(filter)) {
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
    @Ignore
    @Test
    public void testSearch() throws DaoException {
        LocalPage lp = dao.getByTitle(new Title("Barack Obama", Language.getByLangCode("simple")), NameSpace.ARTICLE);
        assertNotNull(lp);
    }
    @Ignore
    @Test
    public void testMeta() throws DaoException {
        MetaInfo mi = metaDao.getInfo(LocalPage.class);
        assertEquals(mi.getNumRecords(), dao.getCount(new DaoFilter()));
        assertEquals(mi.getNumErrors(), 0);
        assertEquals(metaDao.getLoadedLanguages(LocalPage.class).size(), 2);
        assertEquals(dao.getLoadedLanguages().size(), 2);
    }
}
