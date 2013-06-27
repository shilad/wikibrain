package org.wikapidia.download;

import org.junit.Test;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.download.DumpLinkGetter;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Pattern;

/**
 */
public class TestDumpLinkGetter {

    @Test
    public void testDumpLinkGetter() throws IOException {
        for (Language lang : Arrays.asList(Language.getByLangCode("en"))) {
            DumpLinkGetter testGetter = new DumpLinkGetter(lang, Arrays.asList(LinkMatcher.values()), "20130604");
//            System.out.println("" /*+ testGetter.getFileLinks().size()*/ + "\n" + testGetter.getFileLinks() );
        }
    }


//    @Test
//    public void testGetLanguageIndex() throws ParseException {
//        for(Language lang : Language.LANGUAGES) {
//            DumpLinkGetter testGetter = new DumpLinkGetter(DumpLinkGetter.BASEURL_STRING, Arrays.asList(LinkMatcher.values()), lang);
//            try {
//                String date = testGetter.dateSelecter(testGetter.getAvailableDates(lang), testGetter.stringToDate("20131106"));
//                HashMap<String, List<URL>> whoa= testGetter.getDumpFiles(testGetter.getDumpIndexDate(date));
//                System.out.println(testGetter.getDumpIndexDate(date));
//                System.out.println(whoa);
//            } catch (IOException e) {
//                System.err.println("IO");
//            }
//        }
//    }
//
//    @Test
//    public void testIndex() throws IOException {
//        for(Language lang : Language.LANGUAGES) {
//            DumpLinkGetter testGetter = new DumpLinkGetter(DumpLinkGetter.BASEURL_STRING, Arrays.asList(LinkMatcher.values()), lang);
//            String testString = testGetter.getDumpIndex();
//            System.out.println(lang.getId() + " " + testString.substring(214, 240));
//        }
//    }
//
//    @Test
//    public void testParseLinks() throws IOException {
//        for(Language lang : Language.LANGUAGES) {
//            DumpLinkGetter testGetter = new DumpLinkGetter(DumpLinkGetter.BASEURL_STRING, Arrays.asList(LinkMatcher.values()), lang);
//            String html = testGetter.getDumpIndex();
//            List<String> links = testGetter.getLinks(html);
//            System.out.println(lang.getId() + " sample link: " + links.get(2));
//        }
//    }
//
//    @Test
//    public void testGetDumpFiles() throws IOException {
//        List<LinkMatcher> linkMatchers = Arrays.asList(LinkMatcher.ABSTRACT);
//        for(Language lang : Arrays.asList(Language.LANGUAGES)) {
//            DumpLinkGetter testGetter = new DumpLinkGetter(DumpLinkGetter.BASEURL_STRING, linkMatchers, lang);
//            HashMap<String, List<URL>> urlList= testGetter.getDumpFiles();
//            System.out.println(lang.getId() + " " + urlList);
//        }
//    }
//
//    @Test
//    public void testLinks() throws IOException {
//        List<LinkMatcher> linkMatchers = Arrays.asList(LinkMatcher.values());
//        for(Language lang : Language.LANGUAGES){
//            DumpLinkGetter testGetter = new DumpLinkGetter(DumpLinkGetter.BASEURL_STRING, linkMatchers, lang);
//            List<String> allLinks = testGetter.getLinks(testGetter.getDumpIndex());
//            HashMap<String, List<URL>> patternedURLs = testGetter.getDumpFiles();
//            List<String> patternedStrings = new ArrayList<String>();
//            for (int i = 0; i < allLinks.size(); i++) {
//                allLinks.set(i, DumpLinkGetter.BASEURL_STRING.replace("__LANG__", lang.getLangCode().replace("-", "_")) + allLinks.get(i));
//            }
//            for (List<URL> urls : patternedURLs.values()) {
//                for (URL url : urls) {
//                    patternedStrings.add(url.toString());
//                }
//            }
//            for (String link : allLinks) {
//                if (!patternedStrings.contains(link) && !(link.endsWith("rss.xml") || link.endsWith("../"))) {
//                    System.out.println(link);
//                }
//            }
//        }
//    }

//    @Test
//    public void testMain() throws IOException {
//        List<Language> langArray = Arrays.asList(Language.getByLangCode("en"), Language.getByLangCode("zh"));
//        DumpLinkGetter.main(langArray, Arrays.asList("abstract", "stub_meta_current"), "try.tsv");
//    }

}