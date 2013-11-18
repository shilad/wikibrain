package org.wikapidia.cookbook.core;

import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.model.NameSpace;

import java.io.IOException;

/**
 * An Example shows the difference between LocalLinkLiveDao & LocalLinkSqlDao
 * @author Toby "Jiajun" Li
 */

public class CompareLocalLinkLiveSqlDaoExample {

    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
        LocalLinkDao ldao = new Configurator(new Configuration()).get(LocalLinkDao.class, "live");
        LocalPageDao pdao = new Configurator(new Configuration()).get(LocalPageDao.class, "live");
        Language lang = Language.getByLangCode("simple");
        int pageId = pdao.getIdByTitle("Minnesota", lang, NameSpace.getNameSpaceByArbitraryId(0));


        Iterable<LocalLink> inlinks = ldao.getLinks(lang, pageId, false);
        System.out.println("\nLinks into page " + pageId + ":" + "in Live Wiki");
        for (LocalLink inlink : inlinks) {
            System.out.println("\t" + inlink.getAnchorText() + ", " + inlink.getDestId());
        }

        Iterable<LocalLink> outlinks = ldao.getLinks(lang, pageId, true);
        System.out.println("\nLinks out of page " + pageId + ":" + "in Live Wiki");
        for (LocalLink outlink : outlinks) {
            System.out.println("\t" + outlink.getAnchorText() + ", " + outlink.getDestId());
        }

        ldao = new Configurator(new Configuration()).get(LocalLinkDao.class, "sql");
        pageId = pdao.getIdByTitle("Minnesota", lang, NameSpace.getNameSpaceByArbitraryId(0));


        inlinks = ldao.getLinks(lang, pageId, false);
        System.out.println("\nLinks into page " + pageId + ":" + "in static Wiki dump file");
        for (LocalLink inlink : inlinks) {
            System.out.println("\t" + inlink.getAnchorText() + ", " + inlink.getDestId());
        }

        outlinks = ldao.getLinks(lang, pageId, true);
        System.out.println("\nLinks out of page " + pageId + ":" + "in static Wiki dump file");
        for (LocalLink outlink : outlinks) {
            System.out.println("\t" + outlink.getAnchorText() + ", " + outlink.getDestId());
        }
    }

}
