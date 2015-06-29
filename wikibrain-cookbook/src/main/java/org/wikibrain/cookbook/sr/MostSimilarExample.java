package org.wikibrain.cookbook.sr;

import org.apache.commons.lang3.StringUtils;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.sr.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Matt Lesicko
 */
public class MostSimilarExample {
    public static void main(String[] args) throws Exception{
        // Initialize the WikiBrain environment and get the local page dao
        Env env = EnvBuilder.envFromArgs(args);
        Configurator conf = env.getConfigurator();
        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        Language simple = Language.getByLangCode("simple");

        // Retrieve the "milnewitten" sr metric for simple english
        SRMetric sr = conf.get(
                SRMetric.class, "simple-ensemble",
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
