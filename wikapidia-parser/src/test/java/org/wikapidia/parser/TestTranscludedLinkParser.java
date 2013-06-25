package org.wikapidia.parser;

import com.akiban.sql.StandardException;
import com.akiban.sql.parser.SQLParser;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.parser.sql.TranscludedLinkParser;
import org.wikapidia.parser.wiki.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 */
public class TestTranscludedLinkParser {
    public static final File LINK_DUMP = new File("src/test/resources/org/wikapidia/parser/pagelinks.sql");

    @Test
    public void test() {
        TranscludedLinkParser parser = new TranscludedLinkParser();
        int i = 0;
        for (TranscludedLinkParser.RawLink link : parser.parse(LINK_DUMP)) {
            i++;
        }
        assertEquals(i, 1003);
    }

    @Test
    public void testOne() throws IOException, StandardException {
        List<String> lines = FileUtils.readLines(LINK_DUMP, "UTF-8");
        String lastLine = lines.get(lines.size() - 1);
        TranscludedLinkParser parser = new TranscludedLinkParser();
        List<TranscludedLinkParser.RawLink> links = parser.parse(lastLine);

    }
}
