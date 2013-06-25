package org.wikapidia.core.dao;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.model.NameSpace;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A helper class for specifying complex queries.  To use, instantiate a new instance,
 * than call the various set methods in a chain to set the filters.
 * Possible filters are, with the objects that use them:
 *
 * - Language collection     (LocalPage, RawPage, LocalLink, Redirect, LocalCategoryMember)
 * - NameSpace collection    (LocalPage, RawPage, UniversalPage)
 * - Redirect flag           (LocalPage, RawPage)
 * - Disambiguation flag     (LocalPage, RawPage)
 * - LocationType collection (LocalLink)
 * - Source ID collection    (LocalLink, Redirect, UniversalLink)
 * - Dest ID collection      (LocalLink, Redirect, UniversalLink)
 * - Parseable flag          (LocalLink, Redirect)
 * - Algorithm ID collection (UniversalPage, UniversalLink)
 *
 * Not all filters are apply to all objects. Collections are specified as a collection
 * of acceptable entries, while flags are booleans set to true, false, or null. Flags
 * and collections set to null will be ignored when the search is executed.
 *
 * A call might look something like:
 * DaoFilter df = new DaoFilter.
 *          setLanguages(languageSet).
 *          setNameSpace(nameSpaces).
 *          setRedirect(true).
 *          setDisambig(false);
 */
public class DaoFilter {

    private Collection<Short> langIds;
    private Collection<Short> nsIds;
    private Boolean isRedirect;
    private Boolean isDisambig;
    private Collection<Short> locTypeIds;
    private Collection<Integer> sourceIds;
    private Collection<Integer> destIds;
    private Boolean isParseable;
    private Collection<Integer> algorithmIds;


    public DaoFilter() {
        langIds = null;
        nsIds = null;
        isRedirect = null;
        isDisambig = null;
        sourceIds = null;
        destIds = null;
        locTypeIds = null;
        isParseable = null;
        algorithmIds = null;
    }

    public Collection<Short> getLangIds() {
        return langIds;
    }

    public Collection<Short> getNameSpaceIds() {
        return nsIds;
    }

    public Boolean isRedirect() {
        return isRedirect;
    }

    public Boolean isDisambig() {
        return isDisambig;
    }

    public Collection<Short> getLocTypes() {
        return locTypeIds;
    }

    public Collection<Integer> getSourceIds() {
        return sourceIds;
    }

    public Collection<Integer> getDestIds() {
        return destIds;
    }

    public Boolean isParseable() {
        return isParseable;
    }

    public Collection<Integer> getAlgorithmIds() {
        return algorithmIds;
    }

    /**
     * Sets the language filter to the specified LanguageSet.
     * Used by LocalPage, RawPage, LocalLink, Redirect, and LocalCategoryMember.
     * @param languages
     * @return
     */
    public DaoFilter setLanguages(LanguageSet languages) {
        return setLanguages(languages.getLanguages());
    }

    /**
     * Sets the language filter to the specified collection of languages.
     * Used by LocalPage, RawPage, LocalLink, Redirect, and LocalCategoryMember.
     * @param languages
     * @return
     */
    public DaoFilter setLanguages(Collection<Language> languages) {
        Collection<Short> temp = new ArrayList<Short>();
        if (languages.isEmpty() || languages==null) {
            temp = null;
        }
        else {
            for (Language l : languages) {
                temp.add(l.getId());
            }
        }
        this.langIds = temp;
        return this;
    }

    public DaoFilter setLanguages(Language language) {
        return setLanguages(Arrays.asList(new Language[]{language}));
    }

    /**
     * Sets the namespace filter to the specified collection of namespace constants.
     * Used by LocalPage, RawPage, and UniversalPage.
     * @param nameSpaces
     * @return
     */
    public DaoFilter setNameSpaces(Collection<NameSpace> nameSpaces) {
        Collection<Short> temp = new ArrayList<Short>();
        if (nameSpaces.isEmpty() || nameSpaces==null) {
            temp = null;
        }
        else {
            for (NameSpace ns : nameSpaces) {
                temp.add(ns.getArbitraryId());
            }
        }
        this.nsIds = temp;
        return this;
    }

    /**
     * Sets the redirect flag.
     * Used by LocalPage and RawPage.
     * @param redirect
     * @return
     */
    public DaoFilter setRedirect(Boolean redirect) {
        this.isRedirect = redirect;
        return this;
    }

    /**
     * Sets the disambiguation flag.
     * Used by LocalPage and RawPage.
     * @param disambig
     * @return
     */
    public DaoFilter setDisambig(Boolean disambig) {
        this.isDisambig = disambig;
        return this;
    }

    /**
     * Sets the Location Type filter for a LocalLink to the specified array.
     * Used only by LocalLink.
     * @param locTypes
     * @return
     */
    public DaoFilter setLocTypeIds(LocalLink.LocationType[] locTypes) {
        return setLocTypeIds(Arrays.asList(locTypes));
    }

    /**
     * Sets the Location Type filter for a LocalLink to the specified collection.
     * Used only by LocalLink.
     * @param locTypes
     * @return
     */
    public DaoFilter setLocTypeIds(Collection<LocalLink.LocationType> locTypes) {
        Collection<Short> temp = new ArrayList<Short>();
        if (locTypes.isEmpty() || locTypes==null) {
            temp = null;
        }
        else {
            for (LocalLink.LocationType lt : locTypes) {
                temp.add((short)lt.ordinal());
            }
        }
        this.locTypeIds = temp;
        return this;
    }

    /**
     * Sets the SourceIds filter to the specified collection.
     * Used by LocalLink, UniversalLink, and Redirect.
     * @param sourceIds
     * @return
     */
    public DaoFilter setSourceIds(Collection<Integer> sourceIds) {
        this.sourceIds = sourceIds;
        return this;
    }

    /**
     * Sets the DestinationIds filter to the specified collection.
     * Used by LocalLink, UniversalLink, and Redirect.
     * @param destIds
     * @return
     */
    public DaoFilter setDestIds(Collection<Integer> destIds) {
        this.destIds = destIds;
        return this;
    }

    /**
     * Sets the Parseable flag.
     * Used by LocalLink and Redirect.
     * @param parseable
     * @return
     */
    public DaoFilter setParseable(Boolean parseable) {
        isParseable = parseable;
        return this;
    }

    /**
     * Sets the AlgorithmIds filter to the specified single algorithm ID.
     * Used by UniversalPage and UniversalLink.
     * @param algorithmId
     * @return
     */
    public DaoFilter setAlgorithmIds(int algorithmId) {
        return setAlgorithmIds(Arrays.asList(new Integer[]{algorithmId}));
    }

    /**
     * Sets the AlgorithmIds filter to the specified collection of algorithm IDs.
     * Used by UniversalPage and UniversalLink.
     * @param algorithmIds
     * @return
     */
    public DaoFilter setAlgorithmIds(Collection<Integer> algorithmIds) {
        this.algorithmIds = algorithmIds;
        return this;
    }
}
