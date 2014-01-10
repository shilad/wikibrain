package org.wikapidia.cookbook.core;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.live.LocalLinkLiveDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;

import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: derian
 * Date: 11/3/13
 * Time: 10:03 AM
 * To change this template use File | Settings | File Templates.
 */
public class LocalLinkLiveDaoExample {

    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
        //LocalLinkDao ldao = new Configurator(new Configuration()).get(LocalLinkDao.class, "live");    //try to change from "live" to "dao"
        LocalLinkLiveDao ldao = new LocalLinkLiveDao();
        Language lang = Language.getByLangCode("simple");
        int sourceId = 10983;   //Minnesota
        int destId = 3009;      //California
        LocalLink link = ldao.getLink(lang, sourceId, destId);
        if(link != null)
            System.out.println("Got link \"" + link.getAnchorText() + "\" from " + sourceId + " to " + destId);

        Iterable<LocalLink> inlinks = ldao.getLinks(lang, sourceId, false);
        System.out.println("\nLinks into page " + sourceId + ":");
        for (LocalLink inlink : inlinks) {
            System.out.println(inlink);
        }

        Iterable<LocalLink> outlinks = ldao.getLinks(lang, sourceId, true);
        System.out.println("\nLinks out of page " + sourceId + ":");
        for (LocalLink outlink : outlinks) {
            System.out.println(outlink);
        }

    }

}
