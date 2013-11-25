package org.wikapidia.core.cookbook;

import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;

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

        int liveCount = 0, sqlCount = 0, commonCount = 0;
        for(int i = 0; i < 200; i++){
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
                        System.out.printf("LiveDao Failed to get page ");
                        System.out.println(sqlPage);
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
                        System.out.printf("SqlDao Failed to get page ");
                        System.out.println(livePage);
                    }
                }
                catch (Exception e2){}
            }

        }
        System.out.printf("Successfully get live page: %d\nSuccessfully get sql page: %d\nSuccessfully get both page: %d\n", liveCount, sqlCount, commonCount);
    }

}
