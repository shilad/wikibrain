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
        Env env = new EnvBuilder().build();
        //Env env = EnvBuilder.envFromArgs(args); //this is just something that sam and ben said to do.....
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
