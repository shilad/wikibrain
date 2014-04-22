package org.wikapidia.cookbook.core;

import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.UniversalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.phrases.PhraseAnalyzer;

import java.util.LinkedHashMap;

/**
 * @author Shilad Sen
 */
public class ConceptExample {
    public static void main() throws DaoException, ConfigurationException {
        // Prepare the environment; set the root to the current directory (".").
        Env env = new EnvBuilder()
                .setBaseDir(".")
                .build();

        // Get the configurator that creates components and a phraze analyzer from it
        Configurator configurator = env.getConfigurator();
        PhraseAnalyzer pa = configurator.get(PhraseAnalyzer.class);
        UniversalPageDao dao = configurator.get(UniversalPageDao.class);

        // get the most common phrases in simple
        Language simple = Language.getByLangCode("simple");   // simple english
        LinkedHashMap<LocalPage, Float> resolution = pa.resolve(simple, "apple", 5);

        // show the closest pages
        System.out.println("meanings of apple:");
        for (LocalPage p : resolution.keySet()) {
            System.out.println("\t" + p + ": " + resolution.get(p));

            // translate them...
            UniversalPage concept = dao.getByLocalPage(p, 1);
            for (LocalId id : concept.getLocalEntities()) {
                System.out.println("\t\tin language " + id.getLanguage() + " is " + id);
            }
        }
    }
}
