package org.wikapidia.core.cookbook;

import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.*;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Toby "Jiajun" Li
 * Date: 10/26/13
 * Time: 9:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class LocalPageLiveDaoExample {
    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
       String string = new String();
       String url = new String("http://en.wikipedia.org//w/api.php?action=query&prop=info&format=json&titles=hi");


       //Test GetTextByURL
       System.out.println(string);
       LocalPageDao testClass = new Configurator(new Configuration()).get(LocalPageDao.class, "live");
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


    }
}




