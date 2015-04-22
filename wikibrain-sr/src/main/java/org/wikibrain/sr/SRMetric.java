package org.wikibrain.sr;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.set.TIntSet;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.sr.dataset.Dataset;
import org.wikibrain.sr.normalize.Normalizer;

import java.io.File;
import java.io.IOException;

/**
 * A monolingual SR metric supports SR operations in a single language.
 * @author Matt Lesicko
 * @author Shilad Sen
 */

public interface SRMetric {

    /**
     * @return the name of the similarity metric in a human readable format
     */
    public String getName();

    /**
     * @return The language associated with this metric.
     */
    public Language getLanguage();

    /**
     * Returns the directory containing all data for the metric.
     * @return
     */
    public File getDataDir();

    /**
     * Sets the data directory associated with the model.
     * This will apply to all future reads and writes.
     *
     * @param dir
     */
    public void setDataDir(File dir);

    /**
     * Determine the similarity between two local pages.
     *
     * @param pageId1 Id of the first page.
     * @param pageId2 Id of the second page.
     * @param explanations Whether explanations should be created.
     * @return
     */
    public SRResult similarity(int pageId1, int pageId2, boolean explanations) throws DaoException;

    /**
     * Determine the similarity between two strings in a given language by mapping through local pages.
     *
     * @param phrase1 The first phrase.
     * @param phrase2 The second phrase.
     * @param explanations Whether explanations should be created.
     * @return
     */
    public SRResult similarity(String phrase1, String phrase2, boolean explanations) throws DaoException;

    /**
     * Find the most similar local pages to a local page within the same language.
     *
     * @param pageId The id of the local page whose similarity we are examining.
     * @param maxResults The maximum number of results to return.
     * @return
     */
    public SRResultList mostSimilar(int pageId, int maxResults) throws DaoException;

    /**
     * Find the most similar local pages to a local page.
     *
     * @param pageId The id of the local page whose similarity we are examining.
     * @param maxResults The maximum number of results to return.
     * @param validIds The local page ids to be considered.  Null means all ids in the language.
     * @return
     */
    public SRResultList mostSimilar(int pageId, int maxResults, TIntSet validIds) throws DaoException;

    /**
     * Find the most similar local pages to a phrase.
     *
     * @param phrase The phrase whose similarity we are examining.
     * @param maxResults The maximum number of results to return.
     * @return
     */
    public SRResultList mostSimilar(String phrase, int maxResults) throws DaoException;

    /**
     * Find the most similar local pages to a phrase.
     *
     * @param phrase The phrase whose similarity we are examining.
     * @param maxResults The maximum number of results to return.
     * @param validIds The local page ids to be considered.  Null means all ids in the language
     * @return
     */
    public SRResultList mostSimilar(String phrase, int maxResults, TIntSet validIds) throws DaoException;

    /**
     * Writes the metric to the current data directory.
     *
     * @throws java.io.IOException
     */
    public void write() throws IOException;

    /**
     * Reads the metric from the current data directory.
     */
    public void read() throws IOException;

    /**
     * Train the similarity() function.
     * The KnownSims may already be associated with Wikipedia ids (check wpId1 and wpId2).
     *
     * @param dataset A gold standard dataset
     */
    public void trainSimilarity(Dataset dataset) throws DaoException;

    /**
     * Train the mostSimilar() function
     * The KnownSims may already be associated with Wikipedia ids (check wpId1 and wpId2).
     *
     * @param dataset A gold standard dataset.
     * @param numResults The maximum number of similar articles computed per phrase.
     * @param validIds The Wikipedia ids that should be considered in result sets. Null means all ids.
     */
    public void trainMostSimilar(Dataset dataset, int numResults, TIntSet validIds);

    /**
     * @return true if similarity() is already trained (or doesn't need training)
     */
    public boolean similarityIsTrained();

    /**
     * @return true if mostSimilar() is already trained (or doesn't need training)
     */
    public boolean mostSimilarIsTrained();

    /**
     * Construct a cosimilarity matrix of Wikipedia ids in a given language.
     *
     * @param wpRowIds
     * @param wpColIds
     * @return
     */
    public double[][] cosimilarity(int wpRowIds[], int wpColIds[]) throws DaoException;


    /**
     * Construct a cosimilarity matrix of phrases.
     *
     * @param rowPhrases
     * @param colPhrases
     * @return
     */
    public double[][] cosimilarity(String rowPhrases[], String colPhrases[]) throws DaoException;

    /**
     * Construct symmetric comsimilarity matrix of Wikipedia ids in a given language.
     *
     * @param ids
     * @return
     */
    public double[][] cosimilarity(int ids[]) throws DaoException;

    /**
     * Construct symmetric cosimilarity matrix of phrases by mapping through local pages.
     *
     * @param phrases
     * @return
     */
    public double[][] cosimilarity(String phrases[]) throws DaoException;

    /**
     * @return the most similar normalizer.
     */
    public Normalizer getMostSimilarNormalizer();

    /**
     * Sets the most similar normalizer
     * @param n
     */
    public void setMostSimilarNormalizer(Normalizer n);

    /**
     *
     * @return the similarity normalizer.
     */
    public Normalizer getSimilarityNormalizer();

    /**
     * Sets the similarity normalizer.
     * @param n
     */
    public void setSimilarityNormalizer(Normalizer n);
}
