package org.wikapidia.cookbook.sr;

import org.apache.commons.lang3.StringUtils;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.UniversalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.sr.*;
import org.wikapidia.sr.utils.ExplanationFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Shilad Sen
 */
public class SimilarityExample {

    public static void main(String args[]) throws ConfigurationException, DaoException {
        // Initialize the WikAPIdia environment and get the local page dao
        Env env = new EnvBuilder().build();
        Configurator conf = env.getConfigurator();
        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        Language simple = Language.getByLangCode("simple");

        // Retrieve the "ensemble" sr metric for simple english
        MonolingualSRMetric sr = conf.get(
                MonolingualSRMetric.class, "ensemble",
                "language", simple.getLangCode());

        //Similarity between strings
        String pairs[][] = new String[][] {
                { "cat", "kitty" },
                { "obama", "president" },
                { "tires", "car" },
                { "java", "computer" },
                { "dog", "computer" },
        };

        for (String pair[] : pairs) {
            SRResult s = sr.similarity(pair[0], pair[1], false);
            System.out.println(s.getScore() + ": '" + pair[0] + "', '" + pair[1] + "'");
        }
    }
}
