package org.wikapidia.core.model;

import com.google.common.collect.Multimap;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Brent Hecht
 */
public abstract class UniversalPage<T extends LocalPage> {

    /**
     * The universal id for the universal page. Universal ids are defined within but not across namespaces.
     */
    private final int univId;
    private final Multimap<Language, T> localPages;

    public UniversalPage(int univId, Multimap<Language, T> localPages){
        this.univId = univId;
        this.localPages = localPages;
    }

    public int getUnivId(){
        return univId;
    }

    /**
     * Gets the set of pages in the input language.
     * @param language
     * @return A collection of local pages or null if no pages in the input language exist in this concept.
     */
    public Collection<T> getLocalPages(Language language){
        return Collections.unmodifiableCollection(localPages.get(language));
    }

    /**
     * Returns true iff UniversalPage has page in input language.
     * @param language
     * @return True if UniversalPage has page in input language, false otherwise.
     */
    public boolean isInLanguage(Language language){
        return localPages.containsKey(language);
    }

    /**
     * Compares the set of languages in which this UniversalPage exists to the
     * set of languages in the input language set.
     * @param ls The set of languages to query against.
     * @param mustBeInAllLangs Whether or not the UniversalPage must exist in all input languages in
     *                         order to return true.
     * @return If mustBeInAllLangs is true, returns true iff UniversalPage exists in all languages in the input language set.
     * If mustBeInAllLangs is false, returns true iff UniversalPage exists in a single language in the input language set.
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
     * Returns the number of languages in which this UniversalPage exists.
     * @return
     */
    public Integer getNumberOfLanguages(){
        return localPages.size();
    }

    /**
     * Returns a language set of languages in which this UniversalPage has pages.
     * The default language of the language set is undefined.
     * @return
     */
    public LanguageSet getLanguageSetOfExistsInLangs() {
        return new LanguageSet(localPages.keys());
    }

    /**
     * Gets the clarity of the UniversalPage. Clarity was used in Hecht and Gergle (2010) and Bao et al. (2012) and is fully defined there.
     * Briefly, clarity = the number of languages in which a UniversalPage exists divided by the number of LocalPages in the UniversalPage.
     * If a UniversalPage has a clarity of 1, this means it has only one page per language. A lower clarity means that there is more than one page in at least one language.
     * @return
     */
    public double getClarity(){
        double rVal = localPages.keySet().size() / ((double)getNumberOfPages());
        return rVal;
    }

    public int getNumberOfPages() {
        return localPages.values().size();
    }

    public static interface LocalPageChooser<T extends LocalPage> {
        public T choose(Collection<T> localPages);
    }
}
