package org.wikapidia.parser.wiki;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.h2.util.StringUtils;
import org.sweble.wikitext.engine.EngineException;
import org.sweble.wikitext.engine.PageId;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.WtEngineImpl;
import org.sweble.wikitext.engine.config.WikiConfig;
import org.sweble.wikitext.engine.nodes.EngProcessedPage;
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp;
import org.sweble.wikitext.parser.parser.LinkTargetException;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.conf.Provider;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.parser.wiki.sweblewrapper.SweblePlainTextProcessor;

import com.typesafe.config.Config;

import edu.northwestern.websail.wikiparser.wikitextparser.sweblewrapper.SwebleUtils;
/**
 * A parser wraper for SweblePlainTextProcessor. 
 * This class provides a simple information extraction from WikiText including links and plaintext.
 * 
 * 
 * Note that Sweble Engine itself is still in Alpha state (https://github.com/sweble/sweble-wikitext), 
 * and so does SwebleWikiExtractor.
 * 
 * 
 * @author NorThanapon
 * 
 */
public class SwebleWikiTextParser implements WikiTextParser {
	public static final Logger LOG = Logger.getLogger(SwebleWikiTextParser.class.getName());
	private WikiConfig config;
	private List<ParserVisitor> visitors;
	private String plainText;
	public SwebleWikiTextParser(WikiConfig config){
		this.config = config;
	}
	
	/**
	 * @see WikiTextParser#parse(RawPage, List)
	 */
	@Override
	public void parse(RawPage xml, List<ParserVisitor> visitors)
			throws WikapidiaException {
		this.visitors = visitors;
		int pageLocalId = xml.getLocalId();
		String pageTitleName = xml.getTitle().getCanonicalTitle();
		String wikiText = xml.getBody();
		try {
			this.visitBeginPage(xml);
			wikiText = SwebleUtils.cleanWikiText(wikiText);
			WtEngineImpl engine = new WtEngineImpl(config);
	        //System.out.println(wikiText);
	        // Retrieve a page
	        PageTitle pageTitle = PageTitle.make(config, xml.getTitle().getCanonicalTitle());
	        PageId pageId = new PageId(pageTitle, pageLocalId);
			SweblePlainTextProcessor sp = new SweblePlainTextProcessor(config, visitors, xml);
			EngProcessedPage cp = engine.postprocess(pageId, wikiText, null);
			plainText = (String) sp.go(cp.getPage());
			this.visitEndPage(xml);
		} catch (LinkTargetException e) {
			LOG.severe("Could not process link (" +e.getMessage()+")");
			this.visitParseError(xml, e);
		} catch (EngineException e) {
			LOG.severe("Unexpected error while parsing page " + pageTitleName + " ("+e.getMessage()+")");
			this.visitParseError(xml, e);
		}
	}

    /**
     * @see WikiTextParser#getPlainText(RawPage)
     */
    @Override
    public String getPlainText(RawPage xml) throws WikapidiaException {
        if (StringUtils.isNullOrEmpty(xml.getBody())) {
            return "";
        }
    	this.parse(xml, new ArrayList<ParserVisitor>());
        return this.plainText;
        
    }
	
	/* -------------------------------------------------------------------------------
	 * Distribute information to visitors
	 * -------------------------------------------------------------------------------
	 */
	
	private void visitBeginPage(RawPage xml) {
        for (ParserVisitor visitor : visitors) {
            try {
                visitor.beginPage(xml);
            } catch (WikapidiaException e) {
                LOG.log(Level.WARNING, "Visit beginPage failed:", e);
            }
        }
    }
	
    private void visitParseError(RawPage rp, Exception e) {
        for (ParserVisitor visitor : visitors) {
            visitor.parseError(rp, e);
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
	 * Facilitate SwebleWikiTextParser construction
	 */
	public static class SwebleWikiTextParserFactory implements WikiTextParser.Factory {

		private WikiConfig config;
		
		@Override
		public WikiTextParser create(Language language) {
			
			//TODO: Change config for a specific language
			if(config == null) config = DefaultConfigEnWp.generate();
			SwebleWikiTextParser parser = new SwebleWikiTextParser(config);
			return parser;
		}
		
	}
	
	/* -------------------------------------------------------------------------------
	 * Provider
	 * -------------------------------------------------------------------------------
	 */
	
	public static class SwebleProvider extends Provider<WikiTextParser.Factory> {
        /**
         * @see Provider<T>
         */
        public SwebleProvider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class<Factory> getType() {
            return WikiTextParser.Factory.class;
        }

        @Override
        public String getPath() {
            return "parser.wiki";
        }

        @Override
        public Factory get(String name, Config config) throws ConfigurationException {
            if (!config.getString("type").equals("sweble")) {
                return null;
            }
            return new SwebleWikiTextParserFactory();
        }
    }
	
}
