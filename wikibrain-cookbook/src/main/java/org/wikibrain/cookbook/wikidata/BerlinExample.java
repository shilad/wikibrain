package org.wikibrain.cookbook.wikidata;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.Title;
import org.wikibrain.wikidata.LocalWikidataStatement;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataStatement;
import org.wikibrain.wikidata.WikidataValue;

import java.util.List;
import java.util.Map;

/**
 * @author Shilad Sen
 */
public class BerlinExample {
    public static void main(String args[]) throws ConfigurationException, DaoException {
        Env env = EnvBuilder.envFromArgs(args);
        Configurator conf = env.getConfigurator();
        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        WikidataDao wdDao = conf.get(WikidataDao.class);
        UniversalPageDao univDao = conf.get(UniversalPageDao.class);
        Language lang = env.getDefaultLanguage();

        // Show statements for berlin
        Title title = new Title("Berlin", lang);
        LocalPage page = lpDao.getByTitle(title, NameSpace.ARTICLE);
        Map<String, List<LocalWikidataStatement>> statements = wdDao.getLocalStatements(page);
        System.out.println("Properties for " + title);
        for (String property : statements.keySet()) {
            System.out.println("values for property " + property + " are:");
            for (LocalWikidataStatement lws : statements.get(property)) {
                System.out.println("\t" + lws.getFullStatement());
            }
        }

        // Print out who was born in Berlin
        WikidataValue berlinEntity = WikidataValue.forItem(univDao.getUnivPageId(page));
        for (WikidataStatement st : wdDao.getByValue("place of birth", berlinEntity)) {
            System.out.println(wdDao.getLocalStatement(lang, st).getFullStatement());
        }
    }
}
