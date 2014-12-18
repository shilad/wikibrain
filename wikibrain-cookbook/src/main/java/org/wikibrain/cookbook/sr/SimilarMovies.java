package org.wikibrain.cookbook.sr;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataValue;

/**
 * @author Shilad Sen
 */
public class SimilarMovies {
    public static void main(String args[]) throws ConfigurationException, DaoException {
        Env env = EnvBuilder.envFromArgs(args);

        String phrase = "Berlin";
        Language lang = env.getDefaultLanguage();
        WikidataDao wdd = env.getConfigurator().get(WikidataDao.class);
        LocalPageDao lpd = env.getConfigurator().get(LocalPageDao.class);
        SRMetric sr = env.getConfigurator().get(SRMetric.class, "milnewitten", "language", lang.getLangCode());

        // Most similar to phrase
        SRResultList results = sr.mostSimilar(phrase, 10, null);
        results.sortDescending();
        for (SRResult hit : results) {
            System.out.println(hit.getScore() + ": " + lpd.getById(lang, hit.getId()));
        }

        // Get all movies
        TIntSet candidates = new TIntHashSet();
        for (LocalId lid : wdd.pagesWithValue("instance of", WikidataValue.forItem(11424), lang)) {
            candidates.add(lid.getId());
        }

        // Most similar movies to phrase
        int pageId = lpd.getIdByTitle(phrase, lang, NameSpace.ARTICLE);
        results = sr.mostSimilar(pageId, 10, candidates);
        results.sortDescending();
        for (SRResult hit : results) {
            System.out.println(hit.getScore() + ": " + lpd.getById(lang, hit.getId()));
        }
    }
}
