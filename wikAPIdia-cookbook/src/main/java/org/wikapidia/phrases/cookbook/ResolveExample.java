package org.wikapidia.phrases.cookbook;

import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;
import org.wikapidia.phrases.PhraseAnalyzer;

import java.io.IOException;
import java.util.LinkedHashMap;

/**
 * @author Shilad Sen
 */
public class ResolveExample {
    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
        Language lang = Language.getByLangCode("simple");   // simple english
        Configurator c = new Configurator(new Configuration());
        PhraseAnalyzer pa = c.get(PhraseAnalyzer.class, "anchortext");
        LinkedHashMap<LocalPage, Float> resolution = pa.resolveLocal(lang, "apple", 20);
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
