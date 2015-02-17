import org.apache.lucene.util.Version;
import org.junit.Test;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.StringNormalizer;
import org.wikibrain.lucene.LuceneStringNormalizer;
import org.wikibrain.lucene.TokenizerOptions;

import static org.junit.Assert.*;

/**
 * @author Shilad Sen
 */
public class TestLuceneNormalizer {
    private static Language EN = Language.getByLangCode("en");
    private static Language SIMPLE = Language.getByLangCode("simple");

    @Test
    public void testSimple() {
        StringNormalizer n = new LuceneStringNormalizer(
                new TokenizerOptions(false, false, false),
                Version.LUCENE_43);
        long before = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            assertEquals("Hello world", n.normalize(EN, "Hello, world!"));
        }
        long after = System.currentTimeMillis();
        System.err.println("average normalize time is " + 1.0 * (after-before) / 100000 + " millis");

        assertEquals("hello World", n.normalize(SIMPLE, "hello-World"));
    }

    @Test
    public void testCaseinsensitive() {
        StringNormalizer n = new LuceneStringNormalizer(
                new TokenizerOptions(true, true, false),
                Version.LUCENE_43);
        assertEquals("hello world", n.normalize(EN, "Hello, world!"));
        assertEquals("hello world", n.normalize(SIMPLE, "hello-World"));
        assertEquals("hello worldy", n.normalize(EN, "hello-Worldy"));
    }


    @Test
    public void testCaseinsensitivePorter() {
        StringNormalizer n = new LuceneStringNormalizer(
                new TokenizerOptions(true, true, true),
                Version.LUCENE_43);
        assertEquals("hello world", n.normalize(EN, "Hello, world!"));
        assertEquals("hello worldi", n.normalize(SIMPLE, "hello-Worldy"));
    }
}
