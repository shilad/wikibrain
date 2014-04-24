package org.wikibrain.parser;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;

public class TestDumpSplitter {
    public static final File EN_DUMP = new File("src/test/resources/org/wikibrain/parser/en_test.xml");

    @Test
    public void testSimple() {
        DumpSplitter ds = new DumpSplitter(EN_DUMP);
        int i = 0;
        for (String text : ds) {
            i++;
        }
        assertEquals(i, 44);
    }

    @Test
    public void testText() {
        int i = 0;
        DumpSplitter ds = new DumpSplitter(EN_DUMP);
        for (String text : ds) {
            switch (i) {
            case 0:
                assertTrue(text.startsWith(
                    "  <page>\n" +
                    "    <title>Potential difference</title>\n" +
                    "    <ns>0</ns>\n"
                ));
                assertTrue(text.endsWith(
                    "      <format>text/x-wiki</format>\n" +
                    "    </revision>\n" +
                    "  </page>\n"
                ));
                break;
            case 1:
                assertTrue(text.startsWith(
                    "  <page>\n" +
                    "    <title>Pretoria</title>\n" +
                    "    <ns>0</ns>\n" +
                    "    <id>25002</id>\n"
                ));
                assertTrue(text.endsWith(
                    "      <sha1>mszr60y41wz467aw14sa206nu0f1dwq</sha1>\n" +
                    "      <model>wikitext</model>\n" +
                    "      <format>text/x-wiki</format>\n" +
                    "    </revision>\n" +
                    "  </page>\n"
                ));
                break;
            case 43:
                assertTrue(text.startsWith(
                    "  <page>\n" +
                    "    <title>Product of rings</title>\n" +
                    "    <ns>0</ns>\n" +
                    "    <id>25063</id>\n"
                ));
                assertTrue(text.endsWith(
                    "      <sha1>4iqu0kkt3gqipmxin4pxmj0m5qthrxg</sha1>\n" +
                    "      <model>wikitext</model>\n" +
                    "      <format>text/x-wiki</format>\n" +
                    "    </revision>\n" +
                    "  </page>\n"
                ));
                break;
            }
            i++;
        }
    }
}
