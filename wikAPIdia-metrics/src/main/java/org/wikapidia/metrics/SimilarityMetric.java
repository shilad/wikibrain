package org.wikapidia.metrics;

import gnu.trove.set.TIntSet;
import org.wikapidia.core.jooq.tables.UniversalPage;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;

public interface SimilarityMetric {

    /**
     *
     * @return the name of the similarity metric in a human readable format
     */
    public String getName();

    public SRResult similarity(LocalPage page1, LocalPage page2, boolean explanations);

    public SRResult similarity(UniversalPage page1, UniversalPage page2, boolean explanations);

    public SRResult similarity(String phrase1, String phrase2, Language language, boolean explanations);

    public SRResultList mostSimilar(LocalPage page, int maxResults, boolean explanations);

    //TODO: Should the filtering be by ids?

    public SRResultList mostSimilar(LocalPage page, int maxResults, boolean explanations, TIntSet validIds);

    public SRResultList mostSimilar(UniversalPage page, int maxResults, boolean explanations);

    public SRResultList mostSimilar(UniversalPage page, int maxResults, boolean explanations, TIntSet validIds);

    public SRResultList localMostSimilar(String phrase, Language language, int maxResults, boolean explanations);

    public SRResultList localMostSimilar(String phrase, Language language, int maxResults, boolean explanations, TIntSet validIds);

    public SRResultList universalMostSimilar(String phrase, Language language, int maxResults, boolean explanations);

    public SRResultList universalMostSimilar(String phrase, Language language, int maxResults, boolean explanations, TIntSet validIds);
}
