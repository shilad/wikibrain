package org.wikapidia.core.lang;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.wikapidia.core.WikapidiaException;

import java.util.*;

/**
 * User: bjhecht
 */
public class LanguageSet implements Iterable<Language> {
    public static final LanguageSet ALL = new LanguageSet(
            Language.getByLangCode("en"),
            Arrays.asList(Language.LANGUAGES));

    private Set<Language> langs;
    private Language defaultLanguage;

    /**
     * Initializes a new instance of a LanguageSet using a comma-separated list of
     * language codes (as defined by the Wikimedia Foundation). For instance,
     * "en,de,fr" will result in a LanguageSet with English, German, and French.
     * @param csv A list of language codes separated by commas. The first language
     *            is automatically assumed to be the default language.
     */
    public LanguageSet(String csv) {
        this(Arrays.asList(csv.split(",")));
    }

    public LanguageSet(List<String> langCodes) {
        langs = Sets.newHashSet();
        defaultLanguage = null;
        for (String langCode : langCodes) {
            langCode = langCode.trim(); // handle whitespace issues just in case
            Language lang = Language.getByLangCode(langCode);
            langs.add(lang);
            if (defaultLanguage == null){
                defaultLanguage = lang;
            }
        }
    }

    /**
     * Creates an instance of a language set with defaultLang as the default language and
     * inputLangs as the set of languages.
     * @param defaultLang
     * @param inputLangs
     */
    public LanguageSet(Language defaultLang, Collection<Language> inputLangs) {

        if (!inputLangs.contains(defaultLang)) {
            throw new IllegalArgumentException("Attempted to initiate a LanguageSet with a default language" +
                    " that is not in the input collection of languages");
        }

        this.langs = Sets.newHashSet();
        this.langs.addAll(inputLangs);
        this.defaultLanguage = defaultLang;
    }

    /**
     * Creates a LanguageSet instance with an undefined default language
     * @param inputLangs
     */
    public LanguageSet(Collection<Language> inputLangs){
        this(inputLangs.iterator().next(), inputLangs);
    }

    /**
     * Sets the default language.
     * @param newDefaultLanguage
     * @throws WikapidiaException If the input default language is not in the language set.
     */
    public void setDefaultLanguage(Language newDefaultLanguage) throws WikapidiaException {

        if (!langs.contains(newDefaultLanguage)) {
            throw new WikapidiaException(String.format("Attempted to make %s a default language, " +
                    "but it is not in the language set: %s", newDefaultLanguage.getLangCode(), this.toString()));
        }

        this.defaultLanguage = newDefaultLanguage;

    }

    public Language getDefaultLanguage() {
        return defaultLanguage;
    }

    public Set<Language> getLanguages() {
        return Collections.unmodifiableSet(langs);
    }

    public int getNumberOfLanguages(){
        return langs.size();
    }

    public boolean containsLanguage(Language language){
        return langs.contains(language);
    }


    public boolean equals(Object o){
        if (o instanceof LanguageSet){
            String myString = this.toString();
            String theirString = ((LanguageSet)o).toString();
            return (myString.equals(theirString));
        }
        return false;
    }

    public String getLangCodeString() {
        List<String> output = Lists.newArrayList();
        for (Language lang : langs) {
            if (lang.equals(defaultLanguage)) {
                output.add(lang.getLangCode().toUpperCase());
            } else {
                output.add(lang.getLangCode());
            }
        }
        Collections.sort(output);
        return StringUtils.join(output, ",");
    }

    public List<String> getLangCodes() {
        List<String> output = Lists.newArrayList();
        for (Language lang : langs) {
            output.add(lang.getLangCode());
        }
        return output;
    }

    @Override
    public String toString(){
        return "(" + getLangCodeString() + ")";

    }

    @Override
    public Iterator<Language> iterator() {
        return langs.iterator();
    }

    private static Collection<Language> getLangsFromCodes(Collection<String> langCodes) {
        Collection<Language> languages = new ArrayList<Language>();
        for (String langCode : langCodes) {
            languages.add(Language.getByLangCode(langCode));
        }
        return languages;
    }
}
