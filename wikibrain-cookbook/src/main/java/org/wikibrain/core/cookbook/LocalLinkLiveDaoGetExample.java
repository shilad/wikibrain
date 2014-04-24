package org.wikibrain.core.cookbook;

import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalLinkDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalLink;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Toby "Jiajun" Li
 */
public class LocalLinkLiveDaoGetExample {
    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
        LocalLinkDao linkDao = new Configurator(new Configuration()).get(LocalLinkDao.class, "live");
        Set<Language> langSet = new HashSet<Language>();
        langSet.add(Language.getByLangCode("en"));
        langSet.add(Language.getByLangCode("fr"));
        langSet.add(Language.getByLangCode("zh"));
        Set<Integer> idSet = new HashSet<Integer>();
        idSet.add(15000);
        idSet.add(14000);
        DaoFilter df = new DaoFilter().setLanguages(langSet).setSourceIds(idSet);
        Language lang = Language.getByLangCode("en");
        for(LocalLink link: linkDao.get(df)){
            System.out.println(link);
        }
        System.out.println(linkDao.getCount(df));
    }
}
