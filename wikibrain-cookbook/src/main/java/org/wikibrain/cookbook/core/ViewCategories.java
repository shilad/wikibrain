package org.wikibrain.cookbook.core;

import org.joda.time.DateTime;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.Title;
import org.wikibrain.pageview.PageViewSqlDao;
import org.wikibrain.utils.WpCollectionUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
* @author Shilad Sen
*/
public class ViewCategories {
    public static String[] TOP_LEVEL_PARENTS = {
            "Category:Articles",
            "Category:Everyday life",
    };

    public static String[] TOP_LEVEL_IGNORE = {
            "Category:Wikipedia articles by source",
            "Category:Former good articles",
            "Category:Former very good articles",
    };

    public static void main(String args[]) throws ConfigurationException, DaoException {
        Env env = EnvBuilder.envFromArgs(args);

        Configurator configurator = env.getConfigurator();
        PageViewSqlDao viewDao = configurator.get(PageViewSqlDao.class);
        LocalPageDao pageDao = configurator.get(LocalPageDao.class);
        LocalCategoryMemberDao memberDao = configurator.get(LocalCategoryMemberDao.class);

        Set<LocalPage> topLevelCategories = new HashSet<LocalPage>();
        for (String title : TOP_LEVEL_PARENTS) {
            LocalPage c = pageDao.getByTitle(Language.SIMPLE, NameSpace.CATEGORY, title);
            for (LocalPage page : memberDao.getCategoryMembers(c).values()) {
                if (page.getNameSpace().equals(NameSpace.CATEGORY)) {
                    topLevelCategories.add(page);
                }
            }
        }

        for (String title : TOP_LEVEL_IGNORE) {
            LocalPage c = pageDao.getByTitle(Language.SIMPLE, NameSpace.CATEGORY, title);
            topLevelCategories.remove(c);
        }

        System.err.println("top level categories are " + topLevelCategories);

        DaoFilter filter = new DaoFilter()
                .setDisambig(false)
                .setRedirect(false)
                .setNameSpaces(NameSpace.ARTICLE);

        int i = 0;
        Map<LocalPage, Integer> counts = new HashMap<LocalPage, Integer>();
        for (LocalPage page : pageDao.get(filter)) {
            int views = viewDao.getNumViews(Language.SIMPLE, page.getLocalId(), new DateTime(2014, 1, 1, 1, 1), 24, pageDao);
            LocalPage cat = memberDao.getClosestCategory(page, topLevelCategories, true);
            if (cat != null) {
                if (counts.containsKey(cat)) {
                    counts.put(cat, counts.get(cat) + 1);
                } else {
                    counts.put(cat, 1);
                }
                if (i++ % 1000 == 0) {
                    System.err.println("doing page " + i);
                }
            }
        }

        for (LocalPage cat : WpCollectionUtils.sortMapKeys(counts, true)) {
            System.out.println(cat.getTitle() + ": " + counts.get(cat));
        }
    }
}
