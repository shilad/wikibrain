package org.wikibrain.parser.wiki;

import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.model.RawPage;

/**
 * Clients of a parser create a visitor that
 * extends methods they find interesting and passes the visitor to the parser.
 */
public class ParserVisitor {
    public void beginPage(RawPage xml) throws WikiBrainException {}

    public void category(ParsedCategory category) throws WikiBrainException {}

    public void ill(ParsedIll ill) throws WikiBrainException {}

    /**
     * This now includes subarticle links.
     * @param link
     * @throws WikiBrainException
     */
    public void link(ParsedLink link) throws WikiBrainException {}

    public void parseError(RawPage rp, Exception e) {}

    /**
     * TODO: fixme
     * @param redirect
     * @throws WikiBrainException
     */
    public void redirect(ParsedRedirect redirect) throws WikiBrainException {}

    public void endPage(RawPage xml) throws WikiBrainException {}
}
