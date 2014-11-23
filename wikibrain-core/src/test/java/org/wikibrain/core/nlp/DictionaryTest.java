package org.wikibrain.core.nlp;

import org.junit.Test;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.Title;
import org.wikibrain.core.nlp.Dictionary;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Shilad Sen
 */
public class DictionaryTest {
    @Test
    public void testSimple() throws DaoException {
        Dictionary dict = new Dictionary(Language.EN, Dictionary.WordStorage.IN_MEMORY);
        dict.setContainsMentions(true);
        dict.setCountBigrams(true);
        dict.countRawText(TEST_CORPUS);
        assertEquals(429, dict.getTotalCount());
        assertEquals(23, dict.getUnigramCount("the"));
        assertEquals(6, dict.getUnigramCount("I"));
        assertEquals(1, dict.getUnigramCount("veil"));
        assertEquals(3, dict.getBigramCount("in a"));
        assertEquals(3, dict.getUnigramCount("but"));
        assertEquals(252, dict.getNumUnigrams());

        // Test mentions
        assertEquals(2, dict.getNumMentionedArticles());
        assertEquals(1, dict.getMentionCount(3));
        assertEquals(2, dict.getMentionCount(4));

        // Test top unigrams
        List<String> top = Arrays.asList("the", "of", "and", "his", "a", "to", "in", "I", "was", "for", "own", "which");
        assertEquals(top, dict.getFrequentUnigrams(12));

        // Test top unigrams and mentions together
        // Mock a local page dao
        LocalPageDao lpd = mock(LocalPageDao.class);
        LocalPage page3 = new LocalPage(Language.EN, 3, "This_is_page_3");
        LocalPage page4 = new LocalPage(Language.EN, 4, "This_is_page_4");
        when(lpd.getById(Language.EN, 3)).thenReturn(page3);
        when(lpd.getById(Language.EN, 4)).thenReturn(page4);
        top = Arrays.asList("the", "of", "and", "his", "a", "/w/en/4/This_is_page_4", "/w/en/3/This_is_page_3");
        assertEquals(top, dict.getFrequentUnigramsAndMentions(lpd, 5, 3, 1));
    }

    @Test
    public void testReadWrite() throws IOException {
        Dictionary dict = new Dictionary(Language.EN, Dictionary.WordStorage.IN_MEMORY);
        dict.setContainsMentions(true);
        dict.setCountBigrams(true);
        dict.countRawText(TEST_CORPUS);
        File tmp = File.createTempFile("dict", "txt");
        tmp.deleteOnExit();
        tmp.delete();
        dict.write(tmp);

        // Try reading it from disk
        Dictionary dict2 = new Dictionary(Language.EN, Dictionary.WordStorage.NONE);
        dict2.read(tmp);

        assertEquals(dict.getTotalCount(), dict2.getTotalCount());
        assertEquals(dict.getNumUnigrams(), dict2.getNumUnigrams());
        for (String word : dict.getFrequentUnigrams(Integer.MAX_VALUE)) {
            assertEquals(dict.getUnigramCount(word), dict2.getUnigramCount(word));
        }

        // Try streaming the words to disk
        Dictionary dict3 = new Dictionary(Language.EN, Dictionary.WordStorage.ON_DISK);
        dict3.setContainsMentions(true);
        dict3.setCountBigrams(true);
        dict3.countRawText(TEST_CORPUS);
        dict.write(tmp);

        assertEquals(dict.getTotalCount(), dict3.getTotalCount());
        assertEquals(dict.getNumUnigrams(), dict3.getNumUnigrams());
        for (String word : dict.getFrequentUnigrams(Integer.MAX_VALUE)) {
            assertEquals(dict.getUnigramCount(word), dict3.getUnigramCount(word));
        }

        // Try pruning the read-in result
        Dictionary dict4 = new Dictionary(Language.EN);
        dict4.read(tmp, Integer.MAX_VALUE, 3);
        assertEquals(dict4.getNumUnigrams(), 26);


        dict4 = new Dictionary(Language.EN);
        dict4.read(tmp, 5, 2);
        assertEquals(dict4.getNumUnigrams(), 5);
        assertEquals(dict.getTotalCount(), dict4.getTotalCount());
        for (String word : dict.getFrequentUnigrams(5)) {
            assertEquals(dict.getUnigramCount(word), dict4.getUnigramCount(word));
        }
    }

    @Test
    public void testPrune() throws IOException {
        Dictionary dict = new Dictionary(Language.EN, Dictionary.WordStorage.IN_MEMORY);
        dict.setMaxDictionarySize(10);
        dict.setContainsMentions(true);
        dict.setCountBigrams(true);
        dict.countRawText(TEST_CORPUS);
        assertEquals(429, dict.getTotalCount());
        assertEquals(252, dict.getNumUnigrams());
        assertEquals(23, dict.getUnigramCount("the"));
        assertEquals(6, dict.getUnigramCount("I"));
        assertEquals(1, dict.getUnigramCount("veil"));
        assertEquals(3, dict.getBigramCount("in a"));
        assertEquals(3, dict.getUnigramCount("but"));
        dict.pruneIfNecessary();
        assertEquals(429, dict.getTotalCount());
        assertEquals(7, dict.getNumUnigrams());
        assertEquals(23, dict.getUnigramCount("the"));
        assertEquals(8, dict.getUnigramCount("in"));
        assertEquals(0, dict.getUnigramCount("I"));
        assertEquals(0, dict.getUnigramCount("veil"));
        assertEquals(0, dict.getBigramCount("in a"));
        assertEquals(0, dict.getUnigramCount("but"));
    }
    /**
     * From http://www.gutenberg.org/cache/epub/1661/pg1661.txt
     */
    static String TEST_CORPUS =
    "To Sherlock Holmes she is always THE woman. I have seldom heard\n" +
    "him mention her under any other name. In his eyes she eclipses\n" +
    "and predominates the whole of her sex. It was not that he felt\n" +
    "any emotion akin to love for Irene Adler. All emotions, and that\n" +
    "one particularly, were abhorrent to his cold, precise but\n" +
    "admirably balanced mind. He was, I take it, the most perfect\n" +
    "reasoning and observing machine that the world has seen, but as a\n" +
    "lover he would have placed himself in a false position. He never\n" +
    "spoke of the softer passions, save with:/w/en/4/foo a gibe and a sneer. They\n" +
    "were admirable things for the observer--excellent for drawing the\n" +
    "veil from men's motives and actions. But for the trained reasoner\n" +
    "to admit such intrusions into his own delicate and finely\n" +
    "adjusted temperament was to introduce a distracting factor which\n" +
    "might throw a doubt upon all his mental results. Grit in a\n" +
    "sensitive instrument, or a crack in one of his own high-power\n" +
    "lenses, would not be more disturbing than a strong emotion in a\n" +
    "nature such as his. And yet there was but:/w/en/3/foo one woman to him, and\n" +
    "that woman was the late Irene Adler, of dubious and questionable\n" +
    "memory.\n" +
    "\n" +
    "I had seen little of Holmes lately. My marriage had drifted us\n" +
    "away from each other. My own complete happiness, and the\n" +
    "home-centred interests which rise up around the man who first\n" +
    "finds himself master of his own establishment, were sufficient to\n" +
    "absorb all my attention, while Holmes, who loathed every form of\n" +
    "society with his whole Bohemian soul, remained:/w/en/4/foo in our lodgings in\n" +
    "Baker Street, buried among his old books, and alternating from\n" +
    "week to week between cocaine and ambition, the drowsiness of the\n" +
    "drug, and the fierce energy of his own keen nature. He was still,\n" +
    "as ever, deeply attracted by the study of crime, and occupied his\n" +
    "immense faculties and extraordinary powers of observation in\n" +
    "following out those clues, and clearing up those mysteries which\n" +
    "had been abandoned as hopeless by the official police. From time\n" +
    "to time I heard some vague account of his doings: of his summons\n" +
    "to Odessa in the case of the Trepoff murder, of his clearing up\n" +
    "of the singular tragedy of the Atkinson brothers at Trincomalee,\n" +
    "and finally of the mission which he had accomplished so\n" +
    "delicately and successfully for the reigning family of Holland.\n" +
    "Beyond these signs of his activity, however, which I merely\n" +
    "shared with all the readers of the daily press, I knew little of\n" +
    "my former friend and companion.";
}
