package org.wikibrain.parser;

import org.junit.Test;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.parser.xml.DumpPageXmlParser;
import org.wikibrain.core.model.RawPage;

import java.io.File;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestDumpPageParser {

    public static final File EN_DUMP = new File("src/test/resources/org/wikibrain/parser/en_test.xml");
    public static final LanguageInfo EN = LanguageInfo.getByLangCode("en");

    @Test
    public void testSimple() {
        DumpPageXmlParser dpp = new DumpPageXmlParser(EN_DUMP, EN);
        int i = 0;
        for (RawPage xml : dpp) {
            i++;
        }
        assertEquals(i, 44);
    }

    @Test
    public void testText() {
        int i = 0;
        DumpPageXmlParser dpp = new DumpPageXmlParser(EN_DUMP, EN);
        for (RawPage xml : dpp) {
            switch (i) {
            case 0:
                assertEquals(xml.getTitle().getCanonicalTitle(), "Potential difference");
                assertEquals(xml.getLocalId(), 25001);
                assertEquals(xml.getRevisionId(), 301899471);
                assertEquals(xml.getLastEdit(), new Date(109, 6, 13, 18, 8, 34));
                assertEquals(xml.getBody(), "#REDIRECT [[voltage]]");
                break;
            case 43:
                assertEquals(xml.getTitle().getCanonicalTitle(), "Product of rings");
                assertEquals(xml.getLocalId(), 25063);
                assertEquals(xml.getRevisionId(), 540215174);
                assertTrue(xml.getBody().contains("''R''<sub>''i''")); // check escaping
                break;
            }
            i++;
        }
    }
}
