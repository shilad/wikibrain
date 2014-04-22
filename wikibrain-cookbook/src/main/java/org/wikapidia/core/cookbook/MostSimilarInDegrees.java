package org.wikapidia.core.cookbook;

import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.sr.MonolingualSRMetric;
import org.wikapidia.wikidata.LocalWikidataStatement;
import org.wikapidia.wikidata.WikidataDao;
import org.wikapidia.wikidata.WikidataEntity;
import org.wikapidia.wikidata.WikidataStatement;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.SynchronousQueue;

/**
 * An example that finds the most similar article in certain degrees
 * @author aaron jiang
 */
public class MostSimilarInDegrees {

    public MostSimilarInDegrees(String lang) throws ConfigurationException, DaoException, IOException {
        Env env = new EnvBuilder().build();
        Configurator conf = env.getConfigurator();
        this.pDao = conf.get(LocalPageDao.class, "sql");
        this.lDao = conf.get(LocalLinkDao.class, "sql");
        this.lang = Language.getByLangCode(lang);
        this.sr = conf.get(
                MonolingualSRMetric.class, "ensemble",
                "language", this.lang.getLangCode());
    }

    LocalPageDao pDao;
    LocalLinkDao lDao;
    Language lang;
    MonolingualSRMetric sr;


    /**
     * Add all related articles to a list and find the most related one
     *
     * @param srcId
     * @param degrees
     * @return pageId
     * @throws DaoException
     */
    public int getMostSimilar(int srcId, int degrees) throws DaoException {
        int count = degrees;
        List<Integer> idList = new LinkedList<Integer>();
        int mostSimilarId = -1;
        double maxSimilarity = -1;

        idList.add(srcId);
        int prevSize = 0;

        while (count > 0){
            int size = idList.size();
            for (int i = prevSize; i < size; i++) {
                Iterable<LocalLink> outlinks = lDao.getLinks(lang, idList.get(i), true);
                for (LocalLink outlink : outlinks) {
                    if (idList.contains(outlink.getDestId())) continue;
                    idList.add(outlink.getDestId());
                }
            }
            prevSize = size;
            count--;
        }

        idList.remove(0);

        for (int id: idList) {
            double score = sr.similarity(srcId, id, false).getScore();
            if (score > maxSimilarity) {
                maxSimilarity = score;
                mostSimilarId = id;
            }
        }

        return mostSimilarId;

    }

    public String getMostSimilar(String srcTitle, int degrees) throws DaoException {
        int srcId = pDao.getIdByTitle(srcTitle, lang, NameSpace.ARTICLE);
        return pDao.getById(lang, getMostSimilar(srcId, degrees)).getTitle().toString();
    }
}
