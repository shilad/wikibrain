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
public class LocalPageDaoIT {
    private static LocalPageDao dao;

    @BeforeClass
    public static void prepareDb() throws ConfigurationException, ClassNotFoundException, SQLException, DaoException, IOException {
        TestDB testDb = TestUtils.getTestDb();
        testDb.restoreRawAndLocal();
        dao = testDb.getEnv().getConfigurator().get(LocalPageDao.class);
    }

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

    @Test
    public void testSearch() throws DaoException {
        LocalPage lp = dao.getByTitle(Language.getByLangCode("simple"), new Title("Barack Obama", Language.getByLangCode("simple")), NameSpace.ARTICLE);
        assertNotNull(lp);
    }
}
