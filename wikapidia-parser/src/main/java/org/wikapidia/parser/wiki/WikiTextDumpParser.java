package org.wikapidia.parser.wiki;

import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.parser.xml.DumpPageXmlParser;
import org.wikapidia.parser.xml.PageXml;

import java.io.File;
import java.util.Iterator;

/**
 */
public class WikiTextDumpParser {
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

    public void iterate(ParserVisitor visitor) throws WikapidiaException {
        // use the dump page xml parser to iterate over the dump
        Iterator<PageXml> dumpIterator = dpxp.iterator();
        // call the wiki text parser for each article with the visitor
        while (dumpIterator.hasNext()) {
            wtp.parse(dumpIterator.next(), visitor);
        }
    }
}
