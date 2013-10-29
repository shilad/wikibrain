package org.wikapidia.concepts.overlap;

import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Analyzes the overlap in popular concepts.
 * Experimental code for Shilad's intro Java course.
 *
 * @author Shilad Sen
 */
public class PopularArticleAnalyzer {
    private final WikAPIdiaWrapper wpApi;

    public PopularArticleAnalyzer(WikAPIdiaWrapper wpApi) {
        this.wpApi = wpApi;
    }

    public void analyzeArticles(Language language) throws DaoException {
        List<LocalPagePopularity> popularities = new ArrayList<LocalPagePopularity>();
        for (LocalPage lp : wpApi.getLocalPages(language)) {
            popularities.add(new LocalPagePopularity(lp, wpApi.getNumInLinks(lp)));
        }

        Collections.sort(popularities);
        Collections.reverse(popularities);

        for (LocalPagePopularity lpp : popularities.subList(0, 20)) {
            LocalPage lp = lpp.getPage();
            int pop = lpp.getPopularity();
            System.out.println("popular page is " + lp + " with pop " + pop);
            for (LocalPage lp2 : wpApi.getInOtherLanguages(lp)) {
                if (lp2 != lp) {
                    System.out.println("\t" + lp2);
                }
            }
        }
    }

    public static void main(String args[]) throws ConfigurationException, DaoException {
        WikAPIdiaWrapper wpApi = new WikAPIdiaWrapper(".");
        PopularArticleAnalyzer analyzer = new PopularArticleAnalyzer(wpApi);
        for (Language lang : wpApi.getLanguages()) {
            System.out.println("Analyzing language: " + lang);
            analyzer.analyzeArticles(lang);
            System.out.println("\n\n\n");
        }
    }
}
