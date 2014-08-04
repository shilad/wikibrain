package org.wikibrain.sr;

import org.junit.Test;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.Title;
import org.wikibrain.sr.utils.ExplanationFormatter;

import java.util.ArrayList;


public class TestExplanation {
    @Test
    public void test() throws DaoException {
        LanguageInfo lang = LanguageInfo.getByLangCode("en");
        //TODO: Set up test to work for universal pages.
        ExplanationFormatter explanationFormatter = new ExplanationFormatter(null);
        LocalPage uk = new LocalPage(
                lang.getLanguage(),
                1,
                new Title("UK",lang),
                NameSpace.ARTICLE);
        LocalPage tower = new LocalPage(
                lang.getLanguage(),
                2,
                new Title("Tower of London",lang),
                NameSpace.ARTICLE);
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
