package org.wikibrain.sr.wikify;

import org.junit.Test;
import org.wikibrain.core.lang.Language;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Shilad Sen
 */
public class TestWBCorpusLineIterable {
    static final File PATH = new File("src/test/resources/test_corpus.txt.bz2");

    @Test
    public void testBasic() {
        WbCorpusLineReader reader = new WbCorpusLineReader(PATH);
        int numLines = 0;
        List<WbCorpusLineReader.DocInfo> docs = new ArrayList<WbCorpusLineReader.DocInfo>();
        for (WbCorpusLineReader.Line line : reader) {
            assertEquals(line.getCorpus().getLanguage(), Language.SIMPLE);
            if (!docs.contains(line.getDoc())) { docs.add(line.getDoc()); }
            numLines += 1;
            if (numLines == 1) assertEquals(line.getLine(), "Category Asian countries");
            if (numLines == 3) assertEquals(line.getLine(), "It is near Land's_End:/w/simple/-1/Unknown_page");
            if (numLines == 850) assertTrue(line.getLine().startsWith("The same sort of laws:/w/simple/426/Law can"));
        }
        assertEquals(50, docs.size());
        assertEquals(850, numLines);
    }

    @Test
    public void testDoc() {
        WBCorpusDocReader reader = new WBCorpusDocReader(PATH);
        List<WBCorpusDocReader.Doc> docs = new ArrayList<WBCorpusDocReader.Doc>();
        List<String> lines = new ArrayList<String>();
        for (WBCorpusDocReader.Doc doc: reader) {
            assertEquals(doc.getCorpus().getLanguage(), Language.SIMPLE);
            docs.add(doc);
            lines.addAll(doc.getLines());
        }
        assertEquals(9983, docs.get(0).getDoc().getId());
        assertEquals(9464, docs.get(1).getDoc().getId());
        assertEquals(9596, docs.get(49).getDoc().getId());
        assertEquals(50, docs.size());
        assertEquals(850, lines.size());
        assertEquals(lines.get(0), "Category Asian countries");
        assertEquals(lines.get(2), "It is near Land's_End:/w/simple/-1/Unknown_page");
        assertTrue(lines.get(849).startsWith("The same sort of laws:/w/simple/426/Law can"));
    }
}
