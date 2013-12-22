package org.wikapidia.cookbook.pageview;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.live.LocalPageLiveDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.Title;
import org.wikapidia.pageview.PageViewDataStruct;
import org.wikapidia.pageview.PageViewIterator;

/**
 * Created with IntelliJ IDEA.
 * User: derian
 * Date: 12/9/13
 * Time: 11:33 AM
 * To change this template use File | Settings | File Templates.
 */
public class IteratorTest {

    //get page view stats for simple for two hours, one hour at a time
    public static void main(String[] args) throws WikapidiaException, DaoException, ConfigurationException {
        Language lang = Language.getByLangCode("simple");
        Env env = new EnvBuilder().build();
        Configurator configurator = env.getConfigurator();
        LocalPageDao pdao = configurator.get(LocalPageDao.class, "sql");
        PageViewIterator it = new PageViewIterator(lang, 2013, 12, 8, 1, 2013, 12, 8, 5);

        int i = 0;
        while (it.hasNext()) {
            //see how long it takes to get page view stats for one hour
            double start = System.currentTimeMillis();
            PageViewDataStruct data = it.next();
            double elapsed = (System.currentTimeMillis() - start) / 1000;
            System.out.println("Took " + elapsed + " seconds to parse page view data for hour " + i);
            TIntIntMap stats = data.getPageViewStats();
            for (int pageId : stats.keys()) {
                try{
                    Title page = pdao.getById(lang, pageId).getTitle();
                    System.out.println("\tPage: " + page + "; Views: " + stats.get(pageId));
                }
                catch(NullPointerException e){
                    System.out.println("Message: " + e.getMessage());
                    e.printStackTrace();
                    continue;
                }
            }
            i++;
        }

    }
}
