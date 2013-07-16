package org.wikapidia.core.lang;

import org.junit.Test;
import org.wikapidia.conf.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Ari Weiland
 */
public class TestLangStorage {

    private static final int TOTAL_LANGS = LanguageSet.ALL.size();
    private static final List<String> LANG_CODES = LanguageSet.ALL.getLangCodes();
    static {
        Collections.sort(LANG_CODES);
    }

    @Test
    public void test() {
//        int largest = Integer.MAX_VALUE;
//        System.out.println(largest);
//        System.out.println(largest+1);
//        System.out.println(largest+10000000);
        System.out.println(TOTAL_LANGS);

        LanguageSet languages = new LanguageSet(new Configuration().get().getStringList("languages"));
        System.out.println(languages);
        int[] langBits = getLangBits(languages);
        for (int temp : langBits) {
            System.out.print(Integer.toBinaryString(temp));
        }
        System.out.println();
        System.out.println(getLanguageSet(langBits));
    }

    public int[] getLangBits(LanguageSet languages) {
        int index = 0;
        int[] langBits = new int[TOTAL_LANGS/32 + 1];
        Arrays.fill(langBits, 0x0000);
        for (int i=0; i<TOTAL_LANGS; i++) {
            int temp = langBits[index];
            temp = temp << 1;
            if (languages.containsLanguage(Language.getByLangCode(LANG_CODES.get(i)))) {
                temp = temp | 0x0001;
            }
            langBits[index] = temp;
            if ((i+1)%32 == 0) {
                index++;
            }
        }
        return langBits;
    }

    public LanguageSet getLanguageSet(int[] langBits) {
        if (langBits.length != TOTAL_LANGS/32 + 1) {
            throw new IllegalArgumentException();
        }
        List<String> languages = new ArrayList<String>();
        int index = langBits.length - 1;
        for (int i = TOTAL_LANGS - 1; i >= 0; i--) {
            if ((i+1)%32 == 0) {
                index--;
            }
            int temp = langBits[index];
            if ((temp & 0x0001) == 1) {
                languages.add(LANG_CODES.get(i));
            }
            temp = temp >> 1;
            langBits[index] = temp;
        }
        return new LanguageSet(languages);
    }
}
