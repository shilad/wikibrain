package org.wikapidia.pageview;


import org.joda.time.DateTime;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;

/**
 * @author Toby "Jiajun" Li
 */
public class PageViewMapDaoExample {

    public static void main(String args[]) throws ConfigurationException, DaoException, WikapidiaException{

        PageViewMapDao pageViewMapDao = new PageViewMapDao();
        Language lang = Language.getByLangCode("simple");
        Env env = new EnvBuilder().build();
        Configurator configurator = env.getConfigurator();
        LocalPageDao pdao = configurator.get(LocalPageDao.class, "sql");
        PageViewIterator it = new PageViewIterator(lang, 2013, 12, 8, 1, 2013, 12, 8, 12);
        PageViewDataStruct data;
        //int i = 0;
        while(it.hasNext()){
            data = it.next();
            pageViewMapDao.addData(data);

        }
        System.out.println(pageViewMapDao.getPageView(25578, new DateTime(2013, 12, 8, 2, 0), new DateTime(2013, 12, 8, 12, 0)));


    }



}
