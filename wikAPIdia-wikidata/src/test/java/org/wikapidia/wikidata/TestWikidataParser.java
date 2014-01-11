package org.wikapidia.wikidata;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.parser.WpParseException;
import org.wikapidia.parser.xml.PageXmlParser;
import org.wikapidia.utils.WpIOUtils;

import java.io.*;

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
        WikidataRawRecord record = parser2.parse(rawPage);
    }

    @Test
    public void testDump() throws IOException, WpParseException {
        File tmp = File.createTempFile("wikapidia", "dump.xml.bz2");
        try {
            tmp.deleteOnExit();
            InputStream in = TestWikidataParser.class.getResourceAsStream("/testDump.xml.bz2");
            OutputStream out = new FileOutputStream(tmp);
            IOUtils.copy(in, out);
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
            WikidataDumpParser parser = new WikidataDumpParser(tmp);
            int i = 0;
            for (WikidataRawRecord record : parser) {
                i++;
            }
            System.out.println("I is " + i);
        } finally {
            tmp.delete();
        }
    }
}
