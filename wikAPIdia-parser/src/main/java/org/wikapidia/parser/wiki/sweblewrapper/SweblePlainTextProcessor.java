package org.wikapidia.parser.wiki.sweblewrapper;

/**
 * An implementation of Sweble's AstVisitor. 
 * This class provides a simple information extraction from WikiText including links and plaintext.
 * 
 * @author NorThanapon
 */

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.config.WikiConfig;
import org.sweble.wikitext.engine.nodes.CompleteEngineVisitorNoReturn;
import org.sweble.wikitext.engine.nodes.EngNowiki;
import org.sweble.wikitext.engine.nodes.EngPage;
import org.sweble.wikitext.engine.nodes.EngProcessedPage;
import org.sweble.wikitext.engine.nodes.EngSoftErrorNode;
import org.sweble.wikitext.engine.nodes.EngineNodeFactory;
import org.sweble.wikitext.engine.utils.EngineAstTextUtils;
import org.sweble.wikitext.parser.nodes.WtBody;
import org.sweble.wikitext.parser.nodes.WtBold;
import org.sweble.wikitext.parser.nodes.WtDefinitionList;
import org.sweble.wikitext.parser.nodes.WtDefinitionListDef;
import org.sweble.wikitext.parser.nodes.WtDefinitionListTerm;
import org.sweble.wikitext.parser.nodes.WtExternalLink;
import org.sweble.wikitext.parser.nodes.WtHeading;
import org.sweble.wikitext.parser.nodes.WtHorizontalRule;
import org.sweble.wikitext.parser.nodes.WtIgnored;
import org.sweble.wikitext.parser.nodes.WtIllegalCodePoint;
import org.sweble.wikitext.parser.nodes.WtImEndTag;
import org.sweble.wikitext.parser.nodes.WtImStartTag;
import org.sweble.wikitext.parser.nodes.WtImageLink;
import org.sweble.wikitext.parser.nodes.WtInternalLink;
import org.sweble.wikitext.parser.nodes.WtItalics;
import org.sweble.wikitext.parser.nodes.WtLinkOptionAltText;
import org.sweble.wikitext.parser.nodes.WtLinkOptionGarbage;
import org.sweble.wikitext.parser.nodes.WtLinkOptionKeyword;
import org.sweble.wikitext.parser.nodes.WtLinkOptionLinkTarget;
import org.sweble.wikitext.parser.nodes.WtLinkOptionResize;
import org.sweble.wikitext.parser.nodes.WtLinkOptions;
import org.sweble.wikitext.parser.nodes.WtLinkTitle;
import org.sweble.wikitext.parser.nodes.WtListItem;
import org.sweble.wikitext.parser.nodes.WtName;
import org.sweble.wikitext.parser.nodes.WtNewline;
import org.sweble.wikitext.parser.nodes.WtNode;
import org.sweble.wikitext.parser.nodes.WtNodeList;
import org.sweble.wikitext.parser.nodes.WtOnlyInclude;
import org.sweble.wikitext.parser.nodes.WtOrderedList;
import org.sweble.wikitext.parser.nodes.WtPageName;
import org.sweble.wikitext.parser.nodes.WtPageSwitch;
import org.sweble.wikitext.parser.nodes.WtParagraph;
import org.sweble.wikitext.parser.nodes.WtParsedWikitextPage;
import org.sweble.wikitext.parser.nodes.WtPreproWikitextPage;
import org.sweble.wikitext.parser.nodes.WtRedirect;
import org.sweble.wikitext.parser.nodes.WtSection;
import org.sweble.wikitext.parser.nodes.WtSemiPre;
import org.sweble.wikitext.parser.nodes.WtSemiPreLine;
import org.sweble.wikitext.parser.nodes.WtSignature;
import org.sweble.wikitext.parser.nodes.WtTable;
import org.sweble.wikitext.parser.nodes.WtTableCaption;
import org.sweble.wikitext.parser.nodes.WtTableCell;
import org.sweble.wikitext.parser.nodes.WtTableHeader;
import org.sweble.wikitext.parser.nodes.WtTableImplicitTableBody;
import org.sweble.wikitext.parser.nodes.WtTableRow;
import org.sweble.wikitext.parser.nodes.WtTagExtension;
import org.sweble.wikitext.parser.nodes.WtTagExtensionBody;
import org.sweble.wikitext.parser.nodes.WtTemplate;
import org.sweble.wikitext.parser.nodes.WtTemplateArgument;
import org.sweble.wikitext.parser.nodes.WtTemplateArguments;
import org.sweble.wikitext.parser.nodes.WtTemplateParameter;
import org.sweble.wikitext.parser.nodes.WtText;
import org.sweble.wikitext.parser.nodes.WtTicks;
import org.sweble.wikitext.parser.nodes.WtUnorderedList;
import org.sweble.wikitext.parser.nodes.WtUrl;
import org.sweble.wikitext.parser.nodes.WtValue;
import org.sweble.wikitext.parser.nodes.WtWhitespace;
import org.sweble.wikitext.parser.nodes.WtXmlAttribute;
import org.sweble.wikitext.parser.nodes.WtXmlAttributeGarbage;
import org.sweble.wikitext.parser.nodes.WtXmlAttributes;
import org.sweble.wikitext.parser.nodes.WtXmlCharRef;
import org.sweble.wikitext.parser.nodes.WtXmlComment;
import org.sweble.wikitext.parser.nodes.WtXmlElement;
import org.sweble.wikitext.parser.nodes.WtXmlEmptyTag;
import org.sweble.wikitext.parser.nodes.WtXmlEndTag;
import org.sweble.wikitext.parser.nodes.WtXmlEntityRef;
import org.sweble.wikitext.parser.nodes.WtXmlStartTag;
import org.sweble.wikitext.parser.parser.LinkTargetException;
import org.sweble.wikitext.parser.utils.StringConversionException;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.core.model.Title;
import org.wikapidia.parser.wiki.ParsedCategory;
import org.wikapidia.parser.wiki.ParsedIll;
import org.wikapidia.parser.wiki.ParsedLink;
import org.wikapidia.parser.wiki.ParsedLocation;
import org.wikapidia.parser.wiki.ParserVisitor;

import de.fau.cs.osr.ptk.common.AstVisitor;
import de.fau.cs.osr.utils.StringUtils;

public class SweblePlainTextProcessor extends AstVisitor<WtNode> implements
		CompleteEngineVisitorNoReturn {
	public static final Logger LOG = Logger.getLogger(SweblePlainTextProcessor.class.getName());
	protected StringBuilder sbText;
	protected WikiConfig config;
	protected EngineNodeFactory nf;
	protected EngineAstTextUtils tu;
	private int sectionNum = 0;
	private int pNum = -1;
	private int indentLevel = 0;
	private List<ParserVisitor> visitors;
	private HashSet<String> interWikiPrefixSet;
	private Title articleTitle;
	private RawPage rawPage;
	public SweblePlainTextProcessor(WikiConfig config, List<ParserVisitor> visitors, RawPage rawPage){
		sbText = new StringBuilder();
		this.config = config;
		nf = config.getNodeFactory();
		tu = config.getAstTextUtils();
		this.interWikiPrefixSet = SwebleUtils.extractInterWikiPrefixSet(config);
		this.articleTitle = rawPage.getTitle();
		this.rawPage = rawPage;
	}
	
	@Override
	protected boolean before(WtNode node)
	{
		// This method is called by go() before visitation starts
		return super.before(node);
	}
	
	@Override
	protected Object after(WtNode node, Object result)
	{
		return this.sbText.toString();
	}
		
	// IMPLEMENTED ELEMENTS
	
	@Override
	public void visit(EngPage page) {
		iterate(page);
	}
	
	@Override
	public void visit(WtSection s) {
		sectionNum++;
		iterate(s.getHeading());
	}

	@Override
	public void visit(WtHeading heading) {
		//section heading
		iterate(heading);
	}
	
	@Override
	public void visit(WtBody body) {
		//section body
		iterate(body);
	}

	@Override
	public void visit(WtParagraph p) {
		
		int start = sbText.length();
		if(sectionNum == 0) pNum = 0;
		else pNum++;
		iterate(p);
		int end = sbText.length();
		if(sbText.substring(start, end).trim().equals("")) pNum--; //empty paragraph
	}
	
	@Override
	public void visit(WtNewline p) {
		print('\n');
		iterate(p);
	}
	
	@Override
	public void visit(WtHorizontalRule p) {
		print('\n');
		iterate(p);
	}
	
	@Override
	public void visit(WtText text) {
		//text unit
		print(text.getContent());
	}
	
	@Override
	public void visit(WtBold text) {
		//text unit
		iterate(text);
	}

	@Override
	public void visit(WtItalics text) {
		//text unit
		iterate(text);
	}
	
	@Override
	public void visit(WtWhitespace arg0) {
		print(' ');
	}
	
	@Override
	public void visit(WtInternalLink link) {
		int start = sbText.length();
		ParsedLocation location = new ParsedLocation(rawPage, sectionNum, pNum, start);
		boolean isCategory = false;
		boolean isInterWiki = false;
		Language lang = articleTitle.getLanguage();
		String namespace = "";
		Title targetTitle = null;
		try{
			if (link.getTarget().isResolved()){
				PageTitle page = PageTitle.make(config, link.getTarget().getAsString());
				namespace = config.getNamespace(page.getNamespace().getId()).getName();
				if (page.getNamespace().getId() == 14){
					isCategory = true;
				}
				else if (page.getInterwikiLink() != null){
					String languagePrefix = page.getInterwikiLink().getPrefix();
					if(!articleTitle.getLanguage().getLangCode().equals(languagePrefix)){
						isInterWiki = true;
						lang = Language.getByLangCode(languagePrefix);
					}
				}
				if(page.getNamespace().getId() == 0)
					targetTitle = new Title(page.getTitle(), lang);
				else
					targetTitle = new Title(namespace + ":" + page.getTitle(), lang);		
			}
		}
		catch (LinkTargetException e){
		}
		if(!isCategory){
			print(link.getPrefix());
			if (!link.hasTitle()){
				iterate(link.getTarget());
			}
			else{
				iterate(link.getTitle());
			}
		}
		int end = sbText.length();

		print(link.getPostfix());
		String surface = sbText.substring(start, end);
		if(isCategory){
			if(link.hasTitle()) {
				try {
					WtText t = (WtText) link.getTitle().get(0);
					surface = t.getContent().trim();
				}catch(Exception e){
					surface = link.getTarget().getAsString();
				}
			}
			else surface = link.getTarget().getAsString();
		}
		if(isCategory) this.visitCategory(targetTitle, location);
		else if (isInterWiki) this.visitIll(targetTitle, location);
		else this.visitLink(start, end, surface, targetTitle, location);
	}
	
	private void visitLink(int start, int end, String surface, Title target, ParsedLocation location){
		ParsedLink link = new ParsedLink();
		link.location=location;
		link.target=target;
		link.text=surface;
		for (ParserVisitor visitor : visitors){
			try {
				visitor.link(link);
			} catch (WikapidiaException e) {
				LOG.log(Level.WARNING, "Visit link failed:", e);
			}
		}
	}
	
	private void visitCategory(Title target, ParsedLocation location){
		ParsedCategory pc = new ParsedCategory();
		pc.category = target;
		pc.location = location;
		for (ParserVisitor visitor : visitors){
			try {
				visitor.category(pc);
			} catch (WikapidiaException e) {
				LOG.log(Level.WARNING, "Visit category failed:", e);
			}
		}
	}
	
	private void visitIll(Title target, ParsedLocation location){
		ParsedIll pl = new ParsedIll();
		pl.location = location;
        pl.title = target;
		for (ParserVisitor visitor : visitors){
			try {
				visitor.ill(pl);
			} catch (WikapidiaException e) {
				LOG.log(Level.WARNING, "Visit link failed:", e);
			}
		}
	}
	
	@Override
	public void visit(WtExternalLink n) {
		if (n.hasTitle()) {
			iterate(n.getTitle());
		}
	}

	@Override
	public void visit(WtDefinitionList dList) {
		iterate(dList);
	}


	@Override
	public void visit(WtDefinitionListTerm dListTerm) {
		print('\t');
		iterate(dListTerm);
		print('\n');
		
	}
	
	@Override
	public void visit(WtDefinitionListDef dListDef) {
		
		print("\t\t");
		iterate(dListDef);
		print('\n');
	}
	
	@Override
	public void visit(WtOrderedList list) {
		print('\n');
		indentLevel++;
		iterate(list);
		indentLevel--;
	}

	@Override
	public void visit(WtUnorderedList list) {
		print('\n');
		indentLevel++;
		iterate(list);
		indentLevel--;
	}
	
	@Override
	public void visit(WtListItem listItem) {
		this.indent();
		iterate(listItem);
		print('\n');
	}
	
	@Override
	public void visit(WtSemiPre n) {
		iterate(n);
		print('\n');
	}

	@Override
	public void visit(WtSemiPreLine n) {
		iterate(n);
		print('\n');
	}
	
	@Override
	public void visit(WtTagExtension n) {
		//special case for tags
		//including <pre>
		if (n.getName().trim().equalsIgnoreCase("ref"))
                return;
        if (n.getName().trim().equalsIgnoreCase("references"))
                return;
        print(n.getBody().getContent());
	}
	
	@Override
	public void visit(EngNowiki n) {
		print(n.getContent());
	}

	@Override
	public void visit(WtXmlEntityRef entity) {
		print("&" + entity.getName()+";");
	}
	
	@Override
	public void visit(WtXmlCharRef entity) {
		print("&#"+entity.getCodePoint()+";");
	}	

	@Override
	public void visit(WtIllegalCodePoint n) {
		 final String cp = n.getCodePoint();
         for (int i = 0; i < cp.length(); ++i) {
             int code = (int) cp.charAt(i);    
        	 print("&#"+code+";");
         }
	}
	
	@Override
	public void visit(WtTable table) {
		fixTableBody(table.getBody());
	}
	
	@Override
	public void visit(WtTableImplicitTableBody n) {

		iterate(n.getBody());
	}

	@Override
	public void visit(WtTableCaption n) {
		
		print("\n");
		dispatch(getCellContent(n.getBody()));
		print("\n");
		
	}

	@Override
	public void visit(WtTableRow n) {

		boolean cellsDefined = false;
		for (WtNode cell : n.getBody()) {
			switch (cell.getNodeType()) {
			case WtNode.NT_TABLE_CELL:
			case WtNode.NT_TABLE_HEADER:
				cellsDefined = true;
				break;
			}
		}
		if (cellsDefined) {
			print("\n");
			dispatch(getCellContent(n.getBody()));
			print("\n");
		} else {
			iterate(n.getBody());
		}
		
		
	}
	
	@Override
	public void visit(WtTableCell n) {
		
		print("\n");
        dispatch(getCellContent(n.getBody()));
        print("\n");
		
		
	}

	@Override
	public void visit(WtTableHeader n) {
		
		print("\n");
        dispatch(getCellContent(n.getBody()));
        print("\n");
		
	}

	
	@Override
	public void visit(WtUrl linkUrl) {
		String url = ""; 
		if (linkUrl.getProtocol() == "")
			url =  linkUrl.getPath();
		else
			url = linkUrl.getProtocol() + ":" + linkUrl.getPath();
		print(url);
	}
	
	
	
	// DON'T KNOW, BUT  THIS SEEMS TO WORK
	@Override
	public void visit(WtValue n) {
		iterate(n);
	}
	@Override
	public void visit(WtNodeList list) {
		iterate(list);
	}
	@Override
	public void visit(WtLinkTitle title) {
		iterate(title);	
	}
	@Override
	public void visit(WtName name) {
		iterate(name);
	}
	@Override
	public void visit(WtOnlyInclude n) {
		iterate(n);
	}
	@Override
	public void visit(WtParsedWikitextPage n) {
		iterate(n);
	}
	@Override
	public void visit(WtPreproWikitextPage n) {
		iterate(n);
	}
	@Override
	public void visit(EngProcessedPage n) {
		dispatch(n.getPage());
	}
	@Override
	public void visit(EngSoftErrorNode n) {
		visit((WtXmlElement) n);
	}
	
	@Override
	public void visit(WtXmlElement n) {
		if (n.hasBody()) {
			if (blockElements.contains(n.getName().toLowerCase())) {
				print("\n");
				dispatch(n.getBody());
				print("\n");
			} else if(blockedElements.contains(n.getName().toLowerCase())) {
				//ignore
			}
			else {
				dispatch(n.getBody());
			}
		} else {
			if (n.getName().equals("br")){
				print('\n');
			}
		}
	}
	// IGNORED ELEMENTS

	@Override
	public void visit(WtXmlAttribute n) {}
	@Override
	public void visit(WtImageLink arg0) {}
	@Override
	public void visit(WtLinkOptionGarbage arg0) {}
	@Override
	public void visit(WtIgnored arg0) {}
	@Override
	public void visit(WtPageSwitch arg0) {}
	@Override
	public void visit(WtXmlAttributeGarbage arg0) {}
	@Override
	public void visit(WtXmlComment arg0) {}
	@Override
	public void visit(WtRedirect link) {}
	@Override
	public void visit(WtXmlAttributes n) {}
	
	// SHOULD NOT HAPPEN
	@Override
	public void visit(WtPageName arg0) {
		this.shouldNotHappen(arg0);
	}
	@Override
	public void visit(WtLinkOptionLinkTarget arg0) {
		this.shouldNotHappen(arg0);
	}
	@Override
	public void visit(WtLinkOptionAltText arg0) {
		this.shouldNotHappen(arg0);
	}
	@Override
	public void visit(WtLinkOptions arg0) {
		this.shouldNotHappen(arg0);
	}
	@Override
	public void visit(WtTicks arg0) {
		this.shouldNotHappen(arg0);
	}
	@Override
	public void visit(WtImEndTag arg0) {
		//TODO: Sweble bug
		//this.shouldNotHappen(arg0);
	}
	@Override
	public void visit(WtLinkOptionKeyword arg0) {
		this.shouldNotHappen(arg0);
	}
	@Override
	public void visit(WtLinkOptionResize arg0) {
		this.shouldNotHappen(arg0);
	}
	@Override
	public void visit(WtImStartTag arg0) {
		this.shouldNotHappen(arg0);
	}
	@Override
	public void visit(WtTagExtensionBody arg0) {
		this.shouldNotHappen(arg0);
	}

	// NOT IMPLEMENTED ELEMENTS
	
	@Override
	public void visit(WtXmlEmptyTag arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(WtXmlStartTag arg0) {
		// TODO Auto-generated method stub
		
	}	
	
	@Override
	public void visit(WtXmlEndTag arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void visit(WtTemplate arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void visit(WtTemplateArguments arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void visit(WtTemplateArgument arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void visit(WtTemplateParameter arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(WtSignature arg0) {
		// TODO Auto-generated method stub
		
	}

	//PRINTING FUNCTIONS
	private void indent(){
		for(int i = 0; i < indentLevel; i++){
			print('\t');
		}
	}
	
	private void print(String text){
		text = SwebleUtils.replaceTags(
				SwebleUtils.replaceHTMLEntities(text, null)
				,null);
		text = SwebleUtils.removeInterWikiPrefix(text, interWikiPrefixSet);
		sbText.append(text);
	}
	private void print(char c){
		sbText.append(c);
	}
	
	private void shouldNotHappen(WtNode node){
		LOG.warning("Unexpected behavior: Visiting " + node + "("+this.articleTitle.getCanonicalTitle()+")");
	}
	
	/**
	 * CODE BELOW:
	 * Copied from Sweble HtmlRenderer
     */
	private void fixTableBody(WtNodeList body) {
		boolean hadRow = false;
		WtTableRow implicitRow = null;
		for (WtNode c : body) {
			switch (c.getNodeType()) {
			case WtNode.NT_TABLE_HEADER: //SAME THING
			case WtNode.NT_TABLE_CELL: 
				if (hadRow) {
					dispatch(c);
				} else {
					if (implicitRow == null)
						implicitRow = nf
								.tr(nf.emptyAttrs(), nf.body(nf.list()));
					implicitRow.getBody().add(c);
				}
				break;
			

			case WtNode.NT_TABLE_CAPTION: 
				if (!hadRow && implicitRow != null)
					dispatch(implicitRow);
				implicitRow = null;
				dispatch(c);
				break;
			

			case WtNode.NT_TABLE_ROW: 
				if (!hadRow && implicitRow != null)
					dispatch(implicitRow);
				hadRow = true;
				dispatch(c);
				break;

			default:
				if (!hadRow && implicitRow != null)
					implicitRow.getBody().add(c);
				else
					dispatch(c);
				break;
			}
		}
	}
	
	/**
     * If the cell content is only one paragraph, the content of the paragraph
     * is returned. Otherwise the whole cell content is returned. This is done
     * to render cells with a single paragraph without the paragraph tags.
     */
	protected static WtNode getCellContent(WtNodeList body) {
		if (body.size() >= 1 && body.get(0) instanceof WtParagraph) {
			boolean ok = true;
			for (int i = 1; i < body.size(); ++i) {
				if (!(body.get(i) instanceof WtNewline)) {
					ok = false;
					break;
				}
			}
			if (ok)
				body = (WtParagraph) body.get(0);
		}
		return body;
	}

	protected WtNodeList cleanAttribs(WtNodeList xmlAttributes) {
		ArrayList<WtXmlAttribute> clean = null;

		WtXmlAttribute style = null;
		for (WtNode a : xmlAttributes) {
			if (a instanceof WtXmlAttribute) {
				WtXmlAttribute attr = (WtXmlAttribute) a;
				if (!attr.getName().isResolved())
					continue;

				String name = attr.getName().getAsString().toLowerCase();
				if (name.equals("style")) {
					style = attr;
				} else if (name.equals("width")) {
					if (clean == null)
						clean = new ArrayList<WtXmlAttribute>();
					clean.add(attr);
				} else if (name.equals("align")) {
					if (clean == null)
						clean = new ArrayList<WtXmlAttribute>();
					clean.add(attr);
				}
			}
		}

		if (clean == null || clean.isEmpty())
			return xmlAttributes;

		String newStyle = "";
		if (style != null)
			newStyle = cleanAttribValue(style.getValue());

		for (WtXmlAttribute a : clean) {
			if (!a.getName().isResolved())
				continue;

			String name = a.getName().getAsString().toLowerCase();
			if (name.equals("align")) {
				newStyle = String.format("text-align: %s; ",
						cleanAttribValue(a.getValue()))
						+ newStyle;
			} else {
				newStyle = String.format("%s: %s; ", name,
						cleanAttribValue(a.getValue()))
						+ newStyle;
			}
		}

		WtXmlAttribute newStyleAttrib = nf.attr(
				nf.name(nf.list(nf.text("style"))),
				nf.value(nf.list(nf.text(newStyle))));

		WtNodeList newAttribs = nf.attrs(nf.list());
		for (WtNode a : xmlAttributes) {
			if (a == style) {
				newAttribs.add(newStyleAttrib);
			} else if (clean.contains(a)) {
				// Remove
			} else {
				// Copy the rest
				newAttribs.add(a);
			}
		}

		if (style == null)
			newAttribs.add(newStyleAttrib);

		return newAttribs;
	}

	protected String cleanAttribValue(WtNodeList value) {
		try {
			return StringUtils.collapseWhitespace(tu.astToText(value)).trim();
		} catch (StringConversionException e) {
			//logger.warning("Error for " + value.toString() + " " +e.getMessage());
			return "";
		}
	}

	protected static final Set<String> blockElements = new HashSet<String>();
	static {
		// left out del and ins, added table elements
		blockElements.add("div");
		blockElements.add("address");
		blockElements.add("blockquote");
		blockElements.add("center");
		blockElements.add("dir");
		blockElements.add("div");
		blockElements.add("dl");
		blockElements.add("fieldset");
		blockElements.add("form");
		blockElements.add("h1");
		blockElements.add("h2");
		blockElements.add("h3");
		blockElements.add("h4");
		blockElements.add("h5");
		blockElements.add("h6");
		blockElements.add("hr");
		blockElements.add("isindex");
		blockElements.add("menu");
		blockElements.add("noframes");
		blockElements.add("noscript");
		blockElements.add("ol");
		blockElements.add("p");
		blockElements.add("pre");
		blockElements.add("table");
		blockElements.add("ul");
		blockElements.add("center");
		blockElements.add("caption");
		blockElements.add("tr");
		blockElements.add("td");
		blockElements.add("th");
		blockElements.add("colgroup");
		blockElements.add("thead");
		blockElements.add("tbody");
		blockElements.add("tfoot");
	}
	
	protected static final Set<String> blockedElements = new HashSet<String>();
	static {
		// tag to be ignored
		blockElements.add("noinclude"); //automatically excluded by sweble, added here just in case
	}
}
