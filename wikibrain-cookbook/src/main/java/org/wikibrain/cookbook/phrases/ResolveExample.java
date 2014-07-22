package org.wikibrain.cookbook.phrases;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.phrases.PhraseAnalyzer;

import java.io.IOException;
import java.util.LinkedHashMap;

/**
 * @author Shilad Sen
 */
public class ResolveExample {
    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {

        Env env = EnvBuilder.envFromArgs(args);

        // Get the configurator that creates components and a phraze analyzer from it
        Configurator configurator = env.getConfigurator();
        PhraseAnalyzer pa = configurator.get(PhraseAnalyzer.class, "anchortext");
        LocalPageDao pd = configurator.get(LocalPageDao.class);

        // get the most common phrases in simple
        LinkedHashMap<LocalId, Float> resolution = pa.resolve(Language.EN, "Georgia", 100);

        // show the closest pages
        System.out.println("resolution of apple");
        if (resolution == null) {
            System.out.println("\tno resolution !");
        } else {
            for (LocalId p : resolution.keySet()) {
                LocalPage lp = pd.getById(p);
                System.out.println("\t" + lp + ": " + resolution.get(p));
            }
        }
    }
}













