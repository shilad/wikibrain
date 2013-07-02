package org.wikapidia.phrases.cookbook;

import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;
import org.wikapidia.phrases.PhraseAnalyzer;
import org.wikapidia.sr.BaseLocalSRMetric;
import org.wikapidia.sr.Explanation;
import org.wikapidia.sr.MilneWittenSimilarity;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.disambig.TopResultDisambiguator;

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
        Language lang = Language.getByLangCode("simple");
        Configurator c = new Configurator(new Configuration());
        PhraseAnalyzer pa = c.get(PhraseAnalyzer.class,"anchortext");
        Disambiguator disambiguator = new TopResultDisambiguator(pa);
        LocalLinkDao localLinkDao = c.get(LocalLinkDao.class);
        LocalPageDao localPageDao = c.get(LocalPageDao.class);
        BaseLocalSRMetric sr = new MilneWittenSimilarity(disambiguator,localLinkDao,localPageDao);
        LocalPage page1 = localPageDao.getByTitle(lang, new Title("Obama", lang), NameSpace.ARTICLE);
        System.out.println(page1.getTitle());
        LocalPage page2 = localPageDao.getByTitle(lang, new Title("US", lang), NameSpace.ARTICLE);
        System.out.println(page2.getTitle());
        LocalPage page3 = localPageDao.getByTitle(lang, new Title("Canada",lang),NameSpace.ARTICLE);
        System.out.println(page3.getTitle());
        LocalPage page4 = localPageDao.getByTitle(lang, new Title("Vim", lang),NameSpace.ARTICLE);
        System.out.println(page4.getTitle());
        System.out.println("Obama and US:");
        printResult(sr.similarity(page1,page2,true));
        System.out.println("Obama and Canada:");
        printResult(sr.similarity(page1,page3,true));
        System.out.println("Obama and vim:");
        printResult(sr.similarity(page1,page4,true));
    }
}
