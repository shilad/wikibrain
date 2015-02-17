package org.wikibrain.core.cookbook;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.set.TIntSet;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.RedirectDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;

import java.io.IOException;

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
        pdao.setFollowRedirects(false);
        Language lang = Language.getByLangCode("simple");
        int redirectId = 217416; //621375; //Shaq
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

        double start = System.currentTimeMillis();
        try {
            //following line takes 164.3 min (2 hours 44 min) for simple
            //takes about 43 min to throw OutOfMemory error for english
            TIntIntMap allRedirectIdsToDestIds = rdao.getAllRedirectIdsToDestIds(lang);
            TIntIntMap redirectMap = allRedirectIdsToDestIds;
            double elapsed = (System.currentTimeMillis() - start) / 1000.0;
            System.out.println("Retrieved all redirects in " + lang.getLangCode() + " in " + elapsed + " seconds");
            int redirectCount = 0;
            System.out.println("\nNumber of redirects: " + redirectMap.size());
            System.out.println("\nFirst 500 redirects in language " + lang.getLangCode() + ":");
            for (int sourceId : redirectMap.keys()) {
                if (redirectCount >= 500) {break;}
                LocalPage sourcePage = pdao.getById(lang, sourceId);
                LocalPage destPage = pdao.getById(lang, redirectMap.get(sourceId));
                System.out.println("\tFrom \"" + sourcePage.getTitle() + "\" to \"" + destPage.getTitle() + "\"");
                redirectCount++;
            }
        }
        catch (OutOfMemoryError e) {
            System.out.println((System.currentTimeMillis() - start) / 1000.0);
        }
    }
}
