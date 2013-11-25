package org.wikapidia.core.cookbook;

import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalLink;

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
