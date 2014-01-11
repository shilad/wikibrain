package org.wikapidia.wikidata;

import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.parser.DumpSplitter;
import org.wikapidia.parser.WpParseException;
import org.wikapidia.parser.xml.PageXmlParser;

import java.io.File;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Shilad Sen
 */
public class WikidataDumpParser implements Iterable<WikidataRawRecord> {
    public static final Logger LOG = Logger.getLogger(DumpSplitter.class.getName());

    private final WikidataParser wdParser;
    private final PageXmlParser xmlParser;
    DumpSplitter impl;
    LanguageInfo language;

    /**
     * @param file
     */
    public WikidataDumpParser(File file) {
        this.language = LanguageInfo.getByLangCode("en");
        this.impl = new DumpSplitter(file);
        this.xmlParser = new PageXmlParser(language);
        this.wdParser = new WikidataParser();
    }

    @Override
    public Iterator<WikidataRawRecord> iterator() {
        return new IteratorImpl();
    }

    public class IteratorImpl implements Iterator<WikidataRawRecord> {
        private final Iterator<String> iterImpl;
        private WikidataRawRecord buff;

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
                    RawPage rp = xmlParser.parse(iterImpl.next());
                    if (rp.getModel() != null && rp.getModel().equals("wikibase-item")) {
                        buff = wdParser.parse(rp);
                    } else {
                        buff = new WikidataRawRecord(rp);
                    }
                } catch (WpParseException e) {
                    LOG.log(Level.WARNING, "parsing of " + impl.getPath() + " failed:", e);
                }
            }
        }

        @Override
        public WikidataRawRecord next() {
            fillBuff();
            WikidataRawRecord next = buff;
            buff = null;
            return next;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
