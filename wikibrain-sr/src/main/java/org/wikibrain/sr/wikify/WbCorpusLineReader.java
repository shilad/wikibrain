package org.wikibrain.sr.wikify;

import org.apache.commons.io.IOUtils;
import org.wikibrain.core.lang.Language;
import org.wikibrain.utils.WpIOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author Shilad Sen
 */
public class WbCorpusLineReader implements Iterable<WbCorpusLineReader.Line> {
    private final File path;

    public WbCorpusLineReader(File path) {
        this.path = path;
    }

    @Override
    public Iterator<Line> iterator() {
        try {
            return new WBCorpusLineIterator(path);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not open: " + path, e);
        }
    }

    public static class CorpusInfo {
        private final Language language;
        private final String corpusClass;
        private final String wikifierClass;
        private final String creationTime;

        public CorpusInfo(String line) {
            if (!line.startsWith("@WikiBrainCorpus")) {
                throw new IllegalArgumentException("Invalid corpus line: " + line);
            }
            String tokens[] = line.split("\t");
            if (tokens.length != 5) {
                throw new IllegalArgumentException("Invalid corpus line: " + line);
            }
            language = Language.getByLangCode(tokens[1]);
            corpusClass = tokens[2].trim();
            wikifierClass = tokens[3].trim();
            creationTime = tokens[4].trim();
        }

        public Language getLanguage() {
            return language;
        }

        public String getCorpusClass() {
            return corpusClass;
        }

        public String getWikifierClass() {
            return wikifierClass;
        }

        public String getCreationTime() {
            return creationTime;
        }
    }

    public static class DocInfo {
        private final int id;
        private final String title;
        int lineCounter = 0;
        int charCounter = 0;

        public DocInfo(String line) {
            if (!line.startsWith("@WikiBrainDoc")) {
                throw new IllegalArgumentException("Invalid doc line: " + line);
            }
            String tokens[] = line.split("\t");
            if (tokens.length != 3) {
                throw new IllegalArgumentException("Invalid corpus line: " + line);
            }
            id = Integer.valueOf(tokens[1].trim());
            title = tokens[2].trim();
        }

        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return id == ((DocInfo)o).id;
        }

        @Override
        public int hashCode() { return id; }

        @Override
        public String toString() {
            return "DocInfo{" + "id=" + id + ", title='" + title + '\'' + '}';
        }
    }

    public static class Line {
        private final CorpusInfo corpus;
        private final DocInfo doc;
        private final String line;
        private final int lineNumber;
        private final int charNumber;

        public Line(CorpusInfo corpus, DocInfo doc, String line, int lineNumber, int charNumber) {
            this.corpus = corpus;
            this.doc = doc;
            this.line = line;
            this.lineNumber = lineNumber;
            this.charNumber = charNumber;
        }

        public CorpusInfo getCorpus() {
            return corpus;
        }

        public DocInfo getDoc() {
            return doc;
        }

        public int getDocId() {
            return doc.getId();
        }

        public String getTitle() {
            return doc.getTitle();
        }

        public String getLine() {
            return line;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public int getCharNumber() {
            return charNumber;
        }
    }

    public static class WBCorpusLineIterator implements Iterator<Line> {
        private final File path;
        private BufferedReader reader;
        private CorpusInfo corpus;
        private DocInfo doc;
        private String line;

        public WBCorpusLineIterator(File path) throws IOException {
            this.path = path;
            reader = WpIOUtils.openBufferedReader(path);
        }

        @Override
        public synchronized boolean hasNext() {
            if (reader == null && line == null) {
                return false;
            }
            boolean success = false;
            try {
                advanceIfNecessary();
                success = true;
                return line != null;
            } finally {
                if (!success) close();
            }
        }

        @Override
        public synchronized Line next() {
            if (reader == null && line == null) {
                return null;
            }
            boolean success = false;
            try {
                advanceIfNecessary();
                success = true;
                if (line == null) {
                    return null;
                } else {
                    int lineNum = doc.lineCounter++;
                    int charNum = doc.charCounter;
                    doc.charCounter += line.length();
                    Line res = new Line(corpus, doc, line, lineNum, charNum);
                    line = null;    // consume buffer
                    return res;
                }
            } finally {
                if (!success) close();
            }
        }

        @Override
        public void remove() { throw new UnsupportedOperationException(); }

        private synchronized void advanceIfNecessary() {
            if (line == null) {
                if (reader == null) throw new IllegalStateException();
                while (line == null) {
                    String s = null;
                    try {
                        s = reader.readLine();
                    } catch (IOException e) {
                        throw new IllegalStateException("Unexpected IO Exception: ", e);
                    }
                    if (s == null) {
                        close();
                        return;
                    }
                    // Ignore blank lines.
                    if (s.trim().isEmpty()) {
                        continue;
                    }
                    if (s.startsWith("@WikiBrainCorpus")) {
                        corpus = new CorpusInfo(s);
                    } else if (corpus == null) {
                        throw new IllegalStateException("Did not find corpus header in first line of " + path);
                    } else if (s.startsWith("@WikiBrainDoc")) {
                        doc = new DocInfo(s);
                    } else if (doc == null) {
                        throw new IllegalStateException("Did not find doc header in second line of " + path);
                    } else {
                        line = s;
                    }
                }
            }
        }

        public synchronized void close() {
            if (reader != null) {
                IOUtils.closeQuietly(reader);
                reader = null;
            }
        }

        private void ensureNotUsedUp() {
        }

    }
}
