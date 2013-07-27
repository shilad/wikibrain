package org.wikapidia.sr;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.set.TIntSet;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LocalString;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.matrix.SparseMatrix;
import org.wikapidia.matrix.SparseMatrixRow;
import org.wikapidia.sr.normalize.Normalizer;
import org.wikapidia.sr.utils.Dataset;

import java.io.File;
import java.io.IOException;

/**
 * @author Matt Lesicko
 * @author Ben Hillmann
 */

public interface UniversalSRMetric {
    /**
     *
     * @return the name of the similarity metric in a human readable format
     */
    public String getName();

    /**
     *
     * @return the algorithm id
     */
    public int getAlgorithmId();

    /**
     * Set the cached matrix for a universal metric
     * @param matrix
     */
    public void setMostSimilarUniversalMatrix(SparseMatrix matrix);

        /**
         * Determine the similarity between two universal pages
         * @param page1 The first page.
         * @param page2 The second page.
         * @param explanations Whether explanations should be created.
         * @return
         */
    public SRResult similarity(UniversalPage page1, UniversalPage page2, boolean explanations) throws DaoException;


    /**
     * Determine the similarity between two strings, which may be in different languages, using Universal pages
     * @param phrase1 The first phrase.
     * @param phrase2 The second phrase.
     * @param explanations Whether explanations should be created.
     * @return
     */
    public SRResult similarity(LocalString phrase1, LocalString phrase2, boolean explanations) throws DaoException;

    /**
     * Find the most similar universal pages to a universal page.
     * @param page The universal page whose similarity we are examining.
     * @param maxResults The maximum number of results to return.
     * @return
     */
    public SRResultList mostSimilar(UniversalPage page, int maxResults) throws DaoException;

    /**
     * Find the most similar universal pages to a universal page.
     * @param page The universal page whose similarity we are examining.
     * @param maxResults The maximum number of results to return.
     * @param validIds The universal page ids to be considered.  Null means all ids.
     * @return
     */
    public SRResultList mostSimilar(UniversalPage page, int maxResults, TIntSet validIds) throws DaoException;


    /**
     * Find the most similar universal concepts to a phrase
     * @param phrase The phrase whose similarity we are examining.
     * @param maxResults The maximum number of results to return.
     * @return
     */
    public SRResultList mostSimilar(LocalString phrase, int maxResults) throws DaoException;

    /**
     * Find the most similar universal pages to a string in a given language from a set of universal pages
     * @param phrase The phrase whose similarity we are examining.
     * @param maxResults The maximum number of results to return.
     * @param validIds  The UniversalPage ids to be considered.  Null means all ids.
     * @return
     */
    public SRResultList mostSimilar(LocalString phrase, int maxResults, TIntSet validIds) throws DaoException;

    /**
     * Writes the metric to a directory.
     *
     * @param path A directory data will be written to.
     *                  Any existing data in the directory may be destroyed.
     * @throws java.io.IOException
     */
    public void write(String path) throws IOException;

    /**
     * Reads the metric from a directory.
     *
     * @param path A directory data will be read from.
     *                  The directory previously will have been written to by write().
     * @throws IOException if the file is not found or is unusable
     */
    public void read(String path) throws IOException;

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
    public void trainMostSimilar(Dataset dataset, int numResults, TIntSet validIds) throws DaoException;

    public void setMostSimilarNormalizer(Normalizer n);

    public void setSimilarityNormalizer(Normalizer n);

    /**
     * Construct a cosimilarity matrix of Universal Page ids.
     * @param rowIds
     * @param colIds
     * @return
     * @throws java.io.IOException
     */
    public double[][] cosimilarity(int rowIds[], int colIds[]) throws IOException, DaoException;

    /**
     * Construct a cosimilarity matrix of phrases.
     * @param rowPhrases
     * @param colPhrases
     * @return
     * @throws IOException
     */
    public double[][] cosimilarity(LocalString rowPhrases[], LocalString colPhrases[]) throws IOException, DaoException;

    /**
     * Construct a symmetric cosimilarity matrix of Universal Page ids.
     * @param ids
     * @return
     */
    public double[][] cosimilarity(int ids[]) throws IOException, DaoException;

    /**
     * Construct a symmetric cosimilarity matrix of phrases
     * @param phrases
     * @return
     */
    public double[][] cosimilarity(LocalString phrases[]) throws IOException, DaoException;

    /**
     * Return a vector for a UniversalPage
     * @param id the UniversalPage's id
     * @return a vector relating it to other pages.
     * @throws DaoException
     */
    public TIntDoubleMap getVector(int id) throws DaoException;

    /**
     * Writes a cosimilarity matrix to the dat directory based off of the getVector function and the pairwise cosine similarity class
     * @param path the directory to write the matrix in
     * @param maxHits the number of document hits you would like returned from the most similar function
     */
    void writeCosimilarity(String path, int maxHits) throws IOException, DaoException, WikapidiaException;

    void readCosimilarity(String path) throws IOException;
}
