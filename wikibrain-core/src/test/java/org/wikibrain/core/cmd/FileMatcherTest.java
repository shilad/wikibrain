package org.wikibrain.core.cmd;

import org.junit.Test;
import org.wikibrain.core.lang.Language;

import static junit.framework.Assert.assertEquals;

/**
 * Created by shilad on 11/22/15.
 */
public class FileMatcherTest {
    @Test
    public void testLang() {
        String [] tests = {
                "https://dumps.wikimedia.org/enwiki/20151102/enwiki-20151102-pages-meta-history1.xml-p000000010p000002875.7z",
                "en",
                "https://dumps.wikimedia.org/enwiki/20151102/enwiki-20151102-protected_titles.sql.gz",
                "en",
                "https://dumps.wikimedia.org/cbk_zamwiki/20151102/cbk_zamwiki-20151102-pages-logging.xml.gz",
                "cbk_zam",

        };
        for (int i = 0; i < tests.length; i += 2) {
            assertEquals(Language.getByLangCode(tests[i+1]), FileMatcher.ARTICLES.getLanguage(tests[i]));
        }
    }
}
