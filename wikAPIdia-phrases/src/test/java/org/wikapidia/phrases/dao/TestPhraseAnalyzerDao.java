package org.wikapidia.phrases.dao;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.utils.ObjectDb;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 *
 */
public class TestPhraseAnalyzerDao {

    @Test
    public void testResolutionRecord() {
        ResolutionRecord rr = new ResolutionRecord("foo");
        rr.add(3424, 1);
        rr.add(31, 4);
        rr.add(999, 10);
        rr.add(31, 11);
        rr.freeze();

        assertEquals(rr.size(), 3);
        assertEquals(rr.getWpId(0), 31);
        assertEquals(rr.getWpId(1), 999);
        assertEquals(rr.getWpId(2), 3424);
        assertEquals(rr.getCount(0), 15);
        assertEquals(rr.getCount(1), 10);
        assertEquals(rr.getCount(2), 1);

        rr.prune(2);
        assertEquals(rr.size(), 2);
        assertEquals(rr.getWpId(0), 31);
        assertEquals(rr.getWpId(1), 999);
        assertEquals(rr.getCount(0), 15);
        assertEquals(rr.getCount(1), 10);
    }

    @Test
    public void testDescriptionRecord() {
        DescriptionRecord rr = new DescriptionRecord(3242);
        rr.add("z", 1);
        rr.add("wAz", 4);
        rr.add("Z.", 10);
        rr.add("Y", 9);
        rr.freeze();

        assertEquals(rr.size(), 3);
        assertEquals(rr.getPhrase(0), "z");
        assertEquals(rr.getPhrase(1), "y");
        assertEquals(rr.getPhrase(2), "waz");
        assertEquals(rr.getCount(0), 11);
        assertEquals(rr.getCount(1), 9);
        assertEquals(rr.getCount(2), 4);

        rr.prune(2);
        assertEquals(rr.size(), 2);
        assertEquals(rr.getPhrase(0), "z");
        assertEquals(rr.getPhrase(1), "y");
        assertEquals(rr.getCount(0), 11);
        assertEquals(rr.getCount(1), 9);
    }

    @Test
    public void testDao() throws IOException, DaoException {
        File tmp = File.createTempFile("testdb", ".db", null);
        tmp.delete();
        FileUtils.forceDeleteOnExit(tmp);

        PhraseAnalyzerDao dao = new PhraseAnalyzerObjectDbDao(tmp, true);
        Language en = Language.getByLangCode("en");
        dao.add(en, 319, "foo", 93);
        dao.add(en, 319, "foO", 5);
        dao.add(en, 319, "baz", 1);
        dao.add(en, 319, "bar", 12);
        dao.add(en, 132, "fOo", 19);
        dao.add(en, 36, "fOo", 8);
        dao.freezeAndPrune(2, 2, 0.0);

        PhraseAnalyzerDao.PrunedCounts<String> r1 = dao.getPageCounts(en, 319);
        assertNotNull(r1);
        assertEquals(r1.size(), 2);
        assertEquals(r1.getTotal(), 111);
        assertEquals((int)r1.get("foo"), 98);
        assertEquals((int)r1.get("bar"), 12);
        assertNull(r1.get("baz"));

        assertNull(dao.getPhraseCounts(en, "baz"));
        assertNull(dao.getPhraseCounts(en, "BAz!"));


        PhraseAnalyzerDao.PrunedCounts<Integer> r2 = dao.getPhraseCounts(en, "foo");
        assertNotNull(r2);
        assertEquals(r2.size(), 2);
        assertEquals(r2.getTotal(), 125);
        assertEquals((int)r2.get(319), 98);
        assertEquals((int)r2.get(132), 19);
    }
}
