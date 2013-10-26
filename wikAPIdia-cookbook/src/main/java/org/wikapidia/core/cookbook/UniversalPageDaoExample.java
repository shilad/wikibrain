package org.wikapidia.core.cookbook;

import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.model.*;

/**
 */
public class UniversalPageDaoExample {
    public static void main(String args[]) throws ConfigurationException, DaoException {
        Configurator configurator = new Configurator(new Configuration());
        UniversalPageDao pdao = configurator.get(UniversalPageDao.class);
        UniversalLinkDao ldao = configurator.get(UniversalLinkDao.class);
        LocalPageDao lpDao = configurator.get(LocalPageDao.class);

        int i = 0;
        for (UniversalPage page : (Iterable<UniversalPage>)pdao.get(new DaoFilter().setNameSpaces(NameSpace.ARTICLE))) {
            for (LocalId lId : page.getLocalEntities()){
                LocalPage lPage = lpDao.getById(lId.getLanguage(), lId.getId());
                System.out.println(lPage);
            }
            System.out.println();
            i++;
        }
        System.out.println("Concept count: " + i);

    }
}
