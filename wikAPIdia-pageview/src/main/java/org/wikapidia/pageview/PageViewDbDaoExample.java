package org.wikapidia.pageview;

import org.joda.time.DateTime;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;

/**
 * @author Toby "Jiajun" Li
 */
public class PageViewDbDaoExample {

    public static void main(String args[]) throws ConfigurationException, DaoException, WikapidiaException {


        Language lang = Language.getByLangCode("simple");
        PageViewDbDao pageViewDbDao = new PageViewDbDao(lang);
        Env env = new EnvBuilder().build();
        Configurator configurator = env.getConfigurator();
        LocalPageDao pDao = configurator.get(LocalPageDao.class, "live");
        System.out.println(pageViewDbDao.getPageView(47, new DateTime(2013, 12, 8, 0, 0), new DateTime(2013, 12, 9, 0, 0)));
        System.out.println(pageViewDbDao.getPageView(56, new DateTime(2013, 12, 8, 0, 0), new DateTime(2013, 12, 9, 0, 0)));


    }
}
