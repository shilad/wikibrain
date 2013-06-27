package org.wikapidia.metrics;

import gnu.trove.set.TIntSet;
import org.wikapidia.core.jooq.tables.UniversalPage;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;

import java.io.File;
import java.io.IOException;

public interface SimilarityMetric {

    /**
     *
     * @return the name of the similarity metric in a human readable format
     */
    public String getName();

    /**
     * Determine the similarity between two local pages
     * @param page1
     * @param page2
     * @param explanations should create explanations
     * @return
     */
    public SRResult similarity(LocalPage page1, LocalPage page2, boolean explanations);

    /**
     * Determine the similarity between two universal pages
     * @param page1
     * @param page2
     * @param explanations should create explanations
     * @return
     */
    public SRResult similarity(UniversalPage page1, UniversalPage page2, boolean explanations);

    /**
     * Determine the similarity between two strings in a given language by mapping through local pages
     * @param phrase1 a word or phrase in language
     * @param phrase2 a word or phrase in language
     * @param language
     * @param explanations should create explanations
     * @return
     */
    public SRResult localSimilarity(String phrase1, String phrase2, Language language, boolean explanations);

    /**
     * Determine the similarity between two strings in a given language using Universal pages
     * @param phrase1
     * @param phrase2
     * @param language
     * @param explanations should create explanations
     * @return
     */
    public SRResult universalSimilarity(String phrase1, String phrase2, Language language, boolean explanations);

    /**
     * Determine the similarity between two strings, which may be in different languages, using Universal pages
     * @param phrase1
     * @param language1 the language of phrase1
     * @param phrase2
     * @param language2 the language of phrase2
     * @param explanations should create explanations
     * @return
     */
    public SRResult universalSimilarity(String phrase1, Language language1, String phrase2, Language language2, boolean explanations);

    /**
     * Find the most similar local pages to a local page within the same language
     * @param page
     * @param maxResults
     * @param explanations should create explanations
     * @return
     */
    public SRResultList mostSimilar(LocalPage page, int maxResults, boolean explanations);

    /**
     * Find the most similar local pages to a local page from a set of local pages within the same language
     * @param page
     * @param maxResults
     * @param explanations should create explanations
     * @param validIds a set of LocalPage ids to find mostSimilar from
     * @return
     */
    public SRResultList mostSimilar(LocalPage page, int maxResults, boolean explanations, TIntSet validIds);

    /**
     * Find the most similar universal pages to a universal page
     * @param page
     * @param maxResults
     * @param explanations should create explanations
     * @return
     */
    public SRResultList mostSimilar(UniversalPage page, int maxResults, boolean explanations);

    /**
     * Find the most similar universal pages to a universal page from a set of universal pages
     * @param page
     * @param maxResults
     * @param explanations should create explanations
     * @param validIds a set of UniversalPage ids to find mostSimilar from
     * @return
     */
    public SRResultList mostSimilar(UniversalPage page, int maxResults, boolean explanations, TIntSet validIds);

    /**
     * Find the most similar local pages to a string in a given language
     * @param phrase a word or phrase in language
     * @param language
     * @param maxResults
     * @param explanations should create explanations
     * @return
     */
    public SRResultList localMostSimilar(String phrase, Language language, int maxResults, boolean explanations);

    /**
     * Find the most similar local pages to a string in a given language from a given set of local pages
     * @param phrase a word or phrase in language
     * @param language
     * @param maxResults
     * @param explanations should create explanations
     * @param validIds a set of LocalPage ids to find mostSimilar from in the same language as language
     * @return
     */
    public SRResultList localMostSimilar(String phrase, Language language, int maxResults, boolean explanations, TIntSet validIds);

    /**
     * Find the most similar universal concepts to a string in a given language
     * @param phrase a word or phrase in language
     * @param language
     * @param maxResults
     * @param explanations should create explanations
     * @return
     */
    public SRResultList universalMostSimilar(String phrase, Language language, int maxResults, boolean explanations);

    /**
     * Find the most similar universal pages to a string in a given language from a set of universal pages
     * @param phrase a word or phrase in language
     * @param language
     * @param maxResults
     * @param explanations should create explanations
     * @param validIds  a set of UniversalPage ids to find mostSimilar from
     * @return
     */
    public SRResultList universalMostSimilar(String phrase, Language language, int maxResults, boolean explanations, TIntSet validIds);

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
     * Construct a cosimilarity matrix of Wikipedia ids in a given language
     * @param wpRowIds
     * @param wpColIds
     * @return
     * @throws IOException
     */
    public double[][] localCosimilarity(int wpRowIds[], int wpColIds[], Language language) throws IOException;


    //TODO: HOW TO INCORPORATE LANGUAGE?
    /**
     * Construct a cosimilarity matrix of phrases.
     * @param rowPhrases
     * @param colPhrases
     * @return
     * @throws IOException
     */
    public double[][] localCosimilarity(String rowPhrases[], String colPhrases[]) throws IOException;

    /**
     * Construct symmetric comsimilarity matrix of Wikipedia ids in a given language
     * @param ids
     * @return
     * @throws IOException
     */
    public double[][] localCosimilarity(int ids[], Language language) throws IOException;

    //TODO: HOW TO INCORPORATE LANGUAGE?
    /**
     * Construct symmetric cosimilarity matrix of phrases
     * @param phrases
     * @return
     * @throws IOException
     */
    public double[][] localCosimilarity(String phrases[]) throws IOException;

    /**
     * Construct a cosimilarity matrix of Universal Page ids.
     * @param wpRowIds
     * @param wpColIds
     * @return
     * @throws IOException
     */
    public double[][] universalCosimilarity(int wpRowIds[], int wpColIds[]) throws IOException;

    //TODO: HOW TO INCORPORATE LANGUAGE?
    /**
     * Construct a cosimilarity matrix of phrases.
     * @param rowPhrases
     * @param colPhrases
     * @return
     * @throws IOException
     */
    public double[][] universalCosimilarity(String rowPhrases[], String colPhrases[]) throws IOException;

    /**
     * Construct symmetric comsimilarity matrix of Universal Page ids
     * @param ids
     * @return
     * @throws IOException
     */
    public double[][] universalCosimilarity(int ids[]) throws IOException;

    //TODO: HOW TO INCORPORATE LANGUAGE?
    /**
     * Construct symmetric cosimilarity matrix of phrases
     * @param phrases
     * @return
     * @throws IOException
     */
    public double[][] universalCosimilarity(String phrases[]) throws IOException;
}
