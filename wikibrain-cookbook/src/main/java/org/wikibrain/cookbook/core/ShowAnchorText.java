package org.wikibrain.cookbook.core;

import org.apache.commons.cli.*;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.conf.DefaultOptionBuilder;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.Title;

import java.io.IOException;
import java.sql.SQLException;

/**
 * All cookbook examples should include comments. Replace this with a real one.
 *
 * @author Shilad Sen
 */
public class ShowAnchorText {

    public static void main(String args[]) throws ConfigurationException, DaoException {
        // The following ten-line dance to get an env is awkward and repeated over and over.
        // Figure out a good way to consolidate it.
        Env env = EnvBuilder.envFromArgs(args);

        Configurator configurator = env.getConfigurator();
        LocalPageDao lpDao = configurator.get(LocalPageDao.class);
        LocalLinkDao sqlDao = configurator.get(LocalLinkDao.class, "sql");
        Language simple = env.getLanguages().getDefaultLanguage();

        LocalPage page = lpDao.getByTitle(new Title("List of Soundgarden band members", simple), NameSpace.ARTICLE);
        System.out.println("page is " + page);
        DaoFilter filter = new DaoFilter().setSourceIds(page.getLocalId()).setLanguages(simple);
        for (LocalLink link : sqlDao.get(filter)) {
            System.out.println("link is: " + link);
        }
    }
}
