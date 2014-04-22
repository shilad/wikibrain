package org.wikapidia.sr;

import org.junit.Test;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.Title;
import org.wikapidia.sr.utils.ExplanationFormatter;

import java.util.ArrayList;


public class TestExplanation {
    @Test
    public void test() throws DaoException {
        LanguageInfo lang = LanguageInfo.getByLangCode("en");
        //TODO: Set up test to work for universal pages.
        ExplanationFormatter explanationFormatter = new ExplanationFormatter(null);
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
        assert (explanationFormatter.formatExplanation(pageExplanation).equals("UK links to Tower of London"));

        ArrayList intList = new ArrayList<Integer>();
        intList.add(7);
        Explanation intExplanation = new Explanation("?",intList);
        assert (explanationFormatter.formatExplanation(intExplanation).equals("7"));

    }


}
