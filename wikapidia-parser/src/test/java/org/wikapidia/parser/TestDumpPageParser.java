package org.wikapidia.parser;

import org.junit.Test;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;

import java.io.File;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestDumpPageParser {
    public static final File EN_DUMP = new File("src/test/resources/org/wikapidia/parser/en_test.xml");
    public static final LanguageInfo EN = LanguageInfo.getByLangCode("en");

    @Test
    public void testSimple() {
        DumpPageParser dpp = new DumpPageParser(EN_DUMP, EN);
        int i = 0;
        for (PageXml xml : dpp) {
            i++;
        }
        assertEquals(i, 44);
    }

    @Test
    public void testText() {
        int i = 0;
        DumpPageParser dpp = new DumpPageParser(EN_DUMP, EN);
        for (PageXml xml : dpp) {
            switch (i) {
            case 0:
                assertEquals(xml.getTitle(), "Potential difference");
                assertEquals(xml.getPageId(), 25001);
                assertEquals(xml.getRevisionId(), 301899471);
                assertEquals(xml.getLastEdit(), new Date(109, 6, 13, 18, 8, 34));
                assertEquals(xml.getBody(), "#REDIRECT [[voltage]]");
                break;
            case 43:
                assertEquals(xml.getTitle(), "Product of rings");
                assertEquals(xml.getPageId(), 25063);
                assertEquals(xml.getRevisionId(), 540215174);
                assertTrue(xml.getBody().contains("''R''<sub>''i''")); // check escaping
                break;
            }
            i++;
        }
    }
}
