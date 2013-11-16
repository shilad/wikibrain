package org.wikapidia.parser.wiki;

import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.RawPage;

import java.util.List;

/**
 * @author Shilad Sen
 */
public interface WikiTextParser {
    /**
     * Parses a raw page and notifies the visitors of links, categories, etc.
     * @throws WikapidiaException
     */
    public void parse(RawPage xml, List<ParserVisitor> visitors) throws WikapidiaException;

    /**
     * A factory to create new WikiTextParsers
     */
    public interface Factory {
        public WikiTextParser create(Language language);
    }
}
