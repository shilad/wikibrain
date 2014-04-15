package org.wikapidia.cookbook.core;

import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.model.*;

/**
 *
 * Prints out all LocalPage titles in each UniversalPage. For instance,
 * if en and es are loaded, the printout might look like this:
 *
 * ...
 * LocalPage{nameSpace=ARTICLE, title=United States, localId=..., language=English}
 * LocalPage{nameSpace=ARTICLE, title=Estados Unidos, localId=..., language=Spanish}
 *
 * LocalPage{nameSpace=ARTICLE, title=Spain, localId=..., language=English}
 * LocalPage{nameSpace=ARTICLE, title=España, localId=..., language=Spanish}
 *
 * ...
 *
 */
public class UniversalPageDaoExample {
    public static void main(String args[]) throws ConfigurationException, DaoException {

        Env env = new EnvBuilder().build();
        Configurator configurator = env.getConfigurator();
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
