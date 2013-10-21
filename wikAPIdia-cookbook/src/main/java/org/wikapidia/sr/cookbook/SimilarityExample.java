package org.wikapidia.sr.cookbook;

import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.UniversalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.phrases.PhraseAnalyzer;
import org.wikapidia.sr.*;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.utils.ExplanationFormatter;

/**
 * @author Matt Lesicko
 * @author Ben Hillmann
 * \/\/1|<1P3[)14
 */
public class SimilarityExample {
    private static void localPrintResult(SRResult result, Language language,LocalPageDao localPageDao, ExplanationFormatter expf) throws DaoException {
        if (result == null){
            System.out.println("Result was null");
        }
        else {
            LocalPage namepage = localPageDao.getById(language, result.getId());
            if (namepage!=null){
                System.out.println(namepage.getTitle().getCanonicalTitle());
            }
            System.out.println("Similarity score: "+result.getScore());
            int explanationsSeen = 0;
            for (Explanation explanation : result.getExplanations()){
                System.out.println(expf.formatExplanation(explanation));
                if (++explanationsSeen>5){
                    break;
                }
            }
        }

    }

        private static void universalPrintResult(SRResult result, int algorithmId, UniversalPageDao universalPageDao, LocalPageDao localPageDao, ExplanationFormatter expf) throws DaoException {
            if (result == null){
                System.out.println("Result was null");
            }
            else {
                UniversalPage up = universalPageDao.getById(result.getId(), algorithmId);
                LanguageSet languages = up.getLanguageSet();
                LocalId nameId = (LocalId) up.getLocalPages(languages.getDefaultLanguage()).toArray()[0];
                LocalPage namePage = localPageDao.getById(nameId.getLanguage(),nameId.getId());
                System.out.println(namePage.getTitle().getCanonicalTitle());
                System.out.println("Similarity score: "+result.getScore());
                int explanationsSeen = 0;
                for (Explanation explanation : result.getExplanations()){
                    System.out.println(expf.formatExplanation(explanation));
                    if (++explanationsSeen>5){
                        break;
                    }
                }
            }

        }


    public static void main(String args[]) throws ConfigurationException, DaoException {
        //Set-up
        Language lang = Language.getByLangCode("simple");
        Configurator c = new Configurator(new Configuration());
        LocalPageDao localPageDao = c.get(LocalPageDao.class);
        LocalSRMetric sr = c.get(LocalSRMetric.class);
        UniversalSRMetric usr = c.get(UniversalSRMetric.class);
        UniversalPageDao universalPageDao = c.get(UniversalPageDao.class);
        ExplanationFormatter expf = new ExplanationFormatter(localPageDao);

        String path = c.getConf().get().getString("sr.metric.path");

        //Similarity between strings
        String s1 = "Barack Obama";
        String s2 = "US";
        String s3 = "Canada";
        String s4 = "vim";
//        System.out.println("Using local");
//        System.out.println("Barack Obama and US:");
//        localPrintResult(sr.similarity(s1,s2,lang,true),lang,localPageDao, expf);
//        System.out.println("Barack Obama and Canada:");
//        localPrintResult(sr.similarity(s1,s3,lang,true),lang,localPageDao, expf);
//        System.out.println("Barack Obama and vim:");
//        localPrintResult(sr.similarity(s1,s4,lang,true),lang,localPageDao, expf);
//        System.out.println("Barack Obama and Barack Obama");
//        localPrintResult(sr.similarity(s1,s1,lang,true),lang,localPageDao, expf);

//        LocalString ls1 = new LocalString(lang, s1);
//        LocalString ls2 = new LocalString(lang, s2);
//        LocalString ls3 = new LocalString(lang, s3);
//        LocalString ls4 = new LocalString(lang, s4);
//        System.out.println("Using universal");
//        System.out.println("Barack Obama and US:");
//        universalPrintResult(usr.similarity(ls1,ls2,true),usr.getAlgorithmId(),universalPageDao, localPageDao, expf);
//        System.out.println("Barack Obama and Canada:");
//        universalPrintResult(usr.similarity(ls1,ls3,true),usr.getAlgorithmId(),universalPageDao, localPageDao, expf);
//        System.out.println("Barack Obama and vim:");
//        universalPrintResult(usr.similarity(ls1,ls4,true),usr.getAlgorithmId(),universalPageDao, localPageDao, expf);
//        System.out.println("Barack Obama and Barack Obama");
//        universalPrintResult(usr.similarity(ls1,ls1,true),usr.getAlgorithmId(),universalPageDao, localPageDao, expf);

        //Similarity between pages
        LocalPage page1 = localPageDao.getByTitle(lang, new Title(s1, lang), NameSpace.ARTICLE);
        LocalPage page2 = localPageDao.getByTitle(lang, new Title(s2, lang), NameSpace.ARTICLE);
        LocalPage page3 = localPageDao.getByTitle(lang, new Title(s3,lang),NameSpace.ARTICLE);
        LocalPage page4 = localPageDao.getByTitle(lang, new Title(s4, lang),NameSpace.ARTICLE);
//        System.out.println("Barack Obama and US:");
//        localPrintResult(sr.similarity(page1,page2,true),lang,localPageDao, expf);
//        System.out.println("Barack Obama and Canada:");
//        localPrintResult(sr.similarity(page1,page3,true),lang,localPageDao, expf);
//        System.out.println("Barack Obama and vim:");
//        localPrintResult(sr.similarity(page1,page4,true),lang,localPageDao, expf);
//        System.out.println("Barack Obama and Barack Obama:");
//        localPrintResult(sr.similarity(page1,page1,true),lang,localPageDao, expf);
//
//        Most Similar pages
        System.out.println("Most similar to United States:");
        SRResultList resultList = sr.mostSimilar(page2, 5);
        for (int i=0; i<resultList.numDocs(); i++){
            System.out.println("#" + (i + 1));
            localPrintResult(resultList.get(i),lang,localPageDao, expf);
        }
//        System.out.println("Most similar to science fiction:");
//        SRResultList resultList = sr.mostSimilar(new LocalString(lang, "science fiction"), 5);
//        for (int i=0; i<resultList.numDocs(); i++){
//            System.out.println("#" + (i + 1));
//            localPrintResult(resultList.get(i),lang,localPageDao, expf);
//        }

        System.out.println("Most similar to natural language processing:");
        resultList = sr.mostSimilar(new LocalString(lang, "natural language processing"), 5);
        for (int i=0; i<resultList.numDocs(); i++){
            System.out.println("#" + (i + 1));
            localPrintResult(resultList.get(i),lang,localPageDao, expf);
        }

//        System.out.println("Most similar to natural language processing:");
//        resultList = sr.mostSimilar(new LocalString(lang, "natural language processing"), 10000);
//        for (int i=0; i<5; i++){
//            System.out.println("#" + (i + 1));
//            localPrintResult(resultList.get(i),lang,localPageDao, expf);
//        }

//        System.out.println("Most similar to goat using universal");
//        resultList = usr.mostSimilar(new LocalString(lang, "goat"), 5);
//        for (int i=0; i<resultList.numDocs(); i++){
//            System.out.println("#"+(i+1));
//            universalPrintResult(resultList.get(i),usr.getAlgorithmId(),universalPageDao, localPageDao, expf);
//        }
//
//        System.out.println("Most similar to science fiction using universal");
//        resultList = usr.mostSimilar(new LocalString(lang, "science fiction"), 5);
//        for (int i=0; i<resultList.numDocs(); i++){
//            System.out.println("#"+(i+1));
//            universalPrintResult(resultList.get(i),usr.getAlgorithmId(),universalPageDao, localPageDao, expf);
//        }
    }
}
