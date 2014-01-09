package org.wikapidia.cookbook.sr;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.DefaultOptionBuilder;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.sr.*;
import org.wikapidia.sr.utils.ExplanationFormatter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Matt Lesicko
 */
public class MostSimilarExample {
    public static void main(String[] args) throws Exception{
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
        for (String phrase : Arrays.asList("Barack Obama", "US", "Canada", "vim")) {
            SRResultList similar = sr.mostSimilar(phrase, 3);
            List<String> pages = new ArrayList<String>();
            for (int i = 0; i < similar.numDocs(); i++) {
                LocalPage page = lpDao.getById(simple, similar.getId(i));
                pages.add((i+1) + ") " + page.getTitle());
            }
            System.out.println("'" + phrase + "' is similar to " + StringUtils.join(pages, ", "));
        }
    }
}
