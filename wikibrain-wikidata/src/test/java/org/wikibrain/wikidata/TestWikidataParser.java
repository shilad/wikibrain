package org.wikibrain.wikidata;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.wikibrain.core.lang.Language;
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
        String json = WpIOUtils.resourceToString("/testPage.json");
        WikidataParser parser = new WikidataParser();
        WikidataEntity entity = parser.parse(json);
        assertEquals(entity.getType(), WikidataEntity.Type.ITEM);
        assertEquals(entity.getId(), 157);
        assertEquals(entity.getLabels().get(Language.ES), "Fran\u00e7ois Hollande");
        assertEquals(entity.getDescriptions().get(Language.EN), "24th President of the French Republic");
        WikidataStatement stm = entity.getStatements().get(0);
        assertEquals(stm.getProperty().getId(), 40);
        assertEquals(stm.getValue().getType(), WikidataValue.Type.ITEM);
        assertEquals(stm.getValue().getItemValue(), 16783695);
    }

    @Test
    public void testDump() throws IOException, WpParseException {
        File tmp = File.createTempFile("wikibrain", "dump.json.bz2");
        try {
            tmp.deleteOnExit();
            InputStream in = TestWikidataParser.class.getResourceAsStream("/testDump.json.bz2");
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
            assertEquals(1304, numProperties);
        } finally {
            tmp.delete();
        }
    }
}
