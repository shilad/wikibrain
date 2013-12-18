package org.wikapidia.pageview;


import gnu.trove.map.hash.TIntIntHashMap;
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
public class PageViewDaoExample {

    public static void main(String args[]) throws ConfigurationException, DaoException, WikapidiaException{

        PageViewDao pageViewDao = new PageViewDao();
        Language lang = Language.getByLangCode("simple");
        Env env = new EnvBuilder().build();
        Configurator configurator = env.getConfigurator();
        LocalPageDao pdao = configurator.get(LocalPageDao.class, "sql");
        PageViewIterator it = new PageViewIterator(lang, 2013, 12, 8, 1, 2013, 12, 8, 3);
        while(it.hasNext()){
            PageViewDataStruct data = it.next();
            System.out.println(data.getStartDate());
        }


    }



}
