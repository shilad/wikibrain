package org.wikapidia.parser.wiki;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sweble.wikitext.engine.EngineException;
import org.sweble.wikitext.engine.config.WikiConfig;
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp;
import org.sweble.wikitext.parser.parser.LinkTargetException;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.core.model.Title;

import edu.northwestern.websail.wikiparser.wikitextparser.model.WikiExtractedPage;
import edu.northwestern.websail.wikiparser.wikitextparser.model.WikiTitle;
import edu.northwestern.websail.wikiparser.wikitextparser.model.element.WikiElement;
import edu.northwestern.websail.wikiparser.wikitextparser.model.element.WikiLink;
import edu.northwestern.websail.wikiparser.wikitextparser.model.element.WikiPageLocationType;
import edu.northwestern.websail.wikiparser.wikitextparser.model.element.WikiParagraph;
import edu.northwestern.websail.wikiparser.wikitextparser.model.element.WikiSection;
import edu.northwestern.websail.wikiparser.wikitextparser.sweblewrapper.SwebleWikiExtractor;
/**
 * An adapter class for SwebleWikiExtractor that is an implementation of Sweble's AstVisitor. 
 * This class provides a simple information extraction from WikiText. The information includes 
 * plain text, sections, paragraphs, links, interwiki links, category links, and title (with redirect).
 * 
 * The class performs parsing only once, and then distributes information to visitors. Currently, 
 * vistors only includes begin, end, links, interwiki links, category links, and redirect.
 * 
 * Note that Sweble Engine itself is still in Alpha state (https://github.com/sweble/sweble-wikitext), 
 * and so does SwebleWikiExtractor.
 * 
 * @author NorThanapon
 * 
 */
public class WebSAILWikiTextParser implements WikiTextParser {
	public static final Logger LOG = Logger.getLogger(WebSAILWikiTextParser.class.getName());
	private SwebleWikiExtractor extractor;
	private List<ParserVisitor> visitors;
	public WebSAILWikiTextParser(String langPrefix, WikiConfig config){
		extractor = new SwebleWikiExtractor(langPrefix, config);
	}
	
	/**
	 * @see WikiTextParser#parse(RawPage, List)
	 */
	@Override
	public void parse(RawPage xml, List<ParserVisitor> visitors)
			throws WikapidiaException {
		this.visitors = visitors;
		int pageLocalId = xml.getLocalId();
		String pageTitleName = xml.getTitle().getTitleStringWithoutNamespace();
		String wikiText = xml.getBody();
		WikiExtractedPage exPage = null;
		try {
			exPage = extractor.parse(pageLocalId, pageTitleName, wikiText);
		} catch (LinkTargetException e) {
			LOG.severe("Could not process link (" +e.getMessage()+")");
			this.visitParseError(xml, e);
		} catch (EngineException e) {
			LOG.severe("Unexpected error while parsing page " + pageTitleName + " ("+e.getMessage()+")");
			this.visitParseError(xml, e);
		}
		xml.setPlainText(exPage.getPlainText());
		this.visitBeginPage(xml);
		this.visit(xml, exPage);
		this.visitEndPage(xml);
	}
	
	/* -------------------------------------------------------------------------------
	 * Adapter methods WebSAILWikiParser's models -> wikAPIdia's models
	 * -------------------------------------------------------------------------------
	 */
	
	private Title parseTitle(WikiTitle title){
		Language lang = Language.getByLangCode(title.getRedirectedTitle().getLanguage());
		Title pTitle = new Title(title.getRedirectedTitle().getTitle(), lang);
		return pTitle;
	}
	
	private ParsedLocation parseLocation(RawPage xml, WikiExtractedPage exPage, WikiElement element){
		int sectionNum = this.getNumSection(element, exPage.getSections());
		int paragraphNum = this.getNumParagraph(element, exPage.getParagraphs());
		return new ParsedLocation(xml, sectionNum, paragraphNum, element.getOffset());
	}
	
	private ParsedRedirect parseRedirect(RawPage xml, WikiTitle currentTitle){
		if(!currentTitle.isRedirecting()) return null; 
		Title title = this.parseTitle(currentTitle);
		ParsedRedirect pr = new ParsedRedirect();
		pr.target = title;
        pr.location = new ParsedLocation(xml, -1, -1, -1);
        return pr;
	}
	
	private ParsedLink parseLink(RawPage xml, WikiExtractedPage exPage, WikiLink link){
		ParsedLocation pLocation = this.parseLocation(xml, exPage, link);
		ParsedLink pl = new ParsedLink();
        pl.location = pLocation;
        pl.target = this.parseTitle(link.getTarget());
        pl.text = link.getSurface();
        //TODO: Change type to have more detail (i.e. overview, main, table, list, ...)
        pl.subarticleType = ParsedLink.SubarticleType.MAIN_INLINE;
        return pl;
	}
	
	private ParsedCategory parseCategory(RawPage xml, WikiLink catLink){
		ParsedCategory pc = new ParsedCategory();
		pc.location = new ParsedLocation(xml, -1, -1, catLink.getOffset());
		pc.category = this.parseTitle(catLink.getTarget());
		return pc;
	}
	
	private ParsedIll parseInterLanguageLink(RawPage xml, WikiExtractedPage exPage, WikiLink link){
		ParsedLocation pLocation = this.parseLocation(xml, exPage, link);
		ParsedIll pl = new ParsedIll();
        pl.location = pLocation;
        pl.title = this.parseTitle(link.getTarget());
        return pl;
	}
	
	/* -------------------------------------------------------------------------------
	 * Distribute information to visitors
	 * -------------------------------------------------------------------------------
	 */
	
	private void visit(RawPage xml, WikiExtractedPage exPage){
		ParsedRedirect pr =parseRedirect(xml, exPage.getTitle());
		if(pr != null) {
			this.visitRedirect(pr);
			return;
		}
		for(WikiLink link: exPage.getInternalLinks()){
			this.visitLink(this.parseLink(xml, exPage, link));
		}
		for(WikiLink link: exPage.getCategoryLinks()){
			this.visitCategory(this.parseCategory(xml, link));
		}
		for(WikiLink link: exPage.getInterWikiLinks()){
			this.visitIll(this.parseInterLanguageLink(xml, exPage, link));
		}
		
	}
	
	private void visitBeginPage(RawPage xml) {
        for (ParserVisitor visitor : visitors) {
            try {
                visitor.beginPage(xml);
            } catch (WikapidiaException e) {
                LOG.log(Level.WARNING, "Visit beginPage failed:", e);
            }
        }
    }
	
	private void visitRedirect(ParsedRedirect redirect) {
        for (ParserVisitor visitor : visitors) {
            try {
                visitor.redirect(redirect);
            } catch (WikapidiaException e) {
                LOG.log(Level.WARNING, "Visit redirect failed:", e);
            }
        }
    }
    private void visitParseError(RawPage rp, Exception e) {
        for (ParserVisitor visitor : visitors) {
            visitor.parseError(rp, e);
        }
    }
    private void visitIll(ParsedIll ill) {
        for (ParserVisitor visitor : visitors) {
            try {
                visitor.ill(ill);
            } catch (WikapidiaException e) {
                LOG.log(Level.WARNING, "Visit Ill failed:", e);
            }
        }
    }
    private void visitCategory(ParsedCategory cat) {
        for (ParserVisitor visitor : visitors) {
            try {
                visitor.category(cat);
            } catch (WikapidiaException e) {
               LOG.log(Level.WARNING, "Visit category failed:", e);
            }
        }
    }
    
    private void visitLink(ParsedLink pl) {

        for (ParserVisitor visitor : visitors) {
            try {
                visitor.link(pl);
            } catch (WikapidiaException e) {
                LOG.log(Level.WARNING, "Visit link failed:", e);
            }
        }
    }
	
    private void visitEndPage(RawPage xml) {
        for (ParserVisitor visitor : visitors) {
            try {
                visitor.endPage(xml);
            } catch (WikapidiaException e) {
                LOG.log(Level.WARNING, "Visit endPage failed:", e);
            }
        }
    }

    /* -------------------------------------------------------------------------------
	 * Factory and Utilities
	 * -------------------------------------------------------------------------------
	 */
    
	/**
	 * Facilitate WebSAILWikiTextParser construction
	 */
	public static class WebSAILParserFactory implements WikiTextParser.Factory {

		private WikiConfig config;
		
		@Override
		public WikiTextParser create(Language language) {
			String langPrefix = language.getEnLangName();
			//TODO: Change config for a specific language
			if(config == null) config = DefaultConfigEnWp.generate();
			WebSAILWikiTextParser parser = new WebSAILWikiTextParser(langPrefix, config);
			return parser;
		}
		
	}
	
	private int getNumSection(WikiElement element, List<WikiSection> sections){
		if(element.getLocType() == WikiPageLocationType.BEFORE_OVERVIEW || 
				element.getLocType() == WikiPageLocationType.OVERVIEW) 
			return -1;
		for(WikiSection s : sections){
			if(element.getOffset() > s.getOffset()) return s.getSectionNum();
		}
		return -2;
	}
	
	private int getNumParagraph(WikiElement element, List<WikiParagraph> paragraphs){
		if(element.getLocType() == WikiPageLocationType.BEFORE_OVERVIEW) 
			return -1;
		if(element.getLocType() == WikiPageLocationType.OVERVIEW)
			return 0;
		for(WikiParagraph p : paragraphs){
			if(element.getOffset() > p.getOffset()) return p.getParagraphNum();
		}
		return -2;
	}
}
