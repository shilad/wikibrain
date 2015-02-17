package org.wikibrain.cookbook.wikidata;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.Title;
import org.wikibrain.wikidata.LocalWikidataStatement;
import org.wikibrain.wikidata.WikidataDao;

import java.util.List;
import java.util.Map;

/**
 * @author Shilad Sen
 */
public class ShowStatements {
    public static void main(String args[]) throws ConfigurationException, DaoException {
        Env env = EnvBuilder.envFromArgs(args);
        Configurator conf = env.getConfigurator();
        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        WikidataDao wdDao = conf.get(WikidataDao.class);

        // Get page
        Title title = new Title("Berlin", env.getDefaultLanguage());
        LocalPage page = lpDao.getByTitle(title, NameSpace.ARTICLE);
        System.out.println("Properties for " + title);

        // Show statements
        Map<String, List<LocalWikidataStatement>> statements = wdDao.getLocalStatements(page);
        for (String property : statements.keySet()) {
            System.out.println("values for property " + property + " are:");
            for (LocalWikidataStatement lws : statements.get(property)) {
                System.out.println("\t" + lws.getFullStatement());
            }
        }
    }
}
