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
 * An Example shows how LocalCategoryMemberLiveDao works
 * @author Toby "Jiajun" Li
 */
public class LocalCategoryMemberLiveDaoExample {
    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
        LocalPageDao localPageDao = new Configurator(new Configuration()).get(LocalPageDao.class, "live");
        LocalArticleDao localArticleDao = new Configurator(new Configuration()).get(LocalArticleDao.class, "live");
        LocalCategoryDao localCategoryDao = new Configurator(new Configuration()).get(LocalCategoryDao.class, "live");
        LocalCategoryMemberDao localCategoryMemberDao = new Configurator(new Configuration()).get(LocalCategoryMemberDao.class, "live");

        Language lang = Language.getByLangCode("en");


        System.out.println("\n\nTest for LocalArticleDao and LocalCategoryDao \n");
        //Test for LocalArticleDao and LocalCategoryDao
        System.out.println(localArticleDao.getByTitle(lang, new Title("Minnesota", lang)));
        System.out.println(localCategoryDao.getByTitle(lang, new Title("Category:States of the United States", lang)));

        System.out.println("\n\nTest for LocalArticleDao.getByTitles \n");
        //Test for LocalArticleDao.getByTitles
        List<Title> titleList = new ArrayList<Title>();
        titleList.add(new Title("New York", lang));
        titleList.add(new Title("Hawaii", lang));
        Map<Title, LocalArticle> articleMap = localArticleDao.getByTitles(lang, titleList);
        for(Map.Entry<Title, LocalArticle> e : articleMap.entrySet()){
            System.out.println(e.getKey() + "    " + e.getValue());
        }
        System.out.println("\n\nTest for LocalCategoryDao.getByTitles \n\n");
        //Test for LocalCategoryDao.getByTitles
        titleList = new ArrayList<Title>();
        titleList.add(new Title("Category:Midwestern United States", lang));
        titleList.add(new Title("Category:Western United States", lang));
        Map<Title, LocalCategory> localCategoryMap = localCategoryDao.getByTitles(lang, titleList);
        for(Map.Entry<Title, LocalCategory> e : localCategoryMap.entrySet()){
            System.out.println(e.getKey() + "    " + e.getValue());
        }

        System.out.println("\n\nLocalCategoryMemberDao.getCategoryMemberIds (Get all category members of \"Category:Minnesota\" \n");
        //Test for LocalCategoryMemberDao.getCategoryMemberIds
        Collection<Integer> memberList = localCategoryMemberDao.getCategoryMemberIds(lang, 704819);    //Id for Category:Minnesota
        for(Integer e: memberList){
            System.out.println(localArticleDao.getById(lang, e));
        }

        System.out.println("\n\nLocalCategoryMemberDao.getCategoryIds (Get all categories of \"Minnesota\" \n");
        //Test for LocalCategoryMemberDao.getCategoryIds
        Collection<Integer> categoryList = localCategoryMemberDao.getCategoryIds(lang, 19590);  //Id for Minnesota
        for(Integer e: categoryList){
            System.out.println(localCategoryDao.getById(lang, e));
        }

    }


}
