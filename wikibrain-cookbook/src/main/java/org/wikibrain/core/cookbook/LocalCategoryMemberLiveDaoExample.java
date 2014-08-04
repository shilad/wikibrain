package org.wikibrain.core.cookbook;


import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.*;
import java.io.IOException;
import java.util.*;

/**
 * An Example shows how LocalPageMemberLiveDao works
 * @author Toby "Jiajun" Li
 */
public class LocalCategoryMemberLiveDaoExample {
    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
        LocalPageDao localPageDao = new Configurator(new Configuration()).get(LocalPageDao.class, "live");
        LocalCategoryMemberDao localCategoryMemberDao = new Configurator(new Configuration()).get(LocalCategoryMemberDao.class, "live");

        Language lang = Language.getByLangCode("en");


        System.out.println("\n\nTest for LocalPageDao and LocalPageDao \n");
        //Test for LocalPageDao and LocalPageDao
        System.out.println(localPageDao.getByTitle(new Title("Minnesota", lang), NameSpace.ARTICLE));
        System.out.println(localPageDao.getByTitle(new Title("Category:States of the United States", lang), NameSpace.ARTICLE));

        System.out.println("\n\nTest for LocalPageDao.getByTitles \n");
        //Test for LocalPageDao.getByTitles
        List<Title> titleList = new ArrayList<Title>();
        titleList.add(new Title("New York", lang));
        titleList.add(new Title("Hawaii", lang));
        Map<Title, LocalPage> articleMap = localPageDao.getByTitles(lang, titleList, NameSpace.ARTICLE);
        for(Map.Entry<Title, LocalPage> e : articleMap.entrySet()){
            System.out.println(e.getKey() + "    " + e.getValue());
        }
        System.out.println("\n\nTest for LocalPageDao.getByTitles \n\n");
        //Test for LocalPageDao.getByTitles
        titleList = new ArrayList<Title>();
        titleList.add(new Title("Category:Midwestern United States", lang));
        titleList.add(new Title("Category:Western United States", lang));
        Map<Title, LocalPage> localCategoryMap = localPageDao.getByTitles(lang, titleList, NameSpace.CATEGORY);
        for(Map.Entry<Title, LocalPage> e : localCategoryMap.entrySet()){
            System.out.println(e.getKey() + "    " + e.getValue());
        }

        System.out.println("\n\nLocalCategoryMemberDao.getCategoryMemberIds (Get all category members of \"Category:Minnesota\" \n");
        //Test for LocalCategoryMemberDao.getCategoryMemberIds
        Collection<Integer> memberList = localCategoryMemberDao.getCategoryMemberIds(lang, 704819);    //Id for Category:Minnesota
        for(Integer e: memberList){
            System.out.println(localPageDao.getById(lang, e));
        }

        System.out.println("\n\nLocalCategoryMemberDao.getCategoryIds (Get all categories of \"Minnesota\" \n");
        //Test for LocalCategoryMemberDao.getCategoryIds
        Collection<Integer> categoryList = localCategoryMemberDao.getCategoryIds(lang, 19590);  //Id for Minnesota
        for(Integer e: categoryList){
            System.out.println(localPageDao.getById(lang, e));
        }

    }


}
