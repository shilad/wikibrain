package org.wikapidia.core.cookbook;

import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalLink;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: derian
 * Date: 11/3/13
 * Time: 10:03 AM
 * To change this template use File | Settings | File Templates.
 */
public class LocalLinkLiveDaoExample {

    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
        LocalLinkDao ldao = new Configurator(new Configuration()).get(LocalLinkDao.class, "live");
        Language lang = Language.getByLangCode("en");
        int sourceId = 5079506;
        int destId = 454136;
        LocalLink link = ldao.getLink(lang, sourceId, destId);
        System.out.println("Got link \"" + link.getAnchorText() + "\" from " + sourceId + " to " + destId);

        Iterable<LocalLink> inlinks = ldao.getLinks(lang, sourceId, false);
        System.out.println("\nLinks into page " + sourceId + ":");
        for (LocalLink inlink : inlinks) {
            System.out.println("\t" + inlink.getAnchorText() + ", " + inlink.getDestId());
        }

        Iterable<LocalLink> outlinks = ldao.getLinks(lang, sourceId, true);
        System.out.println("\nLinks out of page " + sourceId + ":");
        for (LocalLink outlink : outlinks) {
            System.out.println("\t" + outlink.getAnchorText() + ", " + outlink.getDestId());
        }
    }

}
