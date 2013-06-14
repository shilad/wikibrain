package org.wikapidia.download;

import org.junit.Test;
import org.wikapidia.core.lang.Language;
import org.wikapidia.download.DumpLinkGetter;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 */
public class TestDumpLinkGetter {
    @Test
    public void testArticles() {

        List<String> testLinks = Arrays.asList(
                "http://dumps.wikimedia.org/enwiki/latest/enwiki-latest-pages-articles1.xml-p000000010p000010000.bz2",
                "http://dumps.wikimedia.org/enwiki/latest/enwiki-latest-abstract11.xml"
        );


        DumpLinkGetter testGetter = new DumpLinkGetter(Arrays.asList(DumpLinkGetter.PATTERN_ARTICLES));

        Pattern testPattern = testGetter.getLanguageSpecificPatterns(Language.getByLangCode("en")).get(0);

        for(String testLink : testLinks){
//            assert(Pattern.matches(/*testPattern.toString()*/"aaa", /*testLink*/"bbb"));
            System.out.println(testLink);
            System.out.println(testPattern.toString());
            System.out.println(Pattern.matches(testPattern.toString(), testLink));
        }

//        List<String> articlePatterns = Arrays.asList(DumpLinkGetter.PATTERN_ARTICLES);

//        DumpLinkGetter getter = new DumpLinkGetter(articlePatterns);

//        List<URL> links = getter.getDumpFiles(Language.getByLangCode("en"));
    }
}
