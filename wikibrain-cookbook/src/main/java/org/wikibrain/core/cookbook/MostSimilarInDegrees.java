package org.wikibrain.core.cookbook;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalLinkDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.sr.SRMetric;

import java.io.IOException;
import java.util.*;

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
                SRMetric.class, "ensemble",
                "language", this.lang.getLangCode());
    }

    LocalPageDao pDao;
    LocalLinkDao lDao;
    Language lang;
    SRMetric sr;


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
