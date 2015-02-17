package org.wikibrain.core.dao;

import org.junit.Before;
import org.junit.Test;
import org.wikibrain.core.dao.sql.MetaInfoSqlDao;
import org.wikibrain.core.dao.sql.TestDaoUtil;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Shilad Sen
 */
public class TestMetaInfoDao {
    private MetaInfoSqlDao dao;
    private Language EN = Language.getByLangCode("en");
    private Language IT = Language.getByLangCode("it");
    private WpDataSource ds;

    @Before
    public void setupDao() throws ClassNotFoundException, IOException, DaoException {
        ds = TestDaoUtil.getWpDataSource();
        dao = new MetaInfoSqlDao(ds);
        assertFalse(dao.tableExists());
        dao.beginLoad();
        assertTrue(dao.tableExists());
    }

    @Test
    public void testSimple() throws DaoException {
        long l1 = System.currentTimeMillis();
        for (int i = 1; i <= 1000000; i++) {
            assertEquals(i, dao.incrementRecords(String.class, EN));
            if (i % 10 == 0) {
                dao.incrementErrors(String.class, EN);
            }
        }
        long l2 = System.currentTimeMillis();
        System.err.println("elapsed is " + (l2 - l1));
        assertEquals(1000000, dao.getInfo(String.class, EN).getNumRecords());
        assertEquals(100000, dao.getInfo(String.class, EN).getNumErrors());
        dao.sync(String.class, EN);
        dao = new MetaInfoSqlDao(ds);
        assertEquals(1000000, dao.getInfo(String.class, EN).getNumRecords());
        assertEquals(100000, dao.getInfo(String.class, EN).getNumErrors());

        assertEquals(0, dao.getInfo(Boolean.class, EN).getNumRecords());
        assertEquals(0, dao.getInfo(Boolean.class, EN).getNumErrors());
        assertEquals(0, dao.getInfo(String.class, IT).getNumRecords());
        assertEquals(0, dao.getInfo(String.class, IT).getNumErrors());

    }



    @Test
    public void testNull() throws DaoException {
        dao.incrementRecords(String.class, null);
        dao.incrementRecords(Boolean.class, null);
        dao.incrementRecords(String.class, null);
        assertEquals(2, dao.getInfo(String.class, null).getNumRecords());
        assertEquals(1, dao.getInfo(Boolean.class, null).getNumRecords());

        dao.sync(String.class);
        dao = new MetaInfoSqlDao(ds);
        assertEquals(2, dao.getInfo(String.class, null).getNumRecords());
        assertEquals(0, dao.getInfo(String.class, EN).getNumRecords());
        assertEquals(0, dao.getInfo(String.class, EN).getNumErrors());
    }

    @Test
    public void testNoLang() throws DaoException {
        dao.incrementRecords(String.class, null);
        dao.incrementRecords(String.class);
        dao.incrementRecords(String.class, EN);
        dao.incrementRecords(String.class, IT);
        dao.incrementRecords(Boolean.class, EN);
        dao.incrementRecords(String.class, null);

        assertEquals(5, dao.getInfo(String.class).getNumRecords());
        assertEquals(1, dao.getInfo(Boolean.class).getNumRecords());
        assertEquals(0, dao.getInfo(String.class).getNumErrors());

        dao.sync(String.class);
        dao = new MetaInfoSqlDao(ds);
        assertEquals(5, dao.getInfo(String.class).getNumRecords());
        assertEquals(1, dao.getInfo(Boolean.class).getNumRecords());
        assertEquals(0, dao.getInfo(String.class).getNumErrors());


        LanguageSet langs = dao.getLoadedLanguages(String.class);
        assertEquals(langs.size(), 2);
        assertTrue(langs.containsLanguage(EN));
        assertTrue(langs.containsLanguage(IT));

        langs = dao.getLoadedLanguages(Boolean.class);
        assertEquals(langs.size(), 1);
        assertTrue(langs.containsLanguage(EN));

    }
}
