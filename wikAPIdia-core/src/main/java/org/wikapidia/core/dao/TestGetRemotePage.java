package org.wikapidia.core.dao;

import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.*;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: toby
 * Date: 10/26/13
 * Time: 9:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestGetRemotePage {
    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
       String string = new String();
       String url = new String("http://en.wikipedia.org//w/api.php?action=query&prop=info&format=json&titles=hi");
       try{
           string = GetTextByURL.getText(url);
       }
       catch(Exception e){
           System.out.println("Error get info from wiki server");
       }

       //Test GetTextByURL
       System.out.println(string);

       GetRemotePage testClass = new GetRemotePage();


       System.out.println(testClass.getByTitle(Language.getByLangCode("en"),new Title("Apple", Language.getByLangCode("en")), NameSpace.getNameSpaceByArbitraryId(0)));
       System.out.println(testClass.getByTitle(Language.getByLangCode("en"),new Title("University of Minnesota", Language.getByLangCode("en")), NameSpace.getNameSpaceByArbitraryId(0)));
       System.out.println(testClass.getById(Language.getByLangCode("en"),16308));

       System.out.println(testClass.getById(Language.getByLangCode("en"),416813));

        //Test Following/Unfollowing Redirect
        //Follow Redirect
        System.out.println(testClass.getByTitle(Language.getByLangCode("en"),new Title("Apple Tree", Language.getByLangCode("en")), NameSpace.getNameSpaceByArbitraryId(0)));
        System.out.println("isRedirect? "+testClass.getByTitle(Language.getByLangCode("en"),new Title("Apple Tree", Language.getByLangCode("en")), NameSpace.getNameSpaceByArbitraryId(0)).isRedirect());
        //Not Follow Redirect (Get a Redirect Page)
        testClass.setFollowRedirects(false);
        System.out.println(testClass.getByTitle(Language.getByLangCode("en"),new Title("Apple Tree", Language.getByLangCode("en")), NameSpace.getNameSpaceByArbitraryId(0)));
        System.out.println("isRedirect? "+testClass.getByTitle(Language.getByLangCode("en"),new Title("Apple Tree", Language.getByLangCode("en")), NameSpace.getNameSpaceByArbitraryId(0)).isRedirect());

        //Test Disambig
        System.out.println(testClass.getById(Language.getByLangCode("en"),32672164));
        System.out.println("isDisambig? "+testClass.getById(Language.getByLangCode("en"),32672164).isDisambig());

        System.out.println(testClass.getIdByTitle(new Title("Minnesota", Language.getByLangCode("en"))));


    }
}




