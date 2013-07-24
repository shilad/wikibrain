package org.wikapidia.sr;

import com.jolbox.bonecp.BoneCPDataSource;
import org.apache.lucene.util.Version;
import org.junit.Ignore;
import org.junit.Test;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.sql.LocalArticleSqlDao;
import org.wikapidia.core.dao.sql.LocalLinkSqlDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;
import org.wikapidia.lucene.LuceneOptions;
import org.wikapidia.lucene.LuceneSearcher;
import org.wikapidia.sr.utils.ExplanationFormatter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
*
*/
public class TestESAMetric {

    private static void printResult(SRResult result, ExplanationFormatter expf) throws DaoException {
        if (result == null){
            System.out.println("Result was null");
        }
        else {
            System.out.println("Similarity value: "+result.getNormalized());
            int explanationsSeen = 0;
            for (Explanation explanation : result.getExplanations()){
                System.out.println(expf.formatExplanation(explanation));
                if (++explanationsSeen>5){
                    break;
                }
            }
        }

    }

    private static void printResult(SRResultList results, ExplanationFormatter expf) throws DaoException {
        if (results == null){
            System.out.println("Result was null");
        }
        else {
            for (SRResult srResult : results) {
                printResult(srResult, expf);
            }
        }
    }

//    @Test
//    public void testLuceneOptions() {
//        LuceneOptions options = LuceneOptions.getDefaultOptions();
//        assert (options.matchVersion == Version.LUCENE_43);
//    }

    @Test
    @Ignore
    public void testMostSimilarPages() throws WikapidiaException, DaoException, ConfigurationException, ClassNotFoundException, IOException {

        Configurator c = new Configurator(new Configuration());
        LocalPageDao localPageDao = c.get(LocalPageDao.class);
        ExplanationFormatter expf = new ExplanationFormatter(localPageDao);

        Language lang = Language.getByLangCode("simple");
        LuceneSearcher searcher = new LuceneSearcher(new LanguageSet(Arrays.asList(lang)), LuceneOptions.getDefaultOptions());

        ESAMetric esaMetric = new ESAMetric(lang, searcher, localPageDao);

        String string1 = "Google Search";  //TODO: redirects: homo sapiens, null: nz
        String string2 = "Arts";
        String string3 = "United States";
        String string4 = "Barack Obama";

        LocalPage page1 = localPageDao.getByTitle(lang, new Title(string1, lang), NameSpace.ARTICLE);
        LocalPage page2 = localPageDao.getByTitle(lang, new Title(string2, lang), NameSpace.ARTICLE);
        LocalPage page3 = localPageDao.getByTitle(lang, new Title(string3, lang), NameSpace.ARTICLE);
        LocalPage page4 = localPageDao.getByTitle(lang, new Title(string4, lang), NameSpace.ARTICLE);

//        printResult(esaMetric.similarity(page1, page2, true));
//        printResult(esaMetric.similarity(string1, string2, lang, true));

//        System.out.println(page3);
//        SRResultList srResults= esaMetric.mostSimilar(page3, 10);
//        for (SRResult srResult : srResults) {
//            printResult(srResult, expf);
//        }
//        System.out.println(Arrays.toString(srResults.getScoresAsFloat()));
//
//        System.out.println(page4);
//        SRResultList srResults2= esaMetric.mostSimilar(page4, 10);
//        for (SRResult srResult : srResults2) {
//            printResult(srResult, expf);
//        }
//        System.out.println(Arrays.toString(srResults2.getScoresAsFloat()));
//
        String[] testPhrases = {string3, string4};
        for (int i = 0; i < testPhrases.length; i++) {
            for (int j = i + 1; j < testPhrases.length; j++) {
                SRResult srResult = esaMetric.similarity(testPhrases[i], testPhrases[j], lang, true);
                System.out.println(testPhrases[i] + " and " + testPhrases[j] + ":");
                printResult(srResult, expf);
            }
        }
//
//        LocalPage[] testPages = {page3, page4};
//        for (int i = 0; i < testPages.length; i++) {
//            for (int j = i + 1; j < testPages.length; j++) {
//                SRResult srResult = esaMetric.similarity(testPages[i], testPages[j], true);
//                System.out.println(testPages[i].getTitle().getCanonicalTitle() + " and " + testPages[j].getTitle().getCanonicalTitle() + ":");
//                printResult(srResult, expf);
//            }
//        }
    }

//    @Test
//    public void testPhraseSimilarity() throws DaoException {
//        Language testLanguage = Language.getByLangCode("simple");
//        LuceneSearcher searcher = new LuceneSearcher(new LanguageSet(Arrays.asList(testLanguage)), LuceneOptions.getDefaultOptions());
//        ESAMetric esaMetric = new ESAMetric(testLanguage, searcher);
//        String[] testPhrases = {"United States", "Barack Obama", "geometry", "machine learning"};
//        for (int i = 0; i < testPhrases.length; i++) {
//            for (int j = i; j < testPhrases.length; j++) {
//                SRResult srResult = esaMetric.similarity(testPhrases[i], testPhrases[j], testLanguage, false);
//                System.out.println("Similarity score between " + testPhrases[i] + " and " + testPhrases[j] + " is " + srResult.getValue());
//            }
//        }
//    }

}
