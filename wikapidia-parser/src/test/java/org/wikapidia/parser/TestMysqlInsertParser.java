package org.wikapidia.parser;

import com.akiban.sql.StandardException;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.wikapidia.parser.sql.MySqlInsertParser;
import org.wikapidia.parser.sql.TranscludedLinkParser;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 */
public class TestMysqlInsertParser {
    public static final File LINK_DUMP = new File("src/test/resources/org/wikapidia/parser/pagelinks.sql");

    @Test
    public void test() {
        MySqlInsertParser parser = new MySqlInsertParser();
        int i = 0;
        for (Object [] column : parser.parse(LINK_DUMP)) {
            assertEquals(column.length, 3);
            assert(column[0] instanceof Integer);
            assert(column[1] instanceof Integer);
            assert(column[2] instanceof String);
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
