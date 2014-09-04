package org.wikibrain.integration;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.download.RequestedLinkGetter;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Shilad Sen
 */
public class DownloadIT {

    @Ignore
    @Test
    public void testLinkGetter() throws ConfigurationException, WikiBrainException, ParseException, IOException {
        RequestedLinkGetter.main(TestUtils.getArgs());
        File f = new File("../integration-tests/download/list.tsv");
        assertTrue(f.isFile());

        List<String> lines = FileUtils.readLines(f);
        assertEquals(5, lines.size());

        for (Language lang : new LanguageSet("simple,la")) {
            String match = null;
            for (String line : lines) {
                if (line.startsWith(lang.getLangCode())) {
                    match = line;
                    break;
                }
            }
            assertNotNull(match);
            assertTrue(match.contains("http://dumps.wikimedia.org/" + lang.getLangCode() + "wiki/"));
        }
    }
}
