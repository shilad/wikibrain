package org.wikapidia.sr.disambig;

import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.LocalId;
import org.wikapidia.core.lang.LocalString;

import java.util.List;
import java.util.Set;

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
public interface Disambiguator {

    /**
     * Disambiguates a single string in some language to a local page in that language.
     *
     * @param phrase   The target phrase being disambiguated.
     * @param context  Other phrases (in the same language as the target phrase)
     *                 related to the target phrase being disambiguated that may
     *                 aid disambiguation.
     * @return
     */
    public LocalId disambiguate(LocalString phrase, Set<LocalString> context) throws DaoException;

    /**
     * Disambiguates multiple strings in some language to local pages in that language.
     *
     * @param phrases   The target phrases being disambiguated.
     * @param context  Other phrases (in the same language as the target phrase)
     *                 related to the target phrase being disambiguated that may
     *                 aid disambiguation.
     * @return          The disambiguated pages. The order matches phrases.
     */
    public List<LocalId> disambiguate(List<LocalString> phrases, Set<LocalString> context) throws DaoException;


}
