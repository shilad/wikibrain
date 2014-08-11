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
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataValue;

/**
 * @author Shilad Sen
 */
public class SimilarMusicians {
    public static void main(String args[]) throws ConfigurationException, DaoException {
        Env env = EnvBuilder.envFromArgs(args);
        Language lang = env.getLanguages().getDefaultLanguage();
        WikidataDao wdd = env.getConfigurator().get(WikidataDao.class);
        LocalPageDao lpd = env.getConfigurator().get(LocalPageDao.class);
        SRMetric sr = env.getConfigurator().get(SRMetric.class, "ensemble", "language", lang.getLangCode());

        for (SRResult hit : sr.mostSimilar("jazz", 10, null)) {
            System.out.println(hit.getScore() + ": " + lpd.getById(lang, hit.getId()));
        }

        TIntSet candidates = new TIntHashSet();
        //for (LocalId lid : wdd.pagesWithValue("occupation", WikidataValue.forItem(639669), Language.SIMPLE)) {
        for (LocalId lid : wdd.pagesWithValue("instance of", WikidataValue.forItem(11424), lang)) {
            candidates.add(lid.getId());
        }
	System.out.println("found " + candidates.size() + " candidates.");

	System.out.println("");
        int milesId = lpd.getIdByTitle("Jazz", lang, NameSpace.ARTICLE);
        for (SRResult hit : sr.mostSimilar(milesId, 10, candidates)) {
            System.out.println(hit.getScore() + ": " + lpd.getById(lang, hit.getId()));
        }
    }
}
