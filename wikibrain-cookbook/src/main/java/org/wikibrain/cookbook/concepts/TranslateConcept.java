package org.wikibrain.cookbook.concepts;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.UniversalPage;

/**
 * @author Shilad Sen
 */
public class TranslateConcept {

    public static void main(String args[]) throws ConfigurationException, DaoException {

        // Setup environment
        Env env = EnvBuilder.envFromArgs(args);
        LocalPageDao pageDao = env.getConfigurator().get(LocalPageDao.class);
        UniversalPageDao conceptDao = env.getConfigurator().get(UniversalPageDao.class);

        // Get local and universal pages
        LocalPage page = pageDao.getByTitle(Language.EN, "Apple");
        UniversalPage concept = conceptDao.getByLocalPage(page);

        // Translate to other languages.
        System.out.format("%s in other languages:\n", page.getTitle());
        for (Language lang : concept.getLanguageSet()) {
            LocalPage page2 = pageDao.getById(lang, concept.getLocalId(lang));
            System.out.format("%s: %s\n", lang.toString(), page2.getTitle().getCanonicalTitle());
        }
    }

}
