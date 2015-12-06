package org.wikibrain.cookbook.core;

import gnu.trove.map.TIntDoubleMap;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.LocalCategoryMemberDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.utils.WpCollectionUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * @author Shilad Sen
 */
public class CategoryExample {
    public static void main(String args[]) throws Exception {
        // Prepare the environment
        Env env = EnvBuilder.envFromArgs(args);
        Language lang = env.getDefaultLanguage();

        // Get the configurator that creates components
        Configurator configurator = env.getConfigurator();
        LocalCategoryMemberDao catDao = configurator.get(LocalCategoryMemberDao.class);
        LocalPageDao pageDao = configurator.get(LocalPageDao.class);

        // Get top-level categories
        Set<LocalPage> topLevelCats = catDao.guessTopLevelCategories(lang);
        System.out.println("Top level categories are:");
        for (LocalPage p : topLevelCats) {
            System.out.println("\t" + p);
        }

        // For each article, find the category closest to it.
        long t1 = System.currentTimeMillis();
        Map<LocalPage, TIntDoubleMap> result = catDao.getClosestCategories(topLevelCats);
        long t2 = System.currentTimeMillis();
        System.out.println("\nClosest categories identified in " + (t2 - t1) + "ms");

        for (LocalPage c : result.keySet()) {
            System.out.println("\nExamples of pages closest to " + c);
            int pageIds[] = Arrays.copyOf(result.get(c).keys(), 5);
            for (int id : pageIds) {
                LocalPage p = pageDao.getById(lang, id);
                if (p != null) System.out.format("\t%.3f %s\n", result.get(c).get(id), p.toString());
            }
        }

        // Find closest categories for an article
        LocalPage p = pageDao.getByTitle(lang, "Jesus");
        TIntDoubleMap distances = catDao.getCategoryDistances(topLevelCats, p.getLocalId(), true);
        System.out.println("distances to top-level categories for " + p);
        for (int catId : WpCollectionUtils.sortMapKeys(distances, false)) {
            LocalPage c = pageDao.getById(lang, catId);
            if (c != null) System.out.format("\t%.3f %s\n", distances.get(catId), c.toString());

        }
    }
}
