package org.wikapidia.core.lang;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * A language associated with a language edition of Wikipedia.
 * The set of all Languages is loaded when the system starts up from a text file.
 * Languages can be queried by langCode or id.
 */
public class Language implements Comparable<Language>{
    private static Logger LOG = Logger.getLogger(Language.class.getName());
    public static final String LANGUAGE_TSV = "languages.tsv";

    /**
     * Languages is immediately initialized based on the languages.tsv.
     */
    public static Language[] LANGUAGES;

    static {
        InputStream stream = null;
        try {
            stream = Language.class.getClassLoader()
                    .getResourceAsStream(LANGUAGE_TSV);
            loadAllLanguages(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);  // What else can we do?
        } finally {
            if (stream != null) IOUtils.closeQuietly(stream);
        }
    }

    private final short id;
    private final String langCode;
    private final String enLangName;
    private final String nativeName;
    private Locale locale;

    private Language(short id, String langCode, String enLangName, String nativeName) {
        this.id = id;
        this.langCode = langCode;
        this.enLangName = enLangName;
        this.nativeName = nativeName;
    }

    public short getId() {
        return id;
    }

    public String getLangCode() {
        return langCode;
    }

    public String getEnLangName() {
        return enLangName;
    }

    public String getNativeName() {
        return nativeName;
    }

    public Locale getLocale() {
        if (locale != null) {
            return locale;
        }
        synchronized (this) {
            if (locale == null){
                locale = new Locale(langCode);
            }
        }
        return locale;
    }

    /**
     * @param langCode langCode, such as "en"
     * @return associated language
     * @throws IllegalArgumentException if langCode is unknown.
     */
    public static Language getByLangCode(String langCode) {
        for (Language lang : LANGUAGES) {
            if (lang.langCode.equalsIgnoreCase(langCode)) {
                return lang;
            }
        }
        throw new IllegalArgumentException("unknown langCode: '" + langCode + "'");
    }

    /**
     * @param id numeric id associated with the language
     * @return associated language
     * @throws IllegalArgumentException if id is unknown.
     */
    public static Language getById(int id) {
        if (0 < id && id <= LANGUAGES.length) {
            return LANGUAGES[id-1];
        } else {
            throw new IllegalArgumentException("unknown language id: '" + id + "'");
        }
    }

    /**
     * Loads the languages from the text file.
     */
    static private void loadAllLanguages(InputStream stream) throws IOException {
        List<String> lines = IOUtils.readLines(stream, "UTF-8");
        String header = lines.get(0);
        lines = lines.subList(1, lines.size());
        if (header.equals("id\tlangCode\tenLangName\tnativeName")) {
            LANGUAGES = new Language[lines.size()];
            for (int i = 0; i < lines.size(); i++) {
                String[] cols = StringUtils.splitPreserveAllTokens(lines.get(i), "\t");
                short id = Short.parseShort(cols[0]);
                //int id = Integer.parseInt(cols[0]);
                if (id != i+1) {
                    throw new IOException("expected language id " + (i+1) + ", but got " + id);
                }
                LANGUAGES[i] = new Language(id, cols[1], cols[2], cols[3]);
            }
            LOG.info("loaded " + LANGUAGES.length + " languages");
        } else {
            throw new IOException("invalid header in languages.tsv: " + header);
        }
    }

    @Override
    public int compareTo(Language language) {
        return Short.valueOf(this.id).compareTo(language.id);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Language) {
            Language input = (Language)o;
            return (this.getId()==input.getId());
        } else {
            return false;
        }
    }
}
