package org.wikibrain.phrases;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.util.Version;
import org.junit.Test;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.IdentityStringNormalizer;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.StringNormalizer;
import org.wikibrain.lucene.LuceneStringNormalizer;
import org.wikibrain.lucene.TokenizerOptions;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


/**
 * @author Shilad Sen
 */
public class TestPhraseAnalyzerDao {

    @Test
    public void testPrunedPageCounts() {
        Map<Integer, Integer> rr = new HashMap<Integer, Integer>();
        rr.put(3424, 1);
        rr.put(31, 4);
        rr.put(999, 10);

        PrunedCounts<Integer> p1 = new SimplePruner<Integer>(0, 10000, 0.0).prune(rr);
        assertEquals(p1.size(), 3);
        assertEquals(p1.getTotal(), 15);
        assertEquals(new ArrayList<Integer>(p1.keySet()), Arrays.asList(999, 31, 3424));
        assertEquals(new ArrayList<Integer>(p1.values()), Arrays.asList(10, 4, 1));

        PrunedCounts<Integer> p2 = new SimplePruner<Integer>(0, 2, 0.0).prune(rr);
        assertEquals(p2.size(), 2);
        assertEquals(p2.getTotal(), 15);
        assertEquals(new ArrayList<Integer>(p2.keySet()), Arrays.asList(999, 31));
        assertEquals(new ArrayList<Integer>(p2.values()), Arrays.asList(10, 4));

        PrunedCounts<Integer> p3 = new SimplePruner<Integer>(3, 10000, 0.0).prune(rr);
        assertEquals(p3.size(), 2);
        assertEquals(p3.getTotal(), 15);
        assertEquals(new ArrayList<Integer>(p3.keySet()), Arrays.asList(999, 31));
        assertEquals(new ArrayList<Integer>(p3.values()), Arrays.asList(10, 4));

        PrunedCounts<Integer> p4 = new SimplePruner<Integer>(0, 10000, 0.25).prune(rr);
        assertEquals(p4.size(), 2);
        assertEquals(p4.getTotal(), 15);
        assertEquals(new ArrayList<Integer>(p4.keySet()), Arrays.asList(999, 31));
        assertEquals(new ArrayList<Integer>(p4.values()), Arrays.asList(10, 4));
    }

    @Test
    public void testPrunedPhraseCounts() {
        Map<String, Integer> counts = new HashMap<String, Integer>();
        counts.put("z", 3);
        counts.put("wAz", 4);
        counts.put("Z.", 8);
        counts.put("Y", 9);

        PrunedCounts<String> p1 = new NormalizedStringPruner(0, 10000, 0.0).prune(counts);
        assertEquals(p1.size(), 3);
        assertEquals(p1.getTotal(), 24);
        assertEquals(new ArrayList<String>(p1.keySet()), Arrays.asList("Z.", "Y", "wAz"));
        assertEquals(new ArrayList<Integer>(p1.values()), Arrays.asList(11, 9, 4));
    }

    @Test
    public void testDao() throws IOException, DaoException {
        File tmp = File.createTempFile("testdb", ".db", null);
        tmp.delete();

        StringNormalizer normalizer = new LuceneStringNormalizer(new TokenizerOptions(true, false, false), Version.LUCENE_43);
        PhraseAnalyzerDao dao = new PhraseAnalyzerObjectDbDao(normalizer, tmp, true);
        FileUtils.forceDeleteOnExit(tmp);
        Language en = Language.getByLangCode("en");

        PrunedCounts<Integer> c1 = new PrunedCounts<Integer>(12);
        c1.put(349, 7);
        c1.put(3121, 3);
        dao.savePhraseCounts(en, "FOo!", c1);

        PrunedCounts<String> c2 = new PrunedCounts<String>(13);
        c2.put("Bar", 9);
        c2.put("baz", 3);
        c2.put("boof", 1);

        dao.savePageCounts(en, 3214, c2);

        assertNull(dao.getPageCounts(en, 34321, 19));
        assertNull(dao.getPhraseCounts(en, "sadfas", 19));

        PrunedCounts<Integer> c3 = dao.getPhraseCounts(en, "fOO-", 5);
        assertNotNull(c3);
        assertEquals(c3.size(), 2);
        assertEquals(c3.getTotal(), 12);
        assertEquals(new ArrayList<Integer>(c3.keySet()), Arrays.asList(349, 3121));
        assertEquals(new ArrayList<Integer>(c3.values()), Arrays.asList(7, 3));

        PrunedCounts<Integer> c4 = dao.getPhraseCounts(en, "fOO-", 1);
        assertNotNull(c4);
        assertEquals(c4.size(), 1);
        assertEquals(c4.getTotal(), 12);
        assertEquals(new ArrayList<Integer>(c4.keySet()), Arrays.asList(349));
        assertEquals(new ArrayList<Integer>(c4.values()), Arrays.asList(7));


        PrunedCounts<String> c5 = dao.getPageCounts(en, 3214, 5);
        assertNotNull(c5);
        assertEquals(c5.size(), 3);
        assertEquals(c5.getTotal(), 13);
        assertEquals(new ArrayList<String>(c5.keySet()), Arrays.asList("Bar", "baz", "boof"));
        assertEquals(new ArrayList<Integer>(c5.values()), Arrays.asList(9, 3, 1));

        PrunedCounts<String> c6 = dao.getPageCounts(en, 3214, 2);
        assertNotNull(c6);
        assertEquals(c6.size(), 2);
        assertEquals(c6.getTotal(), 13);
        assertEquals(new ArrayList<String>(c6.keySet()), Arrays.asList("Bar", "baz"));
        assertEquals(new ArrayList<Integer>(c6.values()), Arrays.asList(9, 3));

        List<String> phrases = IteratorUtils.toList(dao.getAllPhrases(en));
        System.out.println("phrases are " + phrases);
        assertEquals(phrases, Arrays.asList("foo"));

        List<Pair<String, PrunedCounts<Integer>>> phraseCounts = IteratorUtils.toList(dao.getAllPhraseCounts(en));
        assertEquals(1, phraseCounts.size());
        assertEquals("foo", phraseCounts.get(0).getKey());
        assertEquals(2, phraseCounts.get(0).getValue().size());
        assertEquals((Integer)7, (Integer)phraseCounts.get(0).getValue().get(349));

        dao.close();

        dao = new PhraseAnalyzerObjectDbDao(normalizer, tmp, false);
        phrases = IteratorUtils.toList(dao.getAllPhrases(en));
        System.out.println("phrases are " + phrases);
        assertEquals(phrases, Arrays.asList("foo"));

        phraseCounts = IteratorUtils.toList(dao.getAllPhraseCounts(en));
        assertEquals(1, phraseCounts.size());
        assertEquals("foo", phraseCounts.get(0).getKey());
        assertEquals(2, phraseCounts.get(0).getValue().size());
        assertEquals((Integer)7, (Integer)phraseCounts.get(0).getValue().get(349));

        dao.close();

    }
}
