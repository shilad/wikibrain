package org.wikapidia.cookbook.phrases;

import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.phrases.PhraseAnalyzer;

import java.io.IOException;
import java.util.LinkedHashMap;

/**
 * @author Shilad Sen
 */
public class ResolveExample {
    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {

        Env env = new EnvBuilder().build();

        // Get the configurator that creates components and a phraze analyzer from it
        Configurator configurator = env.getConfigurator();
        PhraseAnalyzer pa = configurator.get(PhraseAnalyzer.class, "anchortext");

        // get the most common phrases in simple
        Language simple = Language.getByLangCode("simple");   // simple english
        LinkedHashMap<LocalPage, Float> resolution = pa.resolve(simple, "Apple", 20);

        // show the closest pages
        System.out.println("resolution of apple");
        if (resolution == null) {
            System.out.println("\tno resolution !");
        } else {
            for (LocalPage p : resolution.keySet()) {
                System.out.println("\t" + p + ": " + resolution.get(p));
            }
        }
    }
}













