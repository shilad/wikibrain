package org.wikibrain.core.lang;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.typesafe.config.Config;
import gnu.trove.set.TByteSet;
import gnu.trove.set.hash.TByteHashSet;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.FileMatcher;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.model.LocalPage;

import java.util.*;

/**
 * Author: bjhecht, shilad
 */
public class LanguageSet implements Iterable<Language> {

    public static final LanguageSet ALL = new LanguageSet(
            Language.getByLangCode("en"),
            Arrays.asList(Language.LANGUAGES));

    private Set<Language> langs;
    private Language defaultLanguage;

    /**
     * Creates the empty language set
     */
    public LanguageSet() {
        this(null, new ArrayList<Language>());
    }

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
        this(getLangsFromCodes(langCodes));
    }

    /**
     * Creates an instance of a language set with defaultLang as the default language and
     * inputLangs as the set of languages.
     * @param defaultLang
     * @param inputLangs
     */
    public LanguageSet(Language defaultLang, Collection<Language> inputLangs) {

        if (defaultLang != null && !inputLangs.contains(defaultLang)) {
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
    public LanguageSet(Collection<Language> inputLangs) {
        this(getDefault(inputLangs), inputLangs);
    }

    /**
     * Creates a LanguageSet instance with a single language
     * @param inputLang
     */
    public LanguageSet(Language inputLang) {
        this(inputLang, Arrays.asList(inputLang));
    }

    private static Language getDefault(Collection<Language> inputLangs) {
        if (inputLangs.isEmpty()) {
            return null;
        }
        List<Language> temp = new ArrayList<Language>(inputLangs);
        Collections.sort(temp);
        return temp.iterator().next();
    }

    /**
     * Sets the default language.
     * @param newDefaultLanguage
     * @throws WikiBrainException If the input default language is not in the language set.
     */
    public void setDefaultLanguage(Language newDefaultLanguage) throws WikiBrainException {

        if (!langs.contains(newDefaultLanguage)) {
            throw new WikiBrainException(String.format("Attempted to make %s a default language, " +
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

    public int size() {
        return langs.size();
    }

    public boolean containsLanguage(Language language){
        return langs.contains(language);
    }

    public boolean containsLanguage(String langCode){
        return langs.contains(Language.getByLangCode(langCode));
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

    public byte[] toByteArray() {
        TByteSet byteSet = new TByteHashSet();
        Set<byte[]> extras = new HashSet<byte[]>();
        for (Language l : langs) {
            short id = l.getId();
            if (id < 256) {
                byteSet.add((byte) (id-128));
            } else {
                byte[] temp = new byte[2];
                temp[0] = (byte) -128;
                temp[1] = (byte) (id-255-128);
                extras.add(temp);
            }
        }
        byte[] output = byteSet.toArray();
        for (byte[] b : extras) {
            output = ArrayUtils.addAll(output, b);
        }
        return output;
    }

    public byte[] toByteArray(int maxSize) {
        byte[] temp = toByteArray();
        return Arrays.copyOf(temp, maxSize < temp.length ? maxSize : temp.length);
    }

    public static LanguageSet getLanguageSet(byte[] truncated) {
        Set<Language> languages = new HashSet<Language>();
        boolean extra = false;
        for (byte b : truncated) {
            if (extra) {
                languages.add(Language.getById(b+128+255));
                extra = false;
            } else if (b == -128) {
                extra = true;
            } else {
                languages.add(Language.getById(b + 128));
            }
        }
        return new LanguageSet(languages);
    }

    /**
     * Returns English if English is in the set, else returns Simple. If Simple is not in the
     * set, will return the default language or throws an exception, depending on the value of returnDefaultLangIfEnglishNotAvailable
     * @return
     * @throws WikiBrainException
     */
    public Language getBestAvailableEnglishLang(boolean returnDefaultLangIfEnglishNotAvailable) throws WikiBrainException {
        if (this.containsLanguage(Language.getByLangCode("en"))){
            return Language.getByLangCode("en");
        }else if (this.containsLanguage(Language.getByLangCode("simple"))){
            return Language.getByLangCode("simple");
        }else{
            if (returnDefaultLangIfEnglishNotAvailable){
                return this.getDefaultLanguage();
            }
            throw new WikiBrainException("No English language available");
        }
    }

    private static Collection<Language> getLangsFromCodes(Collection<String> langCodes) {
        Collection<Language> languages = new ArrayList<Language>();
        for (String langCode : langCodes) {
            languages.add(Language.getByLangCode(langCode.trim()));
        }
        return languages;
    }

    @Override
    public boolean equals(Object o){
        if (o instanceof LanguageSet){
            String myString = this.toString();
            String theirString = o.toString();
            return (myString.equals(theirString));
        }
        return false;
    }

    @Override
    public String toString(){
        return "(" + getLangCodeString() + ")";

    }

    @Override
    public Iterator<Language> iterator() {
        return langs.iterator();
    }

    static class Provider extends org.wikibrain.conf.Provider<LanguageSet> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class<LanguageSet> getType() {
            return LanguageSet.class;
        }

        @Override
        public String getPath() {
            return "languages";   // hack: languages are in the root element
        }

        @Override
        public LanguageSet get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            try {
                String type = config.getString("type");
                if (type.equals("loaded")) {
                    MetaInfoDao miDao = getConfigurator().get(MetaInfoDao.class);
                    return miDao.getLoadedLanguages(LocalPage.class);
                } else if (type.equals("downloaded")) {
                    List<Language> languages = new ArrayList<Language>();
                    // TODO: set the default language reasonably
                    for (Language lang : Language.LANGUAGES) {
                        if (Env.getFiles(lang, FileMatcher.ARTICLES, getConfig()).size() > 0) {
                            languages.add(lang);
                        }
                    }
                    return new LanguageSet(languages);
                } else if (type.equals("custom")) {
                    return new LanguageSet(config.getStringList("langCodes"));
                } else {
                    throw new ConfigurationException("Unknown LanguageSet type: " + type);
                }
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
