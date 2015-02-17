package org.wikibrain.parser;

import com.akiban.sql.StandardException;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.wikibrain.parser.sql.MySqlDumpParser;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 */
public class TestMysqlDumpParser {
    public static final File LINK_DUMP = new File("src/test/resources/org/wikibrain/parser/pagelinks.sql");

    @Test
    public void test() {
        MySqlDumpParser parser = new MySqlDumpParser();
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
        MySqlDumpParser parser = new MySqlDumpParser();
        List<Object[]> rows = parser.parse(lastLine);
        assertEquals(rows.size(), 437);
    }
}
