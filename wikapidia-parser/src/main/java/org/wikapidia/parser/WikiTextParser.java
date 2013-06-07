package org.wikapidia.parser;

import de.tudarmstadt.ukp.wikipedia.parser.*;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParser;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParserFactory;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.PageType;
import org.wikapidia.core.model.Title;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiTextParser {
    public static final Logger LOG = Logger.getLogger(WikiTextParser.class.getName());

    private final MediaWikiParser jwpl;
    private final SubarticleParser subarticleParser;
    private final LanguageInfo lang;

    public WikiTextParser(LanguageInfo lang) {
        this.lang = lang;
        subarticleParser = new SubarticleParser(lang);

        MediaWikiParserFactory pf = new MediaWikiParserFactory();
        pf.setCalculateSrcSpans(true);
        pf.setCategoryIdentifers(lang.getCategoryNames());
        // TODO: why was there more language originally passed below?
        pf.setLanguageIdentifers(Arrays.asList(lang.getLanguage().getLangCode()));
        jwpl = pf.createParser();

    }

    /**
     * TODO: change exception to WpParseException
     * @param xml
     * @param visitor
     * @throws WikapidiaException
     */
    public void parse(PageXml xml, Visitor visitor) throws WikapidiaException {
        ParsedPage pp = jwpl.parse(xml.getBody());

        if (xml.getType() == PageType.REDIRECT) {
            // TODO: calculate redirect text?
            visitor.redirect(xml, null);
        } else if (xml.getType() == PageType.CATEGORY) {
            parseCategory(xml, pp, visitor);
        } else if (xml.getType() == PageType.ARTICLE) {
            parseArticle(xml, visitor, pp);
        }
    }

    private void parseRedirect(PageXml xml, Visitor visitor, ParsedPage page) {
    }

    private void parseArticle(PageXml xml, Visitor visitor, ParsedPage pp) {   		// *** LINKS, ANCHOR TEXTS, SECTIONS
        Title title = new Title(xml.getTitle(), lang);
        int secNum = 0;
        int paraNum = 0;

        Boolean foundFirstParagraph = false;

        for (Section curSection: pp.getSections()){
            try{
                SubarticleParser.EncodingType secSubType = subarticleParser.isSeeAlsoHeader(lang, curSection.getTitle());
                for (Content curContent : curSection.getContentList()){

                    Boolean isFirstParagraph = false;
                    if (!foundFirstParagraph && (curContent instanceof Paragraph)){
                        isFirstParagraph = (paraNum == pp.getFirstParagraphNr());
                        if (isFirstParagraph) foundFirstParagraph = true;
                        paraNum++;
                    }

                    // EASY LINKS
                    for (Link curLink : curContent.getLinks()){
                        if (curLink.getTarget().length() == 0){
                            LOG.warning("Found link with empty target: \t" + xml + "\t text=" + curLink.getText());
                            continue;
                        }
                        Title destTitle = link2Title(curLink);
                        if (destTitle.guessType() != PageType.ARTICLE){
                            continue;
                        }
                        try{
                            SubarticleParser.EncodingType linkSubType;
                            if (secSubType == null){ // don't look for inlines in "see also"
                                linkSubType = subarticleParser.isInlineSubarticle(curLink.getSrcSpan().getStart(), xml);
                            }else{
                                linkSubType = secSubType; // captures see also
                            }
                            parseLink(
                                    xml, visitor, title, destTitle, curLink.getText(),
                                    curLink.getSrcSpan().getStart(), isFirstParagraph,
                                    (secNum == 0), linkSubType);
                        } catch (WikapidiaException e) {
                            LOG.log(Level.WARNING, String.format("Could not process link\t%s\t%s", xml, curLink.toString()),e);
                        }
                    }

                    //TEMPLATES
                    for (Template t : curContent.getTemplates()){
                        boolean errorWithSrcLocation = t.getSrcSpan().getEnd() < 0; // this checks for what seems to be when parsing fails in JWPL
                        String templateTextOrig;
                        if (!errorWithSrcLocation){
                            templateTextOrig = xml.getBody().substring(t.getSrcSpan().getStart(), t.getSrcSpan().getEnd());
                        }else{ // this makes up for errors in JWPL (or bad script, but it mostly looks like erros)
                            int estimatedLength = t.getPos().getEnd() - t.getPos().getStart();
                            templateTextOrig = xml.getBody().substring(t.getSrcSpan().getStart(), t.getSrcSpan().getStart() + estimatedLength + 1);
                        }
                        String templateText;
                        if (templateTextOrig.length() >= 5){
                            templateText = templateTextOrig.substring(2, templateTextOrig.length()-2);
                        }else{
                            continue; // blank template
                        }
                        String templateName = t.getName(); // SUBARTICLE INFO STUFF
                        templateName = new Title(templateName, false, lang).toString(); // this appears to be necessary due to JWPL's handling of template names
                        SubarticleParser.EncodingType tempSubType;
                        tempSubType = subarticleParser.isTemplateSubarticle(templateName, templateText);
                        if (tempSubType == null){
                            try{
                                templateText = templateText.replaceAll("\\{\\{", ""); // <-- these are all special cases in which JWPL fails
                                templateText = templateText.replaceAll("\\}\\}", "");
                                templateText = templateText.replaceAll("<!--", "");
                                templateText = templateText.replaceAll("\\[\\[\\]\\]", "");
                                ParsedPage parsedTemplate = jwpl.parse(templateText);
                                for (Link templateLink : parsedTemplate.getLinks()){
                                    Title destTitle = link2Title(templateLink);
                                    PageType type = destTitle.guessType();
                                    if (type == PageType.ARTICLE){
                                            parseLink(xml, visitor, title, destTitle, templateLink.getText(),
                                                    t.getSrcSpan().getStart(), isFirstParagraph, (secNum == 0),
                                                    tempSubType);
                                    } else if (type == PageType.CATEGORY){
                                        throw new RuntimeException("Found a category link in a template");
                                    }
                                }
                            }catch(IndexOutOfBoundsException e){
                                LOG.severe("Parsing error while doing templates -> ParsedPages:\t" + xml + "\t" + templateText);
                            }

                        }else{
                            List<String> dests = subarticleParser.getContentsOfTemplatePipe(templateText);
                            for (String dest : dests){
                                dest = subarticleParser.removeTemplateAnchor(dest);
                                Title destTitle = new Title(dest, lang);
                                try {
                                    parseLink(xml, visitor, title, destTitle, dest, t.getSrcSpan().getStart(),
                                            isFirstParagraph, (secNum == 0), tempSubType);

                                } catch (WikapidiaException e) {
                                    LOG.log(Level.SEVERE,
                                            String.format("Could not process template-based subarticle link: \t%s\t%s", xml, t.toString()), e);
                                }
                            }
                        }

                    }
                }
            } catch(WikapidiaException e){
                LOG.log(Level.SEVERE, String.format("Could not store whole section in %s", xml), e);
            }
            secNum++;
        }


        // *** ILLS
        parseIlls(xml, pp, visitor);

        // *** CATEGORY MEMBERSHIPS
        for (Link cat : pp.getCategories()){
            String linkText = cat.getText();
            if (linkText.contains(Pattern.quote("|"))){
                continue;
            }
            Title destTitle = new Title(cat.getTarget(), false, lang);
            // TODO: ensure destTitle is a category
            try {
                visitor.category(xml, destTitle);

            } catch (WikapidiaException e) {
                LOG.log(Level.WARNING, String.format("Could not process category membership\t%s\t%s", xml, destTitle),e);
            }
        }
    }

    private static Pattern illPattern = Pattern.compile("(.+?)\\:\\s*(.+)");
    private void parseIlls(PageXml xml, ParsedPage pp, Visitor visitor) {
        if (pp.getLanguagesElement() !=  null){
            for (Link ill : pp.getLanguages()){
                try{
                    Matcher m = illPattern.matcher(ill.getTarget());
                    if (m.find()){
                        String langCode = m.group(1);
                        String target = m.group(2);
                        Language l = Language.getByLangCode(langCode);
                        if (l == null) {
                            LOG.warning("unkonwn lang code:\t" + langCode);
                        } else if (l != lang.getLanguage()) {
                            visitor.ill(xml, new Title(target, false, lang));
                        }
                    }else{
                        LOG.warning("Invalid ILL:\t" + xml + "\t" + ill.getTarget());
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, String.format("Error while parsing/storing ILL\t%s\t%s\t%s",xml,ill.toString().replaceAll("\n", ","), e.getMessage()));
                }
            }
        }else{
            LOG.info("No ILLs found for\t" + xml);
        }

    }

    private void parseCategory(PageXml xml, ParsedPage pp, Visitor visitor){
        Title title = new Title(xml.getTitle(), lang);

        // handle links
        for (Link catMem : pp.getCategories()){
            try {
                Title destTitle = new Title(catMem.getTarget(), lang);
                // TODO: ensure title is a category
                parseLink(xml, visitor, title, destTitle, catMem.getText(), null,
                        null, null, null);
            } catch (WikapidiaException e) {
                LOG.log(Level.WARNING, String.format("Could not parse/store link\t%s\t%s", xml, catMem.toString()), e);
            }
        }

        // handle ILLs
        parseIlls(xml, pp, visitor);
    }


    private void parseLink(PageXml xml, Visitor visitor, Title src, Title dest, String linkText, Integer location,
                                     Boolean isFirstParagraph, Boolean isFirstSection, SubarticleParser.EncodingType subType) throws WikapidiaException{

        // don't want to consider within-page links
        if (src.toString().startsWith("#") || src.equals(dest)) {
            return;
        }
        visitor.link(xml, src, linkText, location, isFirstParagraph, isFirstSection, subType != null);

        /**
         * TODO: are there reasons to specially capture anchor texts independent of links?
        // deal with anchor texts
        if (type.equals(PageXMLType.ARTICLE)){
            AnchorTextShell atShell = new AnchorTextShell(curPageXML, linkText);
            handler.handleNewAnchorTextShell(atShell, destShell);
        }
         */

        // deal with subarticle infos
        if (subType != null && dest.guessType() == PageType.ARTICLE) {
            visitor.subarticleLink(xml, dest, location, subType);
        }

    }

    private PageType getLinkType(Link link){
        Title t = link2Title(link);
        return t == null ? null : t.guessType();
    }

    private Title link2Title(Link link) {
        if (link.getType().equals(Link.type.INTERNAL) || link.getType().equals(Link.type.UNKNOWN)) {
            return new Title(link.getTarget(), lang);
        } else {
            return null;
        }
    }


    public static interface Visitor {
        public void category(PageXml xml, Title category) throws WikapidiaException;
        public void ill(PageXml xml, Title title) throws WikapidiaException;
//        public void section(SectionShell shell) throws WikapidiaException;
//        public void paragraph(SectionShell shell) throws WikapidiaException;

        // TODO: isFirstParagraph and isFirstSection should be able to be tracked by the consumer and not passed
        public void link(PageXml xml, Title target, String text,
                         int location, boolean isFirstParagraph, boolean isFirstSection,
                         boolean isSubarticle) throws WikapidiaException;

        public void subarticleLink(PageXml xml, Title subarticle, int location, SubarticleParser.EncodingType type);

        public void redirect(PageXml xml, String redirect) throws WikapidiaException;
    }
}
