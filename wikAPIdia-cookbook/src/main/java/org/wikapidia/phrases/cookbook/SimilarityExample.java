package org.wikapidia.phrases.cookbook;

import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;
import org.wikapidia.phrases.PhraseAnalyzer;
import org.wikapidia.sr.BaseLocalSRMetric;
import org.wikapidia.sr.Explanation;
import org.wikapidia.sr.MilneWittenInLinkSimilarity;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.disambig.TopResultDisambiguator;

import java.util.HashSet;

/**
 * @author Matt Lesicko
 */
public class SimilarityExample {
    private static void printResult(SRResult result){
        if (result == null){
            System.out.println("Result was null");
        }
        else {
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
        Disambiguator disambiguator = new TopResultDisambiguator(pa);
        LocalLinkDao localLinkDao = c.get(LocalLinkDao.class);
        LocalPageDao localPageDao = c.get(LocalPageDao.class);
        BaseLocalSRMetric sr = new MilneWittenInLinkSimilarity(disambiguator,localLinkDao,localPageDao);
        //Similarity between strings
        String string1 = "Barack Obama";
        String string2 = "US";
        String string3 = "Canada";
        String string4 = "vim";
        printResult(sr.similarity(string1,string2,lang,true));
        System.out.println("Obama and Canada:");
        printResult(sr.similarity(string1,string3,lang,true));
        System.out.println("Obama and vim:");
        printResult(sr.similarity(string1,string4,lang,true));
        System.out.println("Obama and Obama");
        printResult(sr.similarity(string1,string1,lang,true));


        //Similarity between pages
        LocalPage page1 = localPageDao.getByTitle(lang, new Title(string1, lang), NameSpace.ARTICLE);
        System.out.println(page1.getTitle());
        LocalPage page2 = localPageDao.getByTitle(lang, new Title(string2, lang), NameSpace.ARTICLE);
        System.out.println(page2.getTitle());
        LocalPage page3 = localPageDao.getByTitle(lang, new Title(string3,lang),NameSpace.ARTICLE);
        System.out.println(page3.getTitle());
        LocalPage page4 = localPageDao.getByTitle(lang, new Title(string4, lang),NameSpace.ARTICLE);
        System.out.println(page4.getTitle());
        System.out.println("Obama and US:");
        printResult(sr.similarity(page1,page2,true));
        System.out.println("Obama and Canada:");
        printResult(sr.similarity(page1,page3,true));
        System.out.println("Obama and vim:");
        printResult(sr.similarity(page1,page4,true));
        System.out.println("Obama and Obama");
        printResult(sr.similarity(page1,page1,true));


    }
}
