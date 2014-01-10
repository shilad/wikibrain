package org.wikapidia.core.cookbook;

import au.com.bytecode.opencsv.CSVWriter;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.NameSpace;

import java.io.File;
import java.io.FileWriter;
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
        Set<Integer> sqlMemberSet= new HashSet<Integer>();
        Set<Integer> sqlCategorySet= new HashSet<Integer>();

        File f=new File("../wikAPIdia-cookbook/memberstat.csv");
        String[] entries = new String[3];
        CSVWriter csvWriter = new CSVWriter(new FileWriter(f), ',');

        LocalCategoryMemberDao localCategoryMemberLiveDao = new Configurator(new Configuration()).get(LocalCategoryMemberDao.class, "live");
        LocalPageDao localPageLiveDao = new Configurator(new Configuration()).get(LocalPageDao.class, "live");

        LocalCategoryMemberDao localCategoryMemberSqlDao = new Configurator(new Configuration()).get(LocalCategoryMemberDao.class, "sql");
        LocalPageDao localPageSqlDao = new Configurator(new Configuration()).get(LocalPageDao.class, "sql");

        Language lang = Language.getByLangCode("simple");


        int categoryId = localPageLiveDao.getIdByTitle("Category:Geography of the United States", lang, NameSpace.getNameSpaceByArbitraryId(14));
        int pageId = localPageLiveDao.getIdByTitle("USA", lang, NameSpace.getNameSpaceByArbitraryId(0));


        //Test for LocalCategoryMemberDao.getCategoryMemberIds
        Collection<Integer> memberList = localCategoryMemberLiveDao.getCategoryMemberIds(lang, categoryId);    //Id for Category:Minnesota
        for(Integer e: memberList){
            liveMemberCounter ++;
            liveMemberSet.add(e);

        }


        //Test for LocalCategoryMemberDao.getCategoryIds
        Collection<Integer> categoryList = localCategoryMemberLiveDao.getCategoryIds(lang, pageId);  //Id for Minnesota
        for(Integer e: categoryList){
            liveCategoryCounter ++;
            liveCategorySet.add(e);

        }


        //Test for LocalCategoryMemberDao.getCategoryMemberIds
        memberList = localCategoryMemberSqlDao.getCategoryMemberIds(lang, categoryId);    //Id for Category:Minnesota
        for(Integer e: memberList){
            sqlMemberCounter ++;
            sqlMemberSet.add(e);
            if(liveMemberSet.contains(e)){
                commonMemberCounter ++;
            }
            else{
                entries[0] = "LiveDao Failed to get member";
                entries[1] = localPageSqlDao.getById(lang, e.intValue()).toString();
                csvWriter.writeNext(entries);
            }

        }


        //Test for LocalCategoryMemberDao.getCategoryIds
        categoryList = localCategoryMemberSqlDao.getCategoryIds(lang, pageId);  //Id for Minnesota
        for(Integer e: categoryList){
            sqlCategoryCounter ++;
            sqlCategorySet.add(e);
            if(liveCategorySet.contains(e)){
                commonCategoryCounter ++;
            }
            else{
                entries[0] = "LiveDao Failed to get category";
                entries[1] = localPageSqlDao.getById(lang, e.intValue()).toString();
                csvWriter.writeNext(entries);
            }

        }

        for (Integer e: liveMemberSet){
            if(!sqlMemberSet.contains(e)){
                entries[0] = "SQLDao Failed to get member";
                entries[1] = localPageLiveDao.getById(lang, e.intValue()).toString();
                csvWriter.writeNext(entries);
            }
        }

        for (Integer e: liveCategorySet){
            if(!sqlCategorySet.contains(e)){
                entries[0] = "SQLDao Failed to get category";
                entries[1] = localPageLiveDao.getById(lang, e.intValue()).toString();
                csvWriter.writeNext(entries);
            }
        }



        System.out.printf("\nNumber of members in LiveDao: %d\nNumber of members in SQLDao: %d\nNumber of members in common: %d\n\nNumber of categories in LiveDao: %d\n" +
                "Number of categories in SQLDao: %d\n" +
                "Number of categories in common: %d\n", liveMemberCounter, sqlMemberCounter, commonMemberCounter, liveCategoryCounter, sqlCategoryCounter, commonCategoryCounter);
        System.out.println("Detailed error information is printed to memberstat.csv at wikAPIdia-cookbook directory");
        entries = String.format("Number of members in LiveDao: %d#Number of members in SQLDao: %d#Number of members in common: %d", liveMemberCounter, sqlMemberCounter, commonMemberCounter).split("#");
        csvWriter.writeNext(entries);
        entries = String.format("Number of categories in LiveDao: %d#Number of categories in SQLDao: %d#Number of categories in common: %d", liveCategoryCounter, sqlCategoryCounter, commonCategoryCounter).split("#");
        csvWriter.writeNext(entries);

        csvWriter.close();

    }



}
