package org.wikapidia.parser;

import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;

import java.io.File;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DumpPageParser implements Iterable<PageXml> {
    public static final Logger LOG = Logger.getLogger(DumpSplitter.class.getName());

    private final PageXmlParser parser;
    DumpSplitter impl;
    LanguageInfo language;

    /**
     * @param file
     * @param language  TODO: read language from dump file!
     */
    public DumpPageParser(File file, LanguageInfo language) {
        this.language = language;
        this.impl = new DumpSplitter(file);
        this.parser = new PageXmlParser(language);
    }

    @Override
    public Iterator<PageXml> iterator() {
        return new IteratorImpl();
    }

    public class IteratorImpl implements Iterator<PageXml> {
        private final Iterator<String> iterImpl;
        private PageXml buff;

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
                    LOG.log(Level.WARNING, "parsing of " + impl.getPath() + " failed:", e);
                }
            }
        }

        @Override
        public PageXml next() {
            fillBuff();
            PageXml next = buff;
            buff = null;
            return next;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
