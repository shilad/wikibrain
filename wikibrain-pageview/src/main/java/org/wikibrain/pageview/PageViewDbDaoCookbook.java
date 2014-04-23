package org.wikibrain.pageview;

import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalCategoryMemberDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.NameSpace;

import java.io.IOException;
import java.util.*;

/**
 * An Example showing how to get page views for all pages in a given category in a given period of time
 * In this case, we print the page views for each page in the category "Presidents of the United States" in the day of 2013-12-8
 * @author Toby "Jiajun" Li
 */
public class PageViewDbDaoCookbook {
    public static void main(String args[]) throws ConfigurationException, DaoException, IOException, WikiBrainException {

        LocalCategoryMemberDao localCategoryMemberSqlDao = new Configurator(new Configuration()).get(LocalCategoryMemberDao.class, "sql");
        LocalPageDao localPageSqlDao = new Configurator(new Configuration()).get(LocalPageDao.class, "sql");
        Language lang = Language.getByLangCode("simple");
        int categoryId = 5412;      //Category ID for the Category "Category: Presidents of the United States"
        Collection<Integer> memberList = localCategoryMemberSqlDao.getCategoryMemberIds(lang, categoryId);
        PageViewDbDao pageViewDbDao = new PageViewDbDao(lang);
        //get pageview in 24hrs for pages in memberlist starting from 2013-12-08 0:00
        Map<Integer, Integer> pageViewMap = pageViewDbDao.getPageView(memberList, 2014, 4, 1, 0, 24);
        for(Integer member : pageViewMap.keySet()){
            System.out.print(localPageSqlDao.getById(lang, member).getTitle().getCanonicalTitle());
            System.out.printf(" %d\n", pageViewMap.get(member));
        }
    }


}
