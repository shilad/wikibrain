package org.wikibrain.loader;

import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.parser.wiki.ParsedIll;
import org.wikibrain.parser.wiki.ParserVisitor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Shilad Sen
 */
public class InterLanguageLinkExtractor {

    static class IllParserVisitor extends ParserVisitor {
        private AtomicInteger count = new AtomicInteger();
        private BufferedWriter output;

        public IllParserVisitor(BufferedWriter output) {
            this.output = output;
        }

        public void ill(ParsedIll ill) throws WikiBrainException {
            RawPage page = ill.location.getXml();
            try {
                // This format may not be easy to parse. Change it.
                synchronized (output) {
                    this.output.write(
                            page.getLanguage().getLangCode() + "\t" + page.getTitle().getCanonicalTitle() + "\t" +
                            ill.title.getLanguage().getLangCode() + "\t" + ill.title.getCanonicalTitle() + "\n");
                }
                count.incrementAndGet();
            } catch (IOException e) {
                throw new WikiBrainException(e);
            }
        }

        public int getCount() {
            return count.get();
        }
    }
}
