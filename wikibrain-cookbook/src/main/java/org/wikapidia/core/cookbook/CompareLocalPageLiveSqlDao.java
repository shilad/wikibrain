package org.wikapidia.core.cookbook;

import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import au.com.bytecode.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * An Example shows the difference of results between LocalPageLiveDao & LocalPageSqlDao
 * @author Toby "Jiajun" Li
 */
public class CompareLocalPageLiveSqlDao  {
    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
        LocalPageDao localPageLiveDao = new Configurator(new Configuration()).get(LocalPageDao.class, "live");
        LocalPageDao localPageSqlDao = new Configurator(new Configuration()).get(LocalPageDao.class, "sql");
        Language lang = Language.getByLangCode("simple");
        LocalPage livePage, sqlPage;
        File f=new File("./wikAPIdia-cookbook/pagestat.csv");
        String[] entries = new String[3];
        CSVWriter csvWriter = new CSVWriter(new FileWriter(f), ',');

        int liveCount = 0, sqlCount = 0, commonCount = 0;
        for(int i = 0; i < 30; i++){
            int id = (int)(Math.random() * 500000);
            boolean flag = false;
            try {
                livePage = localPageLiveDao.getById(lang, id);
                if(livePage != null){
                    liveCount++;
                    flag = true;
                }
            }
            catch (Exception e){
                try {
                    sqlPage = localPageSqlDao.getById(lang, id);
                    if(sqlPage != null){
                        entries[0] = "LiveDao Failed to get page ";
                        entries[1] = sqlPage.toString();
                        csvWriter.writeNext(entries);
                    }
                }
                catch (Exception e2){}
            }
            try {
                sqlPage = localPageSqlDao.getById(lang, id);
                if(sqlPage != null){
                    sqlCount++;
                    if(flag)
                        commonCount++;
                }
                else
                    throw new Exception();
            }
            catch (Exception e){
                try {
                    livePage = localPageLiveDao.getById(lang, id);
                    if(livePage != null){
                        entries[0] = "SQLDao Failed to get page ";
                        entries[1] = livePage.toString();
                        csvWriter.writeNext(entries);
                    }
                }
                catch (Exception e2){}
            }

        }
        System.out.println("Detailed error information is printed to pagestat.csv at wikAPIdia-cookbook directory");
        System.out.printf("Successfully get live page: %d\nSuccessfully get sql page: %d\nSuccessfully get both page: %d\n", liveCount, sqlCount, commonCount);
        entries = String.format("Successfully get live page: %d#Successfully get sql page: %d#Successfully get both page: %d#", liveCount, sqlCount, commonCount).split("#");
        csvWriter.writeNext(entries);
        csvWriter.close();
    }


}
