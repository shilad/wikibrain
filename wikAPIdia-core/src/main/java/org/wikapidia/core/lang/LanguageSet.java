package org.wikapidia.core.lang;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.typesafe.config.Config;
import gnu.trove.set.TByteSet;
import gnu.trove.set.hash.TByteHashSet;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.Provider;
import org.wikapidia.core.WikapidiaException;

import java.util.*;

/**
 * User: bjhecht
 */
public class LanguageSet implements Iterable<Language> {
    public static final LanguageSet ALL = new LanguageSet(
            Language.getByLangCode("en"),
            Arrays.asList(Language.LANGUAGES));
    public static final int TOTAL_LANGUAGES = ALL.size();

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
        this(getLangsFromCodes(langCodes));
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
        List<Language> temp = new ArrayList<Language>(inputLangs);
        Collections.sort(temp);
        return temp.iterator().next();
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

    public int size() {
        return langs.size();
    }

    public boolean containsLanguage(Language language){
        return langs.contains(language);
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

    static class Provider extends org.wikapidia.conf.Provider<LanguageSet> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class<LanguageSet> getType() {
            return LanguageSet.class;
        }

        @Override
        public String getPath() {
            return "languages";
        }

        @Override
        public LanguageSet get(String name, Config config) throws ConfigurationException {
            return new LanguageSet(getConfig().get().getStringList("languages"));
        }
    }
}
