package org.wikapidia.parser.wiki;

import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.parser.DumpSplitter;
import org.wikapidia.parser.xml.DumpPageXmlParser;
import org.wikapidia.parser.xml.PageXml;

import java.io.File;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
public class WikiTextDumpParser {
    public static final Logger LOG = Logger.getLogger(WikiTextDumpParser.class.getName());

    private final File file;
    private final LanguageInfo language;
    private final DumpPageXmlParser dpxp;
    private final WikiTextParser wtp;

    public WikiTextDumpParser(File file, LanguageInfo language) {
        this.file = file;
        this.language = language;
        this.dpxp = new DumpPageXmlParser(file, language);
        this.wtp = new WikiTextParser(language);
    }

    public void parse(ParserVisitor visitor) {
        // use the dump page xml parser to iterate over the dump
        Iterator<PageXml> dumpIterator = dpxp.iterator();
        // call the wiki text parser for each article with the visitor
        while (dumpIterator.hasNext()) {
            try {
                wtp.parse(dumpIterator.next(), visitor);
            }
            catch (WikapidiaException e) {
                LOG.log(Level.WARNING, "parsing of " + file + " failed:", e);
            }
        }
    }
}
