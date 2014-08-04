//package org.wikibrain.cookbook.core;
//
//import org.wikibrain.conf.ConfigurationException;
//import org.wikibrain.conf.Configurator;
//import org.wikibrain.core.cmd.Env;
//import org.wikibrain.core.cmd.EnvBuilder;
//import org.wikibrain.core.dao.*;
//import org.wikibrain.core.lang.Language;
//import org.wikibrain.core.model.LocalCategory;
//import org.wikibrain.core.model.LocalPage;
//import org.wikibrain.core.model.NameSpace;
//import org.wikibrain.core.model.Title;
//
//import java.util.HashSet;
//import java.util.Set;
//
///**
// * @author Shilad Sen
// */
//public class ViewCategories {
//    public static String[] TOP_LEVEL_PARENTS = {
//            "Category:Articles",
//            "Category:Everyday life",
//    };
//
//    public static String[] TOP_LEVEL_IGNORE = {
//            "Category:Wikipedia articles by source",
//            "Category:Former good articles",
//            "Category:Former very good articles",
//    };
//
//    public static void main(String args[]) throws ConfigurationException, DaoException {
//        Env env = EnvBuilder.envFromArgs(args);
//
//        Configurator configurator = env.getConfigurator();
//        LocalPageDao pageDao = configurator.get(LocalPageDao.class);
//        LocalCategoryMemberDao memberDao = configurator.get(LocalCategoryMemberDao.class);
//
//        Set<LocalCategory> topLevelCategories = new HashSet<LocalCategory>();
//        for (String title : TOP_LEVEL_PARENTS) {
//            LocalCategory c = categoryDao.getByTitle(Language.SIMPLE, title);
//            for (LocalPage page : memberDao.getCategoryMembers(c).values()) {
//                if (page.getNameSpace().equals(NameSpace.CATEGORY)) {
//                    topLevelCategories.add(((LocalCategory) page));
//                }
//            }
//        }
//
//        for (String title : TOP_LEVEL_IGNORE) {
//            LocalCategory c = categoryDao.getByTitle(Language.SIMPLE, title);
//            topLevelCategories.remove(c);
//        }
//
//        System.err.println("top level categories are " + topLevelCategories);
//
//        DaoFilter filter = new DaoFilter()
//                .setDisambig(false)
//                .setRedirect(false)
//                .setNameSpaces(NameSpace.ARTICLE);
//
//        for (LocalPage page : articleDao.get(filter)) {
//            LocalCategory category = memberDao.getClosestCategory(page, topLevelCategories, true);
//            System.out.println("page " + page + " is in " + category);
//        }
//    }
//}
