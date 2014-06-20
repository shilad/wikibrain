package org.wikibrain.cookbook;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.phrases.PhraseAnalyzer;

import java.io.IOException;
import java.util.LinkedHashMap;

/**
 * @author Shilad Sen
 */
public class Quickstart {
    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {

        // Prepare the environment
        Env env = EnvBuilder.envFromArgs(args);

        // Get the configurator that creates components and a phraze analyzer from it
        Configurator configurator = env.getConfigurator();
        PhraseAnalyzer pa = configurator.get(PhraseAnalyzer.class, "anchortext");

        // get the most common phrases in simple
        LinkedHashMap<LocalId, Float> resolution = pa.resolve(Language.SIMPLE, "Apple", 20);

        // show the closest pages
        System.out.println("resolution of apple");
        if (resolution == null) {
            System.out.println("\tno resolution !");
        } else {
            for (LocalId p : resolution.keySet()) {
                System.out.println("\t" + p + ": " + resolution.get(p));
            }
        }
    }
}













