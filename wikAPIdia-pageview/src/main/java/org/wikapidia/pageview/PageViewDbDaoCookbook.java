package org.wikapidia.pageview;

import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalCategoryMemberDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.NameSpace;

import java.io.IOException;
import java.util.*;

/**
 * An Example showing how to get page views for all pages in a given category in a given period of time
 * In this case, we print the page views for each page in the category "Presidents of the United States" in the day of 2013-12-8
 * @author Toby "Jiajun" Li
 */
public class PageViewDbDaoCookbook {
    public static void main(String args[]) throws ConfigurationException, DaoException, IOException, WikapidiaException {

        LocalCategoryMemberDao localCategoryMemberSqlDao = new Configurator(new Configuration()).get(LocalCategoryMemberDao.class, "sql");
        LocalPageDao localPageSqlDao = new Configurator(new Configuration()).get(LocalPageDao.class, "sql");
        Language lang = Language.getByLangCode("simple");
        int categoryId = 5412;      //Category ID for the Category "Category: Presidents of the United States"
        Collection<Integer> memberList = localCategoryMemberSqlDao.getCategoryMemberIds(lang, categoryId);
        PageViewDbDao pageViewDbDao = new PageViewDbDao(lang);
        //get pageview in 24hrs for pages in memberlist starting from 2013-12-08 0:00
        Map<Integer, Integer> pageViewMap = pageViewDbDao.getPageView(memberList, 2013, 12, 8, 0, 24);
        for(Integer member : pageViewMap.keySet()){
            System.out.print(localPageSqlDao.getById(lang, member).getTitle().getCanonicalTitle());
            System.out.printf(" %d\n", pageViewMap.get(member));
        }
    }


}
