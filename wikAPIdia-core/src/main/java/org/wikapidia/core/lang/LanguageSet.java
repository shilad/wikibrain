package org.wikapidia.core.lang;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.wikapidia.core.WikapidiaException;

import java.math.BigInteger;
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

    public byte[] toByteBits() {
        int index = 0;
        // 8 is the number of bits per byte
        byte[] langBits = new byte[TOTAL_LANGUAGES/8 + 1];
        Arrays.fill(langBits, (byte) 0x0);
        for (int i=1; i <= TOTAL_LANGUAGES; i++) {
            byte temp = langBits[index];
            temp = (byte) (temp << 1);
            if (containsLanguage(Language.getById(i))) {
                temp = (byte) (temp | (byte) 0x1);
            }
            langBits[index] = temp;
            if (i%8 == 0) {
                index++;
            }
        }
        return langBits;
    }

    public static LanguageSet getLanguageSet(byte[] langBits) {
        // 8 is the number of bits per int
        if (langBits.length != TOTAL_LANGUAGES/8 + 1) {
            throw new IllegalArgumentException();
        }
        byte[] copy = Arrays.copyOf(langBits, langBits.length);
        List<Language> languages = new ArrayList<Language>();
        int index = copy.length - 1;
        for (int i=TOTAL_LANGUAGES; i > 0; i--) {
            if (i%8 == 0) {
                index--;
            }
            byte temp = copy[index];
            if ((temp & 0x1) == 1) {
                languages.add(Language.getById(i));
            }
            temp = (byte) (temp >> 1);
            copy[index] = temp;
        }
        return new LanguageSet(languages);
    }

    public short[] toShortBits() {
        int index = 0;
        // 16 is the number of bits per byte
        short[] langBits = new short[TOTAL_LANGUAGES/16 + 1];
        Arrays.fill(langBits, (short) 0x0);
        for (int i=1; i <= TOTAL_LANGUAGES; i++) {
            short temp = langBits[index];
            temp = (short) (temp << 1);
            if (containsLanguage(Language.getById(i))) {
                temp = (short) (temp | (short) 0x1);
            }
            langBits[index] = temp;
            if (i%16 == 0) {
                index++;
            }
        }
        return langBits;
    }

    public static LanguageSet getLanguageSet(short[] langBits) {
        // 16 is the number of bits per int
        if (langBits.length != TOTAL_LANGUAGES/16 + 1) {
            throw new IllegalArgumentException();
        }
        short[] copy = Arrays.copyOf(langBits, langBits.length);
        List<Language> languages = new ArrayList<Language>();
        int index = copy.length - 1;
        for (int i=TOTAL_LANGUAGES; i > 0; i--) {
            if (i%16 == 0) {
                index--;
            }
            short temp = copy[index];
            if ((temp & 0x1) == 1) {
                languages.add(Language.getById(i));
            }
            temp = (short) (temp >> 1);
            copy[index] = temp;
        }
        return new LanguageSet(languages);
    }

    public int[] toIntBits() {
        int index = 0;
        // 32 is the number of bits per byte
        int[] langBits = new int[TOTAL_LANGUAGES/32 + 1];
        Arrays.fill(langBits, 0x0);
        for (int i=1; i <= TOTAL_LANGUAGES; i++) {
            int temp = langBits[index];
            temp = temp << 1;
            if (containsLanguage(Language.getById(i))) {
                temp = temp | 0x1;
            }
            langBits[index] = temp;
            if (i%32 == 0) {
                index++;
            }
        }
        return langBits;
    }

    public static LanguageSet getLanguageSet(int[] langBits) {
        // 32 is the number of bits per int
        if (langBits.length != TOTAL_LANGUAGES/32 + 1) {
            throw new IllegalArgumentException();
        }
        int[] copy = Arrays.copyOf(langBits, langBits.length);
        List<Language> languages = new ArrayList<Language>();
        int index = copy.length - 1;
        for (int i=TOTAL_LANGUAGES; i > 0; i--) {
            if (i%32 == 0) {
                index--;
            }
            int temp = copy[index];
            if ((temp & 0x1) == 1) {
                languages.add(Language.getById(i));
            }
            temp = temp >> 1;
            copy[index] = temp;
        }
        return new LanguageSet(languages);
    }

    public long[] toLongBits() {
        int index = 0;
        // 64 is the number of bits per byte
        long[] langBits = new long[TOTAL_LANGUAGES/64 + 1];
        Arrays.fill(langBits, 0x0L);
        for (int i=1; i <= TOTAL_LANGUAGES; i++) {
            long temp = langBits[index];
            temp = temp << 1;
            if (containsLanguage(Language.getById(i))) {
                temp = temp | 0x1L;
            }
            langBits[index] = temp;
            if (i%64 == 0) {
                index++;
            }
        }
        return langBits;
    }

    public static LanguageSet getLanguageSet(long[] langBits) {
        // 64 is the number of bits per int
        if (langBits.length != TOTAL_LANGUAGES/64 + 1) {
            throw new IllegalArgumentException();
        }
        long[] copy = Arrays.copyOf(langBits, langBits.length);
        List<Language> languages = new ArrayList<Language>();
        int index = copy.length - 1;
        for (int i=TOTAL_LANGUAGES; i > 0; i--) {
            if (i%64 == 0) {
                index--;
            }
            long temp = copy[index];
            if ((temp & 0x1L) == 1) {
                languages.add(Language.getById(i));
            }
            temp = temp >> 1;
            copy[index] = temp;
        }
        return new LanguageSet(languages);
    }

    public BitSet toBitSet() {
        BitSet bits = new BitSet(TOTAL_LANGUAGES);
        for (int i=0; i < TOTAL_LANGUAGES; i++) {
            bits.set(i, containsLanguage(Language.getById(i+1)));
        }
        return bits;
    }

    public static LanguageSet getLanguageSet(BitSet bits) {
        List<Language> languages = new ArrayList<Language>();
        for (int i=0; i < TOTAL_LANGUAGES; i++) {
            if (bits.get(i)) {
                languages.add(Language.getById(i+1));
            }
        }
        return new LanguageSet(languages);
    }

    private static Collection<Language> getLangsFromCodes(Collection<String> langCodes) {
        Collection<Language> languages = new ArrayList<Language>();
        for (String langCode : langCodes) {
            languages.add(Language.getByLangCode(langCode));
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
}
