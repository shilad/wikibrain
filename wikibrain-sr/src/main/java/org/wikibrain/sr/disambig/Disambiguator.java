package org.wikibrain.sr.disambig;

import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.lang.LocalString;

import java.util.*;

/**
 * Disambiguates phrases to local ids.
 *
 * Right now there should be:
 * - A TopResult disambiguator that always returns the most popular
 * meaning from a PhraseAnalyzer and ignores context.
 * - A BaseDisambiguator that is abstract and uses a PhraseAnalyzer combined with a
 * way of computing scores between terms (i.e. an LocalSRMetric).
 * - Two child classes of BaseDisambiguator: SimilarityDisambiguator and MostSimilarDisambiguator
 *
 * UniversalDisambiguator will be a wrapper class around some concrete
 * implementation of a Disambiguator.
 */
public abstract class Disambiguator {

    /**
     * Disambiguates a single string in some language to a local pages in that language.
     * The result can be considered a probability distribution
     *
     * @param phrase   The target phrase being disambiguated.
     * @param context  Other phrases (in the same language as the target phrase)
     *                 related to the target phrase being disambiguated that may
     *                 aid disambiguation.
     * @return
     */
    public LinkedHashMap<LocalId, Float> disambiguate(LocalString phrase, Set<LocalString> context) throws DaoException {
        return disambiguate(Arrays.asList(phrase), context).get(0);
    }

    /**
     * Disambiguates a single string in some language to a local page in that language.
     *
     * @param phrase   The target phrase being disambiguated.
     * @param context  Other phrases (in the same language as the target phrase)
     *                 related to the target phrase being disambiguated that may
     *                 aid disambiguation.
     * @return
     */
    public LocalId disambiguateTop(LocalString phrase, Set<LocalString> context) throws DaoException {
        LinkedHashMap<LocalId, Float> result = disambiguate(phrase, context);
        if (result == null || result.isEmpty()) {
            return null;
        } else {
            return result.keySet().iterator().next();
        }
    }

    /**
     * Disambiguates multiple strings in some language to local pages in that language.
     *
     * @param phrases   The target phrases being disambiguated.
     * @param context  Other phrases (in the same language as the target phrase)
     *                 related to the target phrase being disambiguated that may
     *                 aid disambiguation.
     * @return          The disambiguated pages. The order matches phrases.
     */
    public List<LocalId> disambiguateTop(List<LocalString> phrases, Set<LocalString> context) throws DaoException {
        List<LocalId> result = new ArrayList<LocalId>();
        for (LinkedHashMap<LocalId, Float> dab : disambiguate(phrases, context)) {
            if (dab.isEmpty()) {
                result.add(null);
            } else {
                result.add(dab.keySet().iterator().next());
            }
        }
        return result;
    }


    /**
     * Disambiguates multiple strings in some language to local pages in that language.
     * Each map can be considered a probability distribution
     *
     * @param phrases   The target phrases being disambiguated.
     * @param context  Other phrases (in the same language as the target phrase)
     *                 related to the target phrase being disambiguated that may
     *                 aid disambiguation.
     * @return          The disambiguated pages. The order matches phrases.
     */
    public abstract List<LinkedHashMap<LocalId, Float>> disambiguate(List<LocalString> phrases, Set<LocalString> context) throws DaoException;
}
