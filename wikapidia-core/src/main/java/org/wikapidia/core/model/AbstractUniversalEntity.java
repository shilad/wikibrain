package org.wikapidia.core.model;

import com.google.common.collect.Multimap;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 */
public abstract class AbstractUniversalEntity<T> {

    protected final int algorithmId;
    protected final Multimap<Language, T> localEntities;

    public AbstractUniversalEntity(int algorithmId, Multimap<Language, T> localEntities) {
        this.algorithmId = algorithmId;
        this.localEntities = localEntities;
    }

    public int getAlgorithmId() {
        return algorithmId;
    }

    /**
     * Gets the set of entities in the input language.
     * @param language
     * @return A collection of local pages or null if no pages in the input language exist in this concept.
     */
    public Collection<T> getLocalEntities(Language language){
        return Collections.unmodifiableCollection(localEntities.get(language));
    }

    /**
     * Returns true iff UniversalEntity has page in input language.
     * @param language
     * @return True if UniversalEntity has page in input language, false otherwise.
     */
    public boolean isInLanguage(Language language){
        return localEntities.containsKey(language);
    }

    /**
     * Compares the set of languages in which this UniversalEntity exists to the
     * set of languages in the input language set.
     * @param ls The set of languages to query against.
     * @param mustBeInAllLangs Whether or not the UniversalEntity must exist in all input languages in
     *                         order to return true.
     * @return If mustBeInAllLangs is true, returns true iff UniversalEntity exists in all languages in the input language set.
     * If mustBeInAllLangs is false, returns true iff UniversalEntity exists in a single language in the input language set.
     */
    public boolean isInLanguageSet(LanguageSet ls, boolean mustBeInAllLangs) {

        for (Language lang : ls.getLanguages()) {
            boolean isInLang = isInLanguage(lang);
            if (isInLang && !mustBeInAllLangs){
                return true;
            }
            if (!isInLang && mustBeInAllLangs){
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the number of languages in which this UniversalEntity exists.
     * @return
     */
    public Integer getNumberOfLanguages(){
        return localEntities.size();
    }

    /**
     * Returns a language set of languages in which this UniversalEntity has pages.
     * The default language of the language set is undefined.
     * TODO: choose an appropriate default language based on the config file.
     * @return
     */
    public LanguageSet getLanguageSetOfExistsInLangs() {
        Language en = Language.getByLangCode("en");
        if (localEntities.containsKey(en)) {
            return new LanguageSet(en, localEntities.keys());
        } else {
            List<Language> langs = new ArrayList<Language>(localEntities.keySet());
            Collections.sort(langs);
            return new LanguageSet(langs);
        }
    }

    /**
     * Gets the clarity of the UniversalEntity. Clarity was used in Hecht and Gergle (2010) and Bao et al. (2012) and is fully defined there.
     * Briefly, clarity = the number of languages in which a UniversalEntity exists divided by the number of LocalEntities in the UniversalEntity.
     * If a UniversalEntity has a clarity of 1, this means it has only one page per language. A lower clarity means that there is more than one page in at least one language.
     * @return
     */
    public double getClarity(){
        double rVal = localEntities.keySet().size() / ((double)getNumberOfEntities());
        return rVal;
    }

    /**
     * Returns the amount of local entities contained in this universal
     * @return
     */
    public int getNumberOfEntities() {
        return localEntities.values().size();
    }
}
