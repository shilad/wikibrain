package org.wikapidia.core.dao;

import gnu.trove.set.TIntSet;
import org.junit.Ignore;
import org.junit.Test;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;

/**
 * @author Ari Weiland
 */
public class TestDaoFilter {

    @Ignore
    @Test
    public void test() throws ConfigurationException, DaoException {
        Configurator conf = new Configurator(new Configuration());
        LocalPageDao dao = conf.get(LocalPageDao.class);
        RedirectDao redirectDao = conf.get(RedirectDao.class);
        DaoFilter filter = new DaoFilter()
                .setNameSpaces(NameSpace.ARTICLE);
        Iterable<LocalPage> iterable = dao.get(filter);
        System.out.println(filter.getNameSpaceIds());
        for (LocalPage page : iterable) {
            if (page != null && page.getNameSpace() != NameSpace.ARTICLE) {
                TIntSet set = redirectDao.getRedirects(page);
                System.out.println(page + " : " + set);
            }
        }
    }
}
