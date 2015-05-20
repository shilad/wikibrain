package org.wikibrain.wikidata;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringEscapeUtils;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.parser.DumpSplitter;
import org.wikibrain.parser.xml.PageXmlParser;
import org.wikibrain.utils.WpIOUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Shilad Sen
 */
public class WikidataDumpParser implements Iterable<WikidataEntity> {
    public static final Logger LOG = LoggerFactory.getLogger(DumpSplitter.class);

    private final WikidataParser wdParser;
    private final LanguageSet languages;
    private final File file;

    public WikidataDumpParser(File file) {
        this(file, LanguageSet.ALL);
    }

    /**
     * @param file
     */
    public WikidataDumpParser(File file, LanguageSet languages) {
        this.file = file;
        this.languages = languages;
        this.wdParser = new WikidataParser();
    }

    @Override
    public Iterator<WikidataEntity> iterator() {
        return new IteratorImpl();
    }

    public class IteratorImpl implements Iterator<WikidataEntity> {
        private final Iterator<String> iterImpl;
        private WikidataEntity buff;

        public IteratorImpl() {
            try {
                this.iterImpl = new LineIterator(WpIOUtils.openBufferedReader(file));
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        public boolean hasNext() {
            if (buff == null) {
                fillBuff();
            }
            return (buff != null);
        }

        private void fillBuff() {
            if (buff != null) {
                return;
            }
            // try to queue up the next article
            while (buff == null && iterImpl.hasNext()) {
                String line = iterImpl.next();
                if (line.trim().equals("[") || line.trim().equals("]")) {
                    continue;
                }
                try {
                    if (line.endsWith(",")) {
                        line = line.substring(0, line.length()-1);
                    }
                    if (!line.trim().isEmpty()) {
                        buff = wdParser.parse(line);
                    }
                } catch (Exception e) {
                    LOG.warn("parsing of " + file + " failed for line '" + line  + "':", e);
                }
            }
        }

        @Override
        public WikidataEntity next() {
            fillBuff();
            WikidataEntity next = buff;
            buff = null;
            return next;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
