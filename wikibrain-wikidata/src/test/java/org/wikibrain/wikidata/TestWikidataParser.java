package org.wikibrain.wikidata;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.parser.WpParseException;
import org.wikibrain.parser.xml.PageXmlParser;
import org.wikibrain.utils.WpIOUtils;

import java.io.*;
import java.text.ParseException;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * @author Shilad Sen
 */
public class TestWikidataParser {

    @Test
    public void testWikidataRawRecord() throws IOException, WpParseException {
        String xml = WpIOUtils.resourceToString("/testPage.xml");
        PageXmlParser parser = new PageXmlParser(LanguageInfo.getByLangCode("en"));
        RawPage rawPage = parser.parse(xml);
        assertEquals("application/json", rawPage.getFormat());
        assertEquals("wikibase-item", rawPage.getModel());

        WikidataParser parser2 = new WikidataParser();
        WikidataEntity record = parser2.parse(rawPage);

    }

    @Test
    public void testDump() throws IOException, WpParseException {
        File tmp = File.createTempFile("wikibrain", "dump.xml.bz2");
        try {
            tmp.deleteOnExit();
            InputStream in = TestWikidataParser.class.getResourceAsStream("/testDump.xml.bz2");
            OutputStream out = new FileOutputStream(tmp);
            IOUtils.copy(in, out);
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);

            WikidataDumpParser parser = new WikidataDumpParser(tmp);
            int numItems = 0;
            int numProperties = 0;
            for (WikidataEntity record : parser) {
                if (record.getType() == WikidataEntity.Type.ITEM) {
                    numItems++;
                } else if (record.getType() == WikidataEntity.Type.PROPERTY) {
                    numProperties++;
                }
            }
            assertEquals(400, numItems);
            assertEquals(836, numProperties);
        } finally {
            tmp.delete();
        }
    }

    @Test
    public void testDateParser() throws ParseException {
        String s = "+00000001996-12-20T00:00:00Z";
        Date d = DateUtils.parseDate(s, "'+0000000'yyyy-MM-dd'T'HH:mm:ss'Z'");
    }
}
