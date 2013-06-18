package org.wikapidia.download;

import org.junit.Test;
import org.wikapidia.core.lang.Language;
import org.wikapidia.download.DumpLinkGetter;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 */
public class TestDumpLinkGetter {

    @Test
    public void testIndex() throws IOException {
        for(Language lang : Language.LANGUAGES) {
            DumpLinkGetter testGetter = new DumpLinkGetter(Arrays.asList(LinkMatcher.values()), lang);
            String testString = testGetter.getDumpIndex();
            System.out.println(lang.getId() + " " + testString.substring(214, 240));
        }
    }

    @Test
    public void testParseLinks() throws IOException {
        for(Language lang : Language.LANGUAGES) {
            DumpLinkGetter testGetter = new DumpLinkGetter(Arrays.asList(LinkMatcher.values()), lang);
            List<URL> links = testGetter.getMatchingPatterns(testGetter.getDumpIndex(), Pattern.compile(DumpLinkGetter.PATTERN_ABSTRACT));
            System.out.println(links);
        }
    }

    @Test
    public void testGetDumpFiles() throws IOException {
        List<String> patterns = Arrays.asList(LinkMatcher.values().toString());
        for(Language lang : Language.LANGUAGES){
            DumpLinkGetter testGetter = new DumpLinkGetter(Arrays.asList(LinkMatcher.values()), lang);
            List<URL> urlList= testGetter.getDumpFiles();
            System.out.println(lang.getId() + " " + urlList);
        }
    }
}
