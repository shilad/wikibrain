package org.wikibrain.core.model;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import sun.security.krb5.Config;

/**
 * Created by harpa003 on 6/20/14.
 */
public class LocalIDToUniversalID {
    private static LocalPageDao localPageDao;
    private static UniversalPageDao universalPageDao;
    private static int WIKIDATA_CONCEPTS=1;

    public static int translate(int localID, Language language){
        try {
            LocalPage lp = localPageDao.getById(language, localID);
            UniversalPage upage = universalPageDao.getByLocalPage(lp, WIKIDATA_CONCEPTS);
            return upage.getUnivId();
        } catch(Exception e){
            return -1;
        }
    }

    public static void init(Configurator conf) throws ConfigurationException{
        localPageDao = conf.get(LocalPageDao.class);
        universalPageDao= conf.get(UniversalPageDao.class);
    }
}
