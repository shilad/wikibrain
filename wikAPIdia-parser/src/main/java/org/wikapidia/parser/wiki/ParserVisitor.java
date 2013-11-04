package org.wikapidia.parser.wiki;

import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.model.RawPage;

/**
 * Clients of a parser create a visitor that
 * extends methods they find interesting and passes the visitor to the parser.
 */
public class ParserVisitor {
    public void beginPage(RawPage xml) throws WikapidiaException {}

    public void category(ParsedCategory category) throws WikapidiaException {}

    public void ill(ParsedIll ill) throws WikapidiaException {}

    /**
     * This now includes subarticle links.
     * @param link
     * @throws WikapidiaException
     */
    public void link(ParsedLink link) throws WikapidiaException {}

    public void parseError(RawPage rp, Exception e) {}

    /**
     * TODO: fixme
     * @param redirect
     * @throws WikapidiaException
     */
    public void redirect(ParsedRedirect redirect) throws WikapidiaException {}

    public void endPage(RawPage xml) throws WikapidiaException {}
}
