package org.wikibrain.wikidata;

import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.parser.DumpSplitter;
import org.wikibrain.parser.WpParseException;
import org.wikibrain.parser.xml.PageXmlParser;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Shilad Sen
 */
public class WikidataDumpParser implements Iterable<WikidataEntity> {
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
    public Iterator<WikidataEntity> iterator() {
        return new IteratorImpl();
    }

    public class IteratorImpl implements Iterator<WikidataEntity> {
        private final Iterator<String> iterImpl;
        private WikidataEntity buff;

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
                    if (rp.getModel().equals("wikibase-item") || rp.getModel().equals("wikibase-property")) {
                        buff = wdParser.parse(rp);
                    } else if (Arrays.asList("wikitext", "css", "javascript").contains(rp.getModel())) {
                        buff = null;
                    } else {
                        LOG.warning("unknown model: " + rp.getModel() + " in page " + rp.getTitle());
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "parsing of " + impl.getPath() + " failed:", e);
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
