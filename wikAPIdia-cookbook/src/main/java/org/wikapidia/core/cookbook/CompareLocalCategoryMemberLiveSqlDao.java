package org.wikapidia.core.cookbook;

import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.lang.Language;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * An Example shows the difference of results between LocalCategoryMemberLiveDao & LocalCategoryMemberSqlDao
 * @author Toby "Jiajun" Li
 */
public class CompareLocalCategoryMemberLiveSqlDao {
    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
        int liveMemberCounter= 0, liveCategoryCounter = 0, sqlMemberCounter = 0, sqlCategoryCounter = 0, commonMemberCounter = 0, commonCategoryCounter = 0;
        Set<Integer> liveMemberSet= new HashSet<Integer>();
        Set<Integer> liveCategorySet= new HashSet<Integer>();


        LocalCategoryMemberDao localCategoryMemberLiveDao = new Configurator(new Configuration()).get(LocalCategoryMemberDao.class, "live");
        LocalPageDao localPageLiveDao = new Configurator(new Configuration()).get(LocalPageDao.class, "live");

        LocalCategoryMemberDao localCategoryMemberSqlDao = new Configurator(new Configuration()).get(LocalCategoryMemberDao.class, "sql");
        LocalPageDao localPageSqlDao = new Configurator(new Configuration()).get(LocalPageDao.class, "sql");

        Language lang = Language.getByLangCode("simple");


        System.out.println("\n\nLocalCategoryMemberDao.getCategoryMemberIds (Get all Live category members of \"Category:Minnesota\" \n");
        //Test for LocalCategoryMemberDao.getCategoryMemberIds
        Collection<Integer> memberList = localCategoryMemberLiveDao.getCategoryMemberIds(lang, 11509);    //Id for Category:Minnesota
        for(Integer e: memberList){
            liveMemberCounter ++;
            liveMemberSet.add(localPageLiveDao.getById(lang, e).getLocalId());
            System.out.println(localPageLiveDao.getById(lang, e));
        }

        System.out.println("\n\nLocalCategoryMemberDao.getCategoryIds (Get all Live categories of \"Minnesota\" \n");
        //Test for LocalCategoryMemberDao.getCategoryIds
        Collection<Integer> categoryList = localCategoryMemberLiveDao.getCategoryIds(lang, 10983);  //Id for Minnesota
        for(Integer e: categoryList){
            liveCategoryCounter ++;
            liveCategorySet.add(localPageLiveDao.getById(lang, e).getLocalId());
            System.out.println(localPageLiveDao.getById(lang, e));
        }

        System.out.println("\n\nLocalCategoryMemberDao.getCategoryMemberIds (Get all SQL category members of \"Category:Minnesota\" \n");
        //Test for LocalCategoryMemberDao.getCategoryMemberIds
        memberList = localCategoryMemberSqlDao.getCategoryMemberIds(lang, 11509);    //Id for Category:Minnesota
        for(Integer e: memberList){
            sqlMemberCounter ++;
            if(liveMemberSet.contains(localPageSqlDao.getById(lang, e).getLocalId())){
                commonMemberCounter ++;
            }
            System.out.println(localPageSqlDao.getById(lang, e));
        }

        System.out.println("\n\nLocalCategoryMemberDao.getCategoryIds (Get all SQL categories of \"Minnesota\" \n");
        //Test for LocalCategoryMemberDao.getCategoryIds
        categoryList = localCategoryMemberSqlDao.getCategoryIds(lang, 10983);  //Id for Minnesota
        for(Integer e: categoryList){
            sqlCategoryCounter ++;
            if(liveCategorySet.contains(localPageSqlDao.getById(lang, e).getLocalId())){
                commonCategoryCounter ++;
            }
            System.out.println(localPageSqlDao.getById(lang, e));
        }

        System.out.printf("\nNumber of members in LiveDao: %d\nNumber of members in SQLDao: %d\nNumber of members in common: %d\n\nNumber of categories in LiveDao: %d\n" +
                "Number of categories in SQLDao: %d\n" +
                "Number of categories in common: %d\n", liveMemberCounter, sqlMemberCounter, commonMemberCounter, liveCategoryCounter, sqlCategoryCounter, commonCategoryCounter);


    }


}
