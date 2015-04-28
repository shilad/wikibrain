package org.wikibrain.parser.xml;

import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.parser.DumpSplitter;
import org.wikibrain.parser.WpParseException;

import java.io.File;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DumpPageXmlParser implements Iterable<RawPage> {
    public static final Logger LOG = LoggerFactory.getLogger(DumpSplitter.class);

    private final PageXmlParser parser;
    DumpSplitter impl;
    LanguageInfo language;

    /**
     * @param file
     * @param language  TODO: read language from dump file!
     */
    public DumpPageXmlParser(File file, LanguageInfo language) {
        this.language = language;
        this.impl = new DumpSplitter(file);
        this.parser = new PageXmlParser(language);
    }

    @Override
    public Iterator<RawPage> iterator() {
        return new IteratorImpl();
    }

    public class IteratorImpl implements Iterator<RawPage> {
        private final Iterator<String> iterImpl;
        private RawPage buff;

        public IteratorImpl() {
            this.iterImpl = impl.iterator();
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
                try {
                    buff = parser.parse(iterImpl.next());
                } catch (WpParseException e) {
                    LOG.warn("parsing of " + impl.getPath() + " failed:", e);
                }
            }
        }

        @Override
        public RawPage next() {
            fillBuff();
            RawPage next = buff;
            buff = null;
            return next;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
