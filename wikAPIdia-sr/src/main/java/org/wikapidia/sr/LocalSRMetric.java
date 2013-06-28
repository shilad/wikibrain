package org.wikapidia.sr;

import gnu.trove.set.TIntSet;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.sr.utils.KnownSim;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface LocalSRMetric {

    /**
     *
     * @return the name of the similarity metric in a human readable format
     */
    public String getName();

    /**
     * Determine the similarity between two local pages
     * @param page1 The first page.
     * @param page2 The second page.
     * @param explanations Whether explanations should be created.
     * @return
     */
    public SRResult similarity(LocalPage page1, LocalPage page2, boolean explanations);

    /**
     * Determine the similarity between two strings in a given language by mapping through local pages
     * @param phrase1 The first phrase.
     * @param phrase2 The second phrase.
     * @param language The language of the phrases.
     * @param explanations Whether explanations should be created.
     * @return
     */
    public SRResult similarity(String phrase1, String phrase2, Language language, boolean explanations);

    /**
     * Find the most similar local pages to a local page within the same language
     * @param page The local page whose similarity we are examining.
     * @param maxResults The maximum number of results to return.
     * @param explanations Whether explanations should be created.
     * @return
     */
    public SRResultList mostSimilar(LocalPage page, int maxResults, boolean explanations);

    /**
     * Find the most similar local pages to a local page.
     * @param page The local page whose similarity we are examining.
     * @param maxResults The maximum number of results to return.
     * @param explanations Whether explanations should be created.
     * @param validIds The local page ids to be considered.  Null means all ids in the language.
     * @return
     */
    public SRResultList mostSimilar(LocalPage page, int maxResults, boolean explanations, TIntSet validIds);

    /**
     * Find the most similar local pages to a phrase,
     * @param phrase The phrase whose similarity we are examining.
     * @param maxResults The maximum number of results to return.
     * @param explanations Whether explanations should be created.
     * @return
     */
    public SRResultList mostSimilar(LocalString phrase, int maxResults, boolean explanations);

    /**
     * Find the most similar local pages to a phrase
     * @param phrase The phrase whose similarity we are examining.
     * @param maxResults The maximum number of results to return.
     * @param explanations Whether explanations should be created.
     * @param validIds The local page ids to be considered.  Null means all ids in the language
     * @return
     */
    public SRResultList mostSimilar(LocalString phrase, int maxResults, boolean explanations, TIntSet validIds);

    /**
     * Writes the metric to a directory.
     * @param directory A directory data will be written to.
     *                  Any existing data in the directory may be destroyed.
     * @throws java.io.IOException
     */
    public void write(File directory) throws IOException;

    /**
     * Reads the metric from a directory.
     * @param directory A directory data will be read from.
     *                  The directory previously will have been written to by write().
     * @throws IOException
     */
    public void read(File directory) throws IOException;

    /**
     * Train the similarity() function.
     * The KnownSims may already be associated with Wikipedia ids (check wpId1 and wpId2).
     * @param labeled The labeled gold standard dataset.
     */
    public void trainSimilarity(List<KnownSim> labeled);

    /**
     * Train the mostSimilar() function
     * The KnownSims may already be associated with Wikipedia ids (check wpId1 and wpId2)
     * @param labeled The labeled gold standard dataset.
     * @param numResults The maximum number of similar articles computed per phrase.
     * @param validIds The Wikipedia ids that should be considered in result sets. Null means all ids.
     */
    public void trainMostSimilar(List<KnownSim> labeled, int numResults, TIntSet validIds);


    /**
     * Construct a cosimilarity matrix of Wikipedia ids in a given language
     * @param wpRowIds
     * @param wpColIds
     * @param language The language of the pages.
     * @return
     * @throws IOException
     */
    public double[][] cosimilarity(int wpRowIds[], int wpColIds[], Language language) throws IOException;


    /**
     * Construct a cosimilarity matrix of phrases.
     * @param rowPhrases
     * @param colPhrases
     * @param language The language of the phrases.
     * @return
     * @throws IOException
     */
    public double[][] cosimilarity(String rowPhrases[], String colPhrases[], Language language) throws IOException;

    /**
     * Construct symmetric comsimilarity matrix of Wikipedia ids in a given language
     * @param ids
     * @return
     * @throws IOException
     */
    public double[][] cosimilarity(int ids[], Language language) throws IOException;

    /**
     * Construct symmetric cosimilarity matrix of phrases by mapping through local pages
     * @param phrases
     * @param language The language of the phrases.
     * @return
     * @throws IOException
     */
    public double[][] cosimilarity(String phrases[], Language language) throws IOException;
}