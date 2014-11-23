package org.wikibrain.core.lang;

import gnu.trove.set.TByteSet;
import gnu.trove.set.hash.TByteHashSet;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.wikibrain.conf.Configuration;

import java.util.*;

/**
 * @author Ari Weiland
 */
public class TestLangStorage {

//    @Test
//    public void buildStuff() throws IOException {
//        File file = new File("src/main/resources/tmp.txt");
//        file.createNewFile();
//        List<String> lines = new ArrayList<String>();
//        for (int i=0; i<LanguageSet.ALL.size(); i++) {
//            String lang = "LANG_" + i;
//            lines.add("Tables.UNIVERSAL_SKELETAL_LINK." + lang + ",");
//        }
//        FileUtils.writeLines(file, lines, "\n");
//
//        file = new File("src/main/resources/db/universal-skeletal-link-create-tables.sql");
//        lines = new ArrayList<String>();
//        for (int i=0; i<LanguageSet.ALL.size(); i++) {
//            String lang = "lang_" + i;
//            lines.add(lang + " BOOLEAN NOT NULL,");
//        }
//        FileUtils.writeLines(file, lines, "\n", true);
//    }

    private static final int TOTAL_LANGUAGES = LanguageSet.ALL.size();

    @Test
    public void test() {
        LanguageSet languages = new LanguageSet(new Configuration().get().getStringList("languages.big-economies.langCodes"));
        byte[] bits = toByteArray(languages);
        LanguageSet output = getLanguageSet(bits);
        assert languages.equals(output);

        System.out.println();

        for (int j=0; j<285; j++) {
            List<Language> langs = new ArrayList<Language>();
            for (int i=0; i <= j; i++) {
                langs.add(Language.getById(new Random().nextInt(TOTAL_LANGUAGES) + 1));
            }
            languages = new LanguageSet(langs);
            bits = toByteArray(languages);
            output = getLanguageSet(bits);
            assert languages.equals(output);
        }
    }

    public static byte[] toByteArray(LanguageSet langs) {
        TByteSet byteSet = new TByteHashSet();
        Set<byte[]> extras = new HashSet<byte[]>();
        for (Language l : langs) {
            short id = l.getId();
            if (id < 256) {
                // id-1 because id ranges from 1 to >256 but byte ranges from -128 to 127
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
                languages.add(Language.getById(b+128));
            }
        }
        return new LanguageSet(languages);
    }


    public static byte[] toByteBits(LanguageSet languages) {
        int index = 0;
        // 8 is the number of bits per byte
        byte[] langBits = new byte[TOTAL_LANGUAGES/8 + 1];
        Arrays.fill(langBits, (byte) 0x0);
        for (int i=1; i <= TOTAL_LANGUAGES; i++) {
            byte temp = langBits[index];
            temp = (byte) (temp << 1);
            if (languages.containsLanguage(Language.getById(i))) {
                temp = (byte) (temp | (byte) 0x1);
            }
            langBits[index] = temp;
            if (i%8 == 0) {
                index++;
            }
        }
        return langBits;
    }

//    public static LanguageSet getLanguageSet(byte[] langBits) {
//        // 8 is the number of bits per int
//        if (langBits.length != TOTAL_LANGUAGES/8 + 1) {
//            throw new IllegalArgumentException();
//        }
//        byte[] copy = Arrays.copyOf(langBits, langBits.length);
//        List<Language> languages = new ArrayList<Language>();
//        int index = copy.length - 1;
//        for (int i=TOTAL_LANGUAGES; i > 0; i--) {
//            if (i%8 == 0) {
//                index--;
//            }
//            byte temp = copy[index];
//            if ((temp & 0x1) == 1) {
//                languages.add(Language.getById(i));
//            }
//            temp = (byte) (temp >> 1);
//            copy[index] = temp;
//        }
//        return new LanguageSet(languages);
//    }

    public static short[] toShortBits(LanguageSet languages) {
        int index = 0;
        // 16 is the number of bits per byte
        short[] langBits = new short[TOTAL_LANGUAGES/16 + 1];
        Arrays.fill(langBits, (short) 0x0);
        for (int i=1; i <= TOTAL_LANGUAGES; i++) {
            short temp = langBits[index];
            temp = (short) (temp << 1);
            if (languages.containsLanguage(Language.getById(i))) {
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

    public static int[] toIntBits(LanguageSet languages) {
        int index = 0;
        // 32 is the number of bits per byte
        int[] langBits = new int[TOTAL_LANGUAGES/32 + 1];
        Arrays.fill(langBits, 0x0);
        for (int i=1; i <= TOTAL_LANGUAGES; i++) {
            int temp = langBits[index];
            temp = temp << 1;
            if (languages.containsLanguage(Language.getById(i))) {
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

    public static long[] toLongBits(LanguageSet languages) {
        int index = 0;
        // 64 is the number of bits per byte
        long[] langBits = new long[TOTAL_LANGUAGES/64 + 1];
        Arrays.fill(langBits, 0x0L);
        for (int i=1; i <= TOTAL_LANGUAGES; i++) {
            long temp = langBits[index];
            temp = temp << 1;
            if (languages.containsLanguage(Language.getById(i))) {
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
}
