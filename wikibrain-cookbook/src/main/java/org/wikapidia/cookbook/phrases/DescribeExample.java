package org.wikapidia.cookbook.phrases;

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
import org.wikapidia.phrases.PhraseAnalyzer;

import java.io.IOException;
import java.util.LinkedHashMap;

/**
 * @author Shilad Sen
 */
public class DescribeExample {
    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {

        Env env = new EnvBuilder().build();
        Configurator c = env.getConfigurator();
        Language lang = Language.getByLangCode("simple");   // simple english
        PhraseAnalyzer pa = c.get(PhraseAnalyzer.class, "stanford");
        LocalPageDao pageDao = c.get(LocalPageDao.class);
        LocalPage page = pageDao.getByTitle(new Title("Obama", lang), NameSpace.ARTICLE);
        System.out.println("description of " + page + ":"); // should resolve redirect to Barack Obama
        LinkedHashMap<String, Float> description = pa.describe(lang, page, 20);
        if (description == null) {
            System.out.println("\tno description!");
        } else {
            for (String phrase : description.keySet()) {
                System.out.println("\t" + phrase + ": " + description.get(phrase));
            }
        }
    }
}
