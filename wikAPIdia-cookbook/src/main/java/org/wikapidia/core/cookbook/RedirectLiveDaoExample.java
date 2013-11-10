package org.wikapidia.core.cookbook;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.set.TIntSet;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.RedirectDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.model.LocalPage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: derian
 * Date: 11/10/13
 * Time: 12:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class RedirectLiveDaoExample {
    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
        RedirectDao rdao = new Configurator(new Configuration()).get(RedirectDao.class, "live");
        LocalPageDao pdao = new Configurator(new Configuration()).get(LocalPageDao.class, "live");
        Language lang = Language.getByLangCode("en");
        int redirectId = 621375; //Shaq
        int destId = rdao.resolveRedirect(lang, redirectId);

        assert(rdao.isRedirect(lang, redirectId));
        assert(!rdao.isRedirect(lang, destId));

        LocalPage shaquilleONeal = pdao.getById(lang, destId);
        TIntSet redirectsToShaq = rdao.getRedirects(shaquilleONeal);
        System.out.println("Redirects to Shaquille O'Neal:");
        for (TIntIterator i = redirectsToShaq.iterator(); i.hasNext();) {
            LocalPage redirPage = pdao.getById(lang, i.next());
            System.out.println("\t" + redirPage.getTitle());
        }

        TIntIntMap redirectMap = rdao.getAllRedirectIdsToDestIds(lang);
        int redirectCount = 0;
        System.out.println("\n" + redirectMap.size());
        System.out.println("\nFirst 500 redirects in language " + lang.getLangCode() + ":");
        for (int sourceId : redirectMap.keys()) {
            if (redirectCount >= 500) {break;}
            LocalPage sourcePage = pdao.getById(lang, sourceId);
            LocalPage destPage = pdao.getById(lang, redirectMap.get(sourceId));
            System.out.println("\tFrom \"" + sourcePage.getTitle() + "\" to \"" + destPage.getTitle() + "\"");
            redirectCount++;
        }
    }
}
