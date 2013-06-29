package org.wikapidia.sr;

import gnu.trove.set.TIntSet;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.UniversalPage;

import java.io.IOException;

public interface UniversalSRMetric {
    /**
     *
     * @return the name of the similarity metric in a human readable format
     */
    public String getName();


    /**
     * Determine the similarity between two universal pages
     * @param page1 The first page.
     * @param page2 The second page.
     * @param explanations Whether explanations should be created.
     * @return
     */
    public SRResult similarity(UniversalPage page1, UniversalPage page2, boolean explanations);


    /**
     * Determine the similarity between two strings, which may be in different languages, using Universal pages
     * @param phrase1 The first phrase.
     * @param phrase2 The second phrase.
     * @param explanations Whether explanations should be created.
     * @return
     */
    public SRResult similarity(LocalString phrase1, LocalString phrase2, boolean explanations);

    /**
     * Find the most similar universal pages to a universal page.
     * @param page The universal page whose similarity we are examining.
     * @param maxResults The maximum number of results to return.
     * @param explanations Whether explanations should be created.
     * @return
     */
    public SRResultList mostSimilar(UniversalPage page, int maxResults, boolean explanations);

    /**
     * Find the most similar universal pages to a universal page.
     * @param page The universal page whose similarity we are examining.
     * @param maxResults The maximum number of results to return.
     * @param explanations Whether explanations should be created.
     * @param validIds The universal page ids to be considered.  Null means all ids.
     * @return
     */
    public SRResultList mostSimilar(UniversalPage page, int maxResults, boolean explanations, TIntSet validIds);


    /**
     * Find the most similar universal concepts to a phrase
     * @param phrase The phrase whose similarity we are examining.
     * @param maxResults The maximum number of results to return.
     * @param explanations Whether explanations should be created.
     * @return
     */
    public SRResultList mostSimilar(LocalString phrase, int maxResults, boolean explanations);

    /**
     * Find the most similar universal pages to a string in a given language from a set of universal pages
     * @param phrase The phrase whose similarity we are examining.
     * @param maxResults The maximum number of results to return.
     * @param explanations Whether explanations should be created.s
     * @param validIds  The UniversalPage ids to be considered.  Null means all ids.
     * @return
     */
    public SRResultList mostSimilar(LocalString phrase, int maxResults, boolean explanations, TIntSet validIds);


    /**
     * Construct a cosimilarity matrix of Universal Page ids.
     * @param rowIds
     * @param colIds
     * @return
     * @throws java.io.IOException
     */
    public double[][] cosimilarity(int rowIds[], int colIds[]) throws IOException;

    /**
     * Construct a cosimilarity matrix of phrases.
     * @param rowPhrases
     * @param colPhrases
     * @return
     * @throws IOException
     */
    public double[][] cosimilarity(LocalString rowPhrases[], LocalString colPhrases[]) throws IOException;

    /**
     * Construct a symmetric cosimilarity matrix of Universal Page ids.
     * @param ids
     * @return
     */
    public double[][] cosimilarity(int ids[]) throws IOException;

    /**
     * Construct a symmetric cosimilarity matrix of phrases
     * @param phrases
     * @return
     */
    public double[][] cosimilarity(LocalString phrases[]) throws IOException;

}
