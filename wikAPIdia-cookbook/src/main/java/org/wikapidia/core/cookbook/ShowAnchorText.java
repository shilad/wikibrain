package org.wikapidia.core.cookbook;

import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;

/**
 * @author Shilad Sen
 */
public class ShowAnchorText {

    public static void main(String args[]) throws ConfigurationException, DaoException {
        Env env = new EnvBuilder().build();
        Configurator configurator = env.getConfigurator();
        LocalPageDao lpDao = configurator.get(LocalPageDao.class);
        LocalLinkDao sqlDao = configurator.get(LocalLinkDao.class, "sql");
        Language simple = Language.getByLangCode("simple");
        int id = lpDao.getIdByTitle(new Title("List of Soundgarden band members", simple));
        LocalPage page = lpDao.getByTitle(new Title("List of Soundgarden band members", simple), NameSpace.ARTICLE);
        System.out.println("page is " + page);
        DaoFilter filter = new DaoFilter().setSourceIds(page.getLocalId()).setLanguages(simple);
        for (LocalLink link : sqlDao.get(filter)) {
            System.out.println("link is: " + link);
        }
    }
}
