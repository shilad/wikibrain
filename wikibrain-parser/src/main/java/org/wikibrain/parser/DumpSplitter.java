package org.wikibrain.parser;

import org.apache.commons.compress.archivers.ArchiveException;
import org.wikibrain.utils.WpIOUtils;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Iterates over a file containing an XML dump of wikipedia.
 * Each string is the contents of a single article.
 * Iterators are independent, so multiple iterators can simultaneously open a dump file.
 */
public class DumpSplitter implements Iterable<String> {
    public static final String ARTICLE_BEGIN = "<page>";
    public static final String ARTICLE_END = "</page>";
    private static final int MAX_ARTICLE_LENGTH = 10000000;     // Maximum length of article


    private static final Logger LOG = LoggerFactory.getLogger(DumpSplitter.class);
    private File path;

    /**
     * Creates an iterator over the given file.
     * The file can be gzipped or bzipped.
     * @param path
     */
    public DumpSplitter(File path) {
        this.path = path;
    }

    public File getPath() {
        return path;
    }

    @Override
    public Iterator<String> iterator() {
        try {
            return new ArticleIterator(path);
        } catch (IOException e) {
            LOG.error("article iterator construction failed", e);
            throw new RuntimeException(e);
        } catch (ArchiveException e) {
            LOG.error("article iterator construction failed", e);
            throw new RuntimeException(e);
        } catch (XMLStreamException e) {
            LOG.error("article iterator construction failed", e);
            throw new RuntimeException(e);
        }
    }

    public class ArticleIterator implements Iterator<String> {

        private BufferedReader reader;
        private String buffer = null;
        private int lineNum = 0;
        private boolean closed = false;

        public ArticleIterator(File path) throws IOException, ArchiveException, XMLStreamException {
            reader = WpIOUtils.openBufferedReader(path);
        }

        private void fillBuffer() {
            if (closed || buffer != null) {
                return;
            }
            try {
                String articleOpen = readToArticleBegin();
                if (articleOpen == null) {
                    return;
                }
                buffer = readToArticleClose(articleOpen);
            } catch (IOException e) {
                logParseError("parser failed", e);
                e.printStackTrace();
            }
        }

        /**
         * Reads until it finds the beginning of an article.
         * @return the line with the beginning tag.
         * @throws IOException
         */
        private String readToArticleBegin() throws IOException {
            while (true) {
                String line = readLine();
                if (line == null) {
                    return null;
                }
                if (line.trim().equals(ARTICLE_BEGIN)) {
                    return line + "\n";
                }
            }
        }

        /**
         * Reads until the end of the article.
         * If the article is too long, it truncates the article and adds a closing tag.
         * @param articleOpen First line of the article.
         * @return
         */
        private String readToArticleClose(String articleOpen) throws IOException {
            StringBuffer buffer = new StringBuffer(articleOpen);
            while (true) {
                String line = readLine();
                if (line == null) {
                    logParseError("reached eof in middle of article");
                    buffer.append(ARTICLE_END + "\n");
                    break;
                }
                if (buffer.length() + line.length() > MAX_ARTICLE_LENGTH) {
                    logParseError("truncating overly long article");
                    buffer.append(ARTICLE_END + "\n");
                    break;
                }
                buffer.append(line + "\n");
                if (line.trim().equals(ARTICLE_END)) {
                    break;
                }
            }
            return buffer.toString();
        }

        private void logParseError(String message) {
            LOG.error("parsing " + path + "  failed in line " + message);
        }

        private void logParseError(String message, Exception e) {
            LOG.error("parsing " + path + "  failed in line " + message);
        }

        private String readLine() throws IOException {
            if (closed) {
                return null;
            }
            String line = reader.readLine();
            if (line == null) {
                reader.close();
                closed = true;
                return null;
            }
            lineNum++;
            return line;
        }

        @Override
        public boolean hasNext() {
            fillBuffer();
            return (buffer != null);
        }

        public String next() {
            fillBuffer();
            String tmp = buffer;
            buffer = null;
            return tmp;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
