package org.wikibrain.pageview;

import org.apache.commons.io.IOUtils;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.Title;
import org.wikibrain.utils.WpIOUtils;

import java.io.*;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shilad Sen
 * An iterator over the page views in a particular file.
 */
public class PageViewReader implements Iterable<RawPageView> {


    private final LanguageSet langs;

    private static final Logger LOG = LoggerFactory.getLogger(PageViewReader.class);

    private final File path;

    public PageViewReader(File path, LanguageSet langs) {
        this.path = path;
        this.langs = langs;
        if (!path.isFile()) {
            throw new IllegalArgumentException("Page view file " + path + " does not exist");
        }
    }

    public PageViewIterator iterator() {
        try {
            return new PageViewIterator();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public class PageViewIterator implements Iterator<RawPageView> {
        private AtomicInteger lines = new AtomicInteger();
        private AtomicInteger errors = new AtomicInteger();
        private AtomicInteger matches = new AtomicInteger();

        private BufferedReader reader;
        private RawPageView buffer = null;

        public PageViewIterator() throws IOException {
            this.reader = WpIOUtils.openBufferedReader(path);
        }

        @Override
        public synchronized boolean hasNext() {
            return fillBuffer();
        }

        @Override
        public synchronized RawPageView next() {
            if (!fillBuffer()) {
                return null;
            }
            RawPageView view = buffer;
            buffer = null;
            return view;
        }

        private synchronized boolean fillBuffer() {
            if (buffer != null) {
                return true;
            }
            if (reader == null) {
                return false;
            }
            while (buffer == null) {
                String line = "";
                try {
                    line = reader.readLine();
                    if (line == null) {
                        close();
                        break;
                    }
                    if (lines.incrementAndGet() % 1000000 == 0) {
                        LOG.info(String.format("File %s: lines=%d, errors=%d, matches=%d",
                                path, lines.get(), errors.get(), matches.get()));
                    }
                    String[] cols = line.split(" ");
                    if (cols.length < 3) {
                        LOG.info("Invalid pageview line: " + line);
                        continue;
                    }

                    Language lang;
                    try {
                        lang = Language.getByLangCode(cols[0]);
                    } catch (IllegalArgumentException e) {
                        continue;   // Not a wikipedia (e.g. Wiktionary)
                    }
                    if (langs.containsLanguage(lang)) {
                        String title = URLDecoder.decode(cols[1], "UTF-8");
                        buffer = new RawPageView(
                                null,
                                new Title(title, lang),
                                Integer.valueOf(cols[2]));
                        matches.incrementAndGet();
                    }
                } catch (IllegalArgumentException e) {
                    errors.incrementAndGet();
//                    LOG.log(Level.INFO, "Invalid pageview line: " + line, e);
                    // Invalid language, perhaps... just continue
                } catch (UnsupportedEncodingException e) {
                    errors.incrementAndGet();
//                    LOG.log(Level.INFO, "Invalid pageview line: " + line, e);
                    // Invalid language, perhaps... just continue
                } catch (IOException e) {
                    errors.incrementAndGet();
                    throw new RuntimeException(e);
                } catch (Exception e) {
                }
            }
            return buffer != null;
        }

        public synchronized void close() {
            if (reader != null) {
                IOUtils.closeQuietly(reader);
                reader = null;
            }
        }

        @Override
        public void remove() { throw new UnsupportedOperationException(); }
    }
}
