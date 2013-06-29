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
        ArrayList list = new ArrayList<Object>();
        list.add(uk);
        list.add(tower);
        Explanation explanation = new Explanation("? links to ?",list);
        System.out.println(explanation.getPlaintext());
        assert (explanation.getPlaintext().equals("UK links to Tower of London"));
    }


}
