package org.wikibrain.parser.xml;

import org.apache.commons.lang.StringEscapeUtils;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.core.model.Title;
import org.wikibrain.parser.WpParseException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the Xml associated with a single Wikipedia page.
 */
public class PageXmlParser {
    private static final Logger LOG =LoggerFactory.getLogger(PageXmlParser.class);
    private static final Pattern TITLE_PATTERN = Pattern.compile("<title>(.*?)</title>");
    private static final Pattern ID_PATTERN = Pattern.compile("<id>(.*?)</id>");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("<timestamp>(.*?)</timestamp>");
    private static final Pattern CONTENT_PATTERN = Pattern.compile("<text xml:space=\"preserve\">(.*?)</text>", Pattern.DOTALL);
    private static final Pattern SELF_CLOSING_CONTENT_PATTERN = Pattern.compile("<text xml:space=\"preserve\"\\s*/>", Pattern.DOTALL);
    private static final Pattern REDIRECT_PATTERN = Pattern.compile("<redirect title=\"(.*?)\" />");

    private static final Pattern MODEL_PATTERN = Pattern.compile("<model>(.*?)</model>");
    private static final Pattern FORMAT_PATTERN = Pattern.compile("<format>(.*?)</format>");

    // xmlDumpDateFormat is not static because it isn't threadsafe. BOOO!!
    private final SimpleDateFormat xmlDumpDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private final LanguageInfo language;

    public PageXmlParser(LanguageInfo language) {
        this.language = language;
    }

    public RawPage parse(String rawXml) throws WpParseException {
        return parse(rawXml, -1, -1);
    }

    /**
     * Parses a single xml page into the main page components
     * @param rawXml
     * @param startByte
     * @param stopByte
     * @return
     * @throws WpParseException
     */
    public RawPage parse(String rawXml, long startByte, long stopByte) throws WpParseException {
        rawXml = StringEscapeUtils.unescapeHtml(rawXml);
        String title = extractSingleString(TITLE_PATTERN, rawXml, 1);
        String idString = extractSingleString(ID_PATTERN, rawXml, 1);
        String timestampString = extractSingleString(TIMESTAMP_PATTERN, rawXml, 1);
        String revisionIdString = extractSingleString(ID_PATTERN, rawXml, 2);
        String formatString = extractSingleString(FORMAT_PATTERN, rawXml, 1);
        String modelString = extractSingleString(MODEL_PATTERN, rawXml, 1);

        if (title == null) {
            throw new WpParseException("no title for article");
        }
        if (idString == null) {
            throw new WpParseException("no id for article");
        }
        if (revisionIdString == null) {
            throw new WpParseException("no revision id for article");
        }

        String body = extractSingleString(CONTENT_PATTERN, rawXml, 1);
        if (body == null && SELF_CLOSING_CONTENT_PATTERN.matcher(rawXml).find()) {
            body = "";
        }
        if (body == null) {
            System.err.println("invalid body: " + rawXml);
            body = "";
        }
        Date lastEdit = null;
        try {
            lastEdit = xmlDumpDateFormat.parse(timestampString);
        } catch (ParseException e) {
            LOG.warn("Could not parse last edited date: " + timestampString);
        }
        title = title.trim();
        String redirectTitle = getRedirect(rawXml);
        RawPage rp = new RawPage(
                Integer.valueOf(idString),
                Integer.valueOf(revisionIdString),
                title,
                body,
                lastEdit,
                language.getLanguage(),
                getNameSpace(title),
                redirectTitle!=null,
                false,   // TODO: FIXME by properly parsing disambigs!
                redirectTitle
        );
        if (formatString != null) {
            rp.setFormat(formatString);
        }
        if (modelString != null) {
            rp.setModel(modelString);
        }
        return rp;
    }

    // TODO: does this method need to be like this, or can it just return "type"
    private NameSpace getNameSpace(String title) {
        return new Title(title, language).getNamespace();
    }

    private String getRedirect(String rawXml) {
        return extractSingleString(REDIRECT_PATTERN, rawXml, 1);
    }

    private static String extractSingleString(Pattern patternToMatch, String body, int matchNum){
        if (patternToMatch == null || body == null) {
            return null;
        }
        Matcher matcher = patternToMatch.matcher(body);
        int counter = 0;
        String curGroup = null;
        while (matcher.find() && counter < matchNum){
            curGroup = matcher.group(1);
            counter++;
        }
        return curGroup;
    }
}
