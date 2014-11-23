package org.wikibrain.cookbook.overlap;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;

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
    private final WikiBrainWrapper wpApi;

    public PopularArticleAnalyzer(WikiBrainWrapper wpApi) {
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
        WikiBrainWrapper wpApi = new WikiBrainWrapper("..");
        PopularArticleAnalyzer analyzer = new PopularArticleAnalyzer(wpApi);
        for (Language lang : wpApi.getLanguages()) {
            System.out.println("Analyzing language: " + lang);
            analyzer.analyzeArticles(lang);
            System.out.println("\n\n\n");
        }
    }
}
