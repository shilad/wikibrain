package org.wikibrain.cookbook.core;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.phrases.PhraseAnalyzer;

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
        LinkedHashMap<LocalId, Float> resolution = pa.resolve(simple, "apple", 5);

        // show the closest pages
        System.out.println("meanings of apple:");
        for (LocalId p : resolution.keySet()) {
            System.out.println("\t" + p + ": " + resolution.get(p));

            // translate them...
            UniversalPage concept = dao.getByLocalPage(p.asLocalPage());
            //UniversalPage concept = dao.getByLocalPage(new Local, 1);
            for (LocalId id : concept.getLocalEntities()) {
                System.out.println("\t\tin language " + id.getLanguage() + " is " + id);
            }
        }
    }
}
