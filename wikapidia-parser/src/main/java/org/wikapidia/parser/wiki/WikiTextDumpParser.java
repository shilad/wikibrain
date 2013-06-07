package org.wikapidia.parser.wiki;

import org.wikapidia.core.lang.LanguageInfo;

import java.io.File;

/**
 */
public class WikiTextDumpParser {
    private final File file;
    private final LanguageInfo info;

    public WikiTextDumpParser(File file, LanguageInfo info) {
        this.file = file;
        this.info = info;

        // create a DumpPageXmlParser to use

        // create WikiTextParser
    }

    public void iterate(ParserVisitor visitor) {
        // use the dump page xml parser to iterate over the dump

        // call the wiki text parser for each article with the visitor
    }
}
