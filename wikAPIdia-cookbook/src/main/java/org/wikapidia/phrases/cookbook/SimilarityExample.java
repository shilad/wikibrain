package org.wikapidia.phrases.cookbook;

import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalLinkDao;
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
import org.wikapidia.sr.disambig.TopResultDisambiguator;


/**
 * @author Matt Lesicko
 * @author Ben Hillmann
 */
public class SimilarityExample {
    private static void localPrintResult(SRResult result, Language language,LocalPageDao localPageDao) throws DaoException {
        if (result == null){
            System.out.println("Result was null");
        }
        else {
            System.out.println(localPageDao.getById(language, result.getId()).getTitle().getCanonicalTitle());
            System.out.println("Similarity value: "+result.getValue());
            int explanationsSeen = 0;
            for (Explanation explanation : result.getExplanations()){
                System.out.println(explanation.getPlaintext());
                if (++explanationsSeen>5){
                    break;
                }
            }
        }

    }

        private static void universalPrintResult(SRResult result, int algorithmId, UniversalPageDao universalPageDao) throws DaoException {
            if (result == null){
                System.out.println("Result was null");
            }
            else {
                UniversalPage up = universalPageDao.getById(result.getId(), algorithmId);
                LanguageSet languages = up.getLanguageSetOfExistsInLangs();
                System.out.println(up.getLocalPages(languages.getDefaultLanguage()));
                System.out.println("Similarity value: "+result.getValue());
                int explanationsSeen = 0;
                for (Explanation explanation : result.getExplanations()){
                    System.out.println(explanation.getPlaintext());
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
        PhraseAnalyzer pa = c.get(PhraseAnalyzer.class,"anchortext");
        LocalPageDao localPageDao = c.get(LocalPageDao.class);
        LocalSRMetric sr = c.get(LocalSRMetric.class);
        UniversalSRMetric usr = c.get(UniversalSRMetric.class);
        Disambiguator disambiguator = c.get(Disambiguator.class);
        UniversalPageDao universalPageDao = c.get(UniversalPageDao.class);

        //Similarity between strings
//        String string1 = "Barack Obama";
//        String string2 = "US";
//        String string3 = "Canada";
//        String string4 = "vim";
//        System.out.println("Barack Obama and US:");
//        printResult(sr.similarity(string1,string2,lang,true),lang,localPageDao);
//        System.out.println("Barack Obama and Canada:");
//        printResult(sr.similarity(string1,string3,lang,true),lang,localPageDao);
//        System.out.println("Barack Obama and vim:");
//        printResult(sr.similarity(string1,string4,lang,true),lang,localPageDao);
//        System.out.println("Barack Obama and Barack Obama");
//        printResult(sr.similarity(string1,string1,lang,true),lang,localPageDao);


        //Similarity between pages
//        LocalPage page1 = localPageDao.getByTitle(lang, new Title(string1, lang), NameSpace.ARTICLE);
//        LocalPage page2 = localPageDao.getByTitle(lang, new Title(string2, lang), NameSpace.ARTICLE);
//        LocalPage page3 = localPageDao.getByTitle(lang, new Title(string3,lang),NameSpace.ARTICLE);
//        LocalPage page4 = localPageDao.getByTitle(lang, new Title(string4, lang),NameSpace.ARTICLE);
//        System.out.println("Barack Obama and US:");
//        printResult(sr.similarity(page1,page2,true),lang,localPageDao);
//        System.out.println("Barack Obama and Canada:");
//        printResult(sr.similarity(page1,page3,true),lang,localPageDao);
//        System.out.println("Barack Obama and vim:");
//        printResult(sr.similarity(page1,page4,true),lang,localPageDao);
//        System.out.println("Barack Obama and Barack Obama:");
//        printResult(sr.similarity(page1,page1,true),lang,localPageDao);

        //Most Similar pages
//        System.out.println("Most similar to goat:");
//        SRResultList resultList = sr.mostSimilar(new LocalString(lang, "goat"), 5, true);
//        for (int i=0; i<resultList.numDocs(); i++){
//            System.out.println("#" + (i + 1));
//            printResult(resultList.get(i),lang,localPageDao);
//        }
//        System.out.println("Most similar to science fiction:");
//        resultList = sr.mostSimilar(new LocalString(lang, "science fiction"), 5, true);
//        for (int i=0; i<resultList.numDocs(); i++){
//            System.out.println("#" + (i + 1));
//            printResult(resultList.get(i),lang,localPageDao);
//        }
//
//        System.out.println("Most similar to goat using universal");
//        LocalId localId = disambiguator.disambiguate(new LocalString(lang,"goat"),null);
//        int uId = universalPageDao.getUnivPageId(localId.asLocalPage(),0);
//        System.out.println("Local: "+localId.getId()+"\nUniversal: "+uId);
//        resultList = usr.mostSimilar(new LocalString(lang, "goat"), 5, true);
//        for (int i=0; i<resultList.numDocs(); i++){
//            System.out.println("#"+(i+1));
//            printResult(resultList.get(i),lang,localPageDao);
//        }

        System.out.println("Most similar to science fiction using universal");
        long start = System.currentTimeMillis();
        SRResultList resultList = usr.mostSimilar(new LocalString(lang, "science fiction"), 5, true);
        System.out.println((start-System.currentTimeMillis())/1000);
        if (resultList.numDocs()==0){
            System.out.println("Didn't find shit.");
        }
        for (int i=0; i<resultList.numDocs(); i++){
            System.out.println("#"+(i+1));
            universalPrintResult(resultList.get(i),usr.getAlgorithmId(),universalPageDao);
        }
    }
}
