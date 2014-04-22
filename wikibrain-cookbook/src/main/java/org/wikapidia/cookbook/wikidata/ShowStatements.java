package org.wikapidia.cookbook.wikidata;

import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;
import org.wikapidia.wikidata.LocalWikidataStatement;
import org.wikapidia.wikidata.WikidataDao;

import java.util.List;
import java.util.Map;

/**
 * @author Shilad Sen
 */
public class ShowStatements {
    public static void main(String args[]) throws ConfigurationException, DaoException {
        Env env = new EnvBuilder().build();
        Configurator conf = env.getConfigurator();
        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        WikidataDao wdDao = conf.get(WikidataDao.class);
        Language simple = Language.getByLangCode("simple");

        // Get Barack Obama's page
        Title title = new Title("Barack Obama", simple);
        LocalPage obama = lpDao.getByTitle(title, NameSpace.ARTICLE);
        Map<String, List<LocalWikidataStatement>> statements = wdDao.getLocalStatements(obama);
        System.out.println("Properties for " + title);
        for (String property : statements.keySet()) {
            System.out.println("values for property " + property + " are:");
            for (LocalWikidataStatement lws : statements.get(property)) {
                System.out.println("\t" + lws.getFullStatement());
            }
        }
    }
}
