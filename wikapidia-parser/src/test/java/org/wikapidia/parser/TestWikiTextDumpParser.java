package org.wikapidia.parser;

import org.junit.Test;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.parser.wiki.*;
import org.wikapidia.parser.xml.PageXml;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 */
public class TestWikiTextDumpParser {
    public static final File EN_DUMP = new File("src/test/resources/org/wikapidia/parser/en_test.xml");
    public static final LanguageInfo EN = LanguageInfo.getByLangCode("en");

    @Test
    public void test1() {
        WikiTextDumpParser wtdp = new WikiTextDumpParser(EN_DUMP, EN);

        final AtomicInteger pageCounter = new AtomicInteger();
        final ArrayList<ParsedCategory> categories = new ArrayList<ParsedCategory>();
        final ArrayList<ParsedIll> ills = new ArrayList<ParsedIll>();
        final ArrayList<ParsedLink> links = new ArrayList<ParsedLink>();
        final ArrayList<ParsedRedirect> redirects = new ArrayList<ParsedRedirect>();

        ParserVisitor visitor = new ParserVisitor() {
            @Override
            public void beginPage(PageXml xml) {
                pageCounter.incrementAndGet();
            }

            @Override
            public void category(ParsedCategory category) {
                categories.add(category);
            }

            @Override
            public void ill(ParsedIll ill) {
                ills.add(ill);
            }

            @Override
            public void link(ParsedLink link) {
                links.add(link);
            }

            @Override
            public void redirect(ParsedRedirect redirect) {
                redirects.add(redirect);
            }
        };

        wtdp.parse(visitor);

        assertEquals(pageCounter.get(), 44);
    }
}
