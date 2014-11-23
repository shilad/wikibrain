package org.wikibrain.cookbook.pageview;

import gnu.trove.map.TIntIntMap;
import org.joda.time.DateTime;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalCategoryMemberDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.pageview.PageViewDao;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Shilad Sen
 */
public class CategoryViews {
    public static String TOP_LEVEL_PARENT = "Category:Main_topic_classifications";

    public static void main(String args[]) throws ConfigurationException, DaoException {

        // Get the pageview dao
        Env env = EnvBuilder.envFromArgs(args);
        Language lang = env.getDefaultLanguage();
        final PageViewDao viewDao = env.getConfigurator().get(PageViewDao.class);
        final LocalCategoryMemberDao catDao = env.getConfigurator().get(LocalCategoryMemberDao.class);
        LocalPageDao pageDao = env.getConfigurator().get(LocalPageDao.class);

        // Download and import pageview stats if necessary.
        DateTime start = new DateTime(2014, 8, 14, 11, 0, 0);
        DateTime end = new DateTime(2014, 8, 14, 23, 0, 0);
        viewDao.ensureLoaded(start, end,  env.getLanguages());

        // Build up set of top level categories
        final Set<LocalPage> topLevelCategories = new HashSet<LocalPage>();
        LocalPage parent = pageDao.getByTitle(lang, NameSpace.CATEGORY, TOP_LEVEL_PARENT);
        for (LocalPage page : catDao.getCategoryMembers(parent).values()) {
            if (page.getNameSpace().equals(NameSpace.CATEGORY)) {
                topLevelCategories.add(page);
            }
        }

        // Map from page id -> num views
        final TIntIntMap allViews = viewDao.getAllViews(lang, start, end);

        final Map<LocalPage, Integer> articleCounts = new HashMap<LocalPage, Integer>();
        final Map<LocalPage, Integer> viewCounts = new HashMap<LocalPage, Integer>();
        final AtomicInteger numPages = new AtomicInteger();

        // Build up accumulators for each category by looping over pages in parallel
        ParallelForEach.iterate(
            pageDao.get(DaoFilter.normalPageFilter(lang)).iterator(),
            new Procedure<LocalPage>() {
                @Override
                public void call(LocalPage page) throws Exception {
                    int views = allViews.get(page.getLocalId());
                    LocalPage cat = catDao.getClosestCategory(page, topLevelCategories, true);
                    if (cat != null) {
                        if (articleCounts.containsKey(cat)) {
                            articleCounts.put(cat, articleCounts.get(cat) + 1);
                            viewCounts.put(cat, viewCounts.get(cat) + views);
                        } else {
                            articleCounts.put(cat, 1);
                            viewCounts.put(cat, views);
                        }
                        if (numPages.incrementAndGet() % 10000 == 0) {
                            System.err.println("doing page " + numPages.get());
                        }
                    }
                }
            });

        for (LocalPage page : viewCounts.keySet()) {
            System.out.format("%s\t%d\t%d\n", page.getTitle().getCanonicalTitle(), articleCounts.get(page), viewCounts.get(page) );
        }
    }
}
