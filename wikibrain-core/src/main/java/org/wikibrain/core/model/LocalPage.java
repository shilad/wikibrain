package org.wikibrain.core.model;

import com.google.common.collect.Sets;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.lang.LocalId;

import java.util.Set;

/**
 */
public class LocalPage {

    protected final Language language;
    protected final int localId;
    protected final Title title;
    protected final NameSpace nameSpace;
    protected final boolean isRedirect;
    protected final boolean isDisambig;

    /**
     * Creates a new page in the main namespace that is NOT a redirect or disambig.
     * @param language
     * @param localId
     * @param title
     */
    public LocalPage(Language language, int localId, String title) {
        this(language, localId, new Title(title, language), NameSpace.ARTICLE);
    }

    /**
     * Default for NON-redirect pages.
     * @param language
     * @param localId
     * @param title
     * @param nameSpace
     */
    public LocalPage(Language language, int localId, Title title, NameSpace nameSpace){
        this.language = language;
        this.localId = localId;
        this.title = title;
        this.nameSpace = nameSpace;
        isRedirect = false;
        isDisambig = false;
    }

    /**
     * Ability to set redirect pages.
     * @param language
     * @param localId
     * @param title
     * @param nameSpace
     * @param redirect
     */
    public LocalPage(Language language, int localId, Title title, NameSpace nameSpace, boolean redirect, boolean disambig) {
        this.language = language;
        this.localId = localId;
        this.title = title;
        this.nameSpace = nameSpace;
        isRedirect = redirect;
        isDisambig = disambig;
    }

    public int getLocalId() {
        return localId;
    }

    public Title getTitle() {
        return title;
    }

    public Language getLanguage() {
        return language;
    }

    public NameSpace getNameSpace() {
        return nameSpace;
    }

    public boolean isDisambig() {
        return isDisambig;
    }

    public boolean isRedirect() {
        return isRedirect;
    }

    public int hashCode(){
        return (language.getId() + "_" + localId).hashCode(); //non-optimal
    }

    public LocalId toLocalId() {
        return new LocalId(language, localId);
    }

    public boolean equals(Object o){
        if (o instanceof LocalPage){
            LocalPage input = (LocalPage)o;
            return (input.getLanguage().equals(this.getLanguage()) &&
                    input.getLocalId() == this.getLocalId()
            );
        } else {
            return false;
        }
    }

    /**
     * @return, for example "/w/en/1000/Hercule_Poirot"
     */
    public String getCompactUrl() {
        String escapedTitle = getTitle().getCanonicalTitle().replace(" ", "_");
        escapedTitle = escapedTitle.replaceAll("\\s+", "");
        return "/w/" + getLanguage().getLangCode() + "/" + getLocalId() + "/" + escapedTitle;
    }

    @Override
    public String toString() {
        return "LocalPage{" +
                "nameSpace=" + nameSpace +
                ", title=" + title +
                ", localId=" + localId +
                ", language=" + language +
                '}';
    }

    /**
     * Returns a set of local ids from a collection of local pages
     * @param localPages
     * @return
     */
    public static Set<LocalId> toLocalIds(Iterable<LocalPage> localPages){

        Set<LocalId> rVal = Sets.newHashSet();
        for (LocalPage localPage : localPages){
            rVal.add(localPage.toLocalId());
        }
        return rVal;
    }

    /**
     * Converts a compact url representation of a page to a LocalPage.
     * @param s
     * @return The local page, or null if the string was not a url.
     */
    public static LocalPage fromCompactUrl(String s) {
        String parts[] = s.split("/", 5);
        if (s.startsWith("/w/") && parts.length == 5 && Language.hasLangCode(parts[2])) {
            return new LocalPage(
                        Language.getByLangCode(parts[2]),
                        Integer.valueOf(parts[3]),
                        parts[4]
                    );
        } else {
            return null;
        }
    }

    /**
     * Determines whether a Url is a compact representation of a title.
     * For example, "/w/en/1000/Hercule_Poirot"
     * @param s
     * @return
     */
    public static boolean isCompactUrl(String s) {
        if (!s.startsWith("/w/")) {
            return false;
        } else {
            String parts[] = s.split("/");
            return parts.length >= 5 && Language.hasLangCode(parts[2]);
        }
    }
}
