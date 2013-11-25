package org.wikapidia.core.cookbook;

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
import java.util.HashSet;
import java.util.Set;

/**
 * An Example shows the difference of results between LocalLinkLiveDao & LocalLinkSqlDao
 * @author Toby "Jiajun" Li
 */

public class CompareLocalLinkLiveSqlDao {

    static long liveInCounter = 0, liveOutCounter = 0, sqlInCounter = 0, sqlOutCounter = 0, hitInCounter = 0, hitOutCounter = 0;
    static Set<Integer> inLive = new HashSet();
    static Set<Integer> outLive = new HashSet();
    static Set<Integer> inCommon = new HashSet();
    static Set<Integer> outCommon = new HashSet();



    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
        LocalLinkDao ldao = new Configurator(new Configuration()).get(LocalLinkDao.class, "live");
        LocalPageDao pdao = new Configurator(new Configuration()).get(LocalPageDao.class, "live");
        Language lang = Language.getByLangCode("simple");
        int pageId = pdao.getIdByTitle("Minnesota", lang, NameSpace.getNameSpaceByArbitraryId(0));


        Iterable<LocalLink> inlinks = ldao.getLinks(lang, pageId, false);
        System.out.println("\nLinks into page " + pageId + ":" + "in Live Wiki");
        for (LocalLink inlink : inlinks) {
            liveInCounter++;
            inLive.add(inlink.getSourceId());
            System.out.println(inlink);
        }

        Iterable<LocalLink> outlinks = ldao.getLinks(lang, pageId, true);
        System.out.println("\nLinks out of page " + pageId + ":" + "in Live Wiki");
        for (LocalLink outlink : outlinks) {
            liveOutCounter++;
            outLive.add(outlink.getDestId());
            System.out.println(outlink);
        }

        ldao = new Configurator(new Configuration()).get(LocalLinkDao.class, "sql");
        pdao = new Configurator(new Configuration()).get(LocalPageDao.class, "sql");
        pageId = pdao.getIdByTitle("Minnesota", lang, NameSpace.getNameSpaceByArbitraryId(0));
        System.out.println(pageId);

        inlinks = ldao.getLinks(lang, pageId, false);
        System.out.println("\nLinks into page " + pageId + ":" + "in static Wiki dump file");
        for (LocalLink inlink : inlinks) {
            sqlInCounter++;
            if(inLive.contains(inlink.getSourceId())){
                inCommon.add(inlink.getSourceId());
            }
            System.out.println(inlink);
        }

        outlinks = ldao.getLinks(lang, pageId, true);
        System.out.println("\nLinks out of page " + pageId + ":" + "in static Wiki dump file");
        for (LocalLink outlink : outlinks) {
            sqlOutCounter++;
            if(outLive.contains(outlink.getDestId())){
                outCommon.add(outlink.getDestId());
            }
            System.out.println(outlink);
        }

        System.out.printf("\nNumber of inlinks in LiveDao: %d\nNumber of inlinks in SQLDao: %d\nNumber of inlinks in common: %d\n\nNumber of outlinks in LiveDao: %d\n" +
                "Number of outlinks in SQLDao: %d\n" +
                "Number of outlinks in common: %d\n", liveInCounter, sqlInCounter, inCommon.size(), liveOutCounter, sqlOutCounter, outCommon.size());


    }

}