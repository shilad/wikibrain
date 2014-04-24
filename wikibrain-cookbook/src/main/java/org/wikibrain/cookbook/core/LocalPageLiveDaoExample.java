package org.wikibrain.cookbook.core;

import gnu.trove.list.TIntList;
import gnu.trove.map.TIntIntMap;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.live.LocalPageLiveDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.*;
import java.io.IOException;

/**
 * An Example shows how LocalPageLiveDao works
 * @author Toby "Jiajun" Li
 */

public class LocalPageLiveDaoExample {
    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
       String string = new String();
       String url = new String("http://en.wikipedia.org//w/api.php?action=query&prop=info&format=json&titles=hi");


       //Test GetTextByURL
       System.out.println(string);
       LocalPageLiveDao testClass = (LocalPageLiveDao) new Configurator(new Configuration()).get(LocalPageDao.class, "live");
       Language lang = Language.getByLangCode("en");

       System.out.println(testClass.getByTitle(new Title("Apple", Language.getByLangCode("en")), NameSpace.getNameSpaceByArbitraryId(0)));
       System.out.println(testClass.getByTitle(new Title("University of Minnesota", Language.getByLangCode("en")), NameSpace.getNameSpaceByArbitraryId(0)));
       System.out.println(testClass.getById(lang,16308));

       System.out.println(testClass.getById(lang,416813));

        //Test Following/Unfollowing Redirect
        //Follow Redirect
        System.out.println(testClass.getByTitle(new Title("Apple Tree", Language.getByLangCode("en")), NameSpace.getNameSpaceByArbitraryId(0)));
        System.out.println("isRedirect? "+testClass.getByTitle(new Title("Apple Tree", lang), NameSpace.getNameSpaceByArbitraryId(0)).isRedirect());
        //Not Follow Redirect (Get a Redirect Page)
        testClass.setFollowRedirects(false);
        System.out.println(testClass.getByTitle(new Title("Apple Tree", Language.getByLangCode("en")), NameSpace.getNameSpaceByArbitraryId(0)));
        System.out.println("isRedirect? "+testClass.getByTitle(new Title("Apple Tree", lang), NameSpace.getNameSpaceByArbitraryId(0)).isRedirect());

        //Test Disambig
        System.out.println(testClass.getById(lang,32672164));
        System.out.println("isDisambig? "+testClass.getById(lang,32672164).isDisambig());

        System.out.println(testClass.getIdByTitle(new Title("Minnesota", lang)));

        Language simple = Language.getByLangCode("simple");
        //Test retrieval of all categories in simple
        double start = System.currentTimeMillis();
        try {
            TIntList allCategoryPageIds = testClass.getAllPageIdsInNamespace(lang, NameSpace.CATEGORY);
            double elapsed = (System.currentTimeMillis() - start) / 1000.0;
            System.out.println("Retrieved all categories in " + lang + " in " + elapsed + " seconds");
            System.out.println("\nNumber of categories:" + allCategoryPageIds.size());
            System.out.println("\nFirst 500 categories in language " + lang.getLangCode() + ":");
            for (int i = 0; i < 500 && i < allCategoryPageIds.size(); i++) {
                int categoryId = allCategoryPageIds.get(i);
                LocalPage category = testClass.getById(lang, categoryId);
                System.out.println("\t" + categoryId + ": " + category.getTitle());
            }
        }
        catch (OutOfMemoryError e) {
            System.out.println((System.currentTimeMillis() - start) / 1000.0);
        }

        //Test retrieval of all pages in simple
        start = System.currentTimeMillis();
        try {
            //following line takes about 1.5 min
            TIntIntMap pages = testClass.getAllPageIdNamespaceMappings(simple);
            double elapsed = (System.currentTimeMillis() - start) / 1000.0;
            System.out.println("Retrieved all pages in simple in " + elapsed + " seconds");
            //about 140 thousand pages returned
            System.out.println("\nNumber of pages: " + pages.size());
            System.out.println("\nFirst 500 pages in simple:");
            int pageCount = 0;
            for (int pageId : pages.keys()) {
                if (pageCount >= 500) {
                    break;
                }
                Title pageTitle = testClass.getById(simple, pageId).getTitle();
                System.out.println("\tPage: " + pageTitle + "; Namespace: " + NameSpace.getNameSpaceByArbitraryId(pages.get(pageId)));
                pageCount++;
            }
        }
        catch (OutOfMemoryError e) {
            System.out.println((System.currentTimeMillis() - start) / 1000.0);
        }
    }
}




