package org.wikapidia.parser.wiki;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.parser.xml.DumpPageXmlParser;
import org.wikapidia.parser.xml.PageXml;

import java.io.File;
import java.util.Iterator;
import java.util.List;
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

    public WikiTextDumpParser(File file, LanguageInfo language, List<String> allowedIllLangs) {
        this.file = file;
        this.language = language;
        this.dpxp = new DumpPageXmlParser(file, language);
        this.wtp = new WikiTextParser(language, allowedIllLangs);
    }

    /**
     * Parses the input file completely. First splits the file into individual PageXmls via
     * DumpPageXmlParser, then parses each page via WikiTextParser
     *
     * @param visitor extracts data from side effects
     */
    public void parse(ParserVisitor visitor) {
        Iterator<PageXml> pageIterator = dpxp.iterator();
        while (pageIterator.hasNext()) {
            try {
                wtp.parse(pageIterator.next(), visitor);
            }
            catch (WikapidiaException e) {
                LOG.log(Level.WARNING, "parsing of " + file + " failed:", e);
            }
        }
    }
}
