package org.wikapidia.sr;

import org.junit.Test;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.Title;

import java.util.ArrayList;


public class TestExplanation {
    @Test
    public void test(){
        LanguageInfo lang = LanguageInfo.getByLangCode("en");
        LocalArticle uk = new LocalArticle(
                lang.getLanguage(),
                1,
                new Title("UK",lang));
        LocalArticle tower = new LocalArticle(
                lang.getLanguage(),
                2,
                new Title("Tower of London",lang));
        ArrayList pageList = new ArrayList<Object>();
        pageList.add(uk);
        pageList.add(tower);
        Explanation pageExplanation = new Explanation("? links to ?",pageList);
        assert (pageExplanation.getPlaintext().equals("UK links to Tower of London"));

        ArrayList intList = new ArrayList<Integer>();
        intList.add(7);
        Explanation intExplanation = new Explanation("?",intList);
        assert (intExplanation.getPlaintext().equals("7"));

    }


}
