package org.wikapidia.parser;

import org.apache.commons.lang3.StringEscapeUtils;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.PageType;
import org.wikapidia.core.model.Title;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the Xml associated with a single Wikipedia page.
 * TODO: figure out pageType
 */
public class PageXmlParser {
    private static final Logger LOG =Logger.getLogger(PageXmlParser.class.getName());
    private static final Pattern titlePattern = Pattern.compile("<title>(.*?)</title>");
    private static final Pattern idPattern = Pattern.compile("<id>(.*?)</id>");
    private static final Pattern timestampPattern = Pattern.compile("<timestamp>(.*?)</timestamp>");
    private static final SimpleDateFormat xmlDumpDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private static final Pattern contentPattern = Pattern.compile("<text xml:space=\"preserve\">(.*?)</text>", Pattern.DOTALL);
    private final LanguageInfo language;

    public PageXmlParser(LanguageInfo language) {
        this.language = language;
    }

    public PageXml parse(String rawXml) throws WpParseException {
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
    public PageXml parse(String rawXml, long startByte, long stopByte) throws WpParseException {
        rawXml = StringEscapeUtils.unescapeHtml4(rawXml);
        String title = extractSingleString(titlePattern, rawXml, 1);
        String idString = extractSingleString(idPattern, rawXml, 1);
        String timestampString = extractSingleString(timestampPattern, rawXml, 1);
        String revisionIdString = extractSingleString(idPattern, rawXml, 2);

        if (title == null) {
            throw new WpParseException("no title for article");
        }
        if (idString == null) {
            throw new WpParseException("no id for article");
        }
        if (revisionIdString == null) {
            throw new WpParseException("no revision id for article");
        }

        String body = extractSingleString(contentPattern, rawXml, 1);
        Date lastEdit = null;
        try {
            lastEdit = xmlDumpDateFormat.parse(timestampString);
        } catch (ParseException e) {
            LOG.warning("Could not parse last edited date: " + timestampString);
        }
        title = title.trim();

        return new PageXml(
                Integer.valueOf(idString),
                Integer.valueOf(revisionIdString),
                title,
                body,
                lastEdit,
                getPageType(title, body),
                language.getLanguage(),
                startByte,
                stopByte
        );
    }

    private PageType getPageType(String title, String body) {
        PageType type = new Title(title, language).guessType();
        if (type == PageType.ARTICLE) {
            if (extractSingleString(language.getRedirectPattern(), body, 1) != null) {
                return PageType.REDIRECT;
            } else {
                return PageType.ARTICLE;
            }
        } else if (type == PageType.CATEGORY) {
            return PageType.CATEGORY;
        } else {
            return PageType.SPECIAL;
        }
    }

    private static String extractSingleString(Pattern patternToMatch, String body, int matchNum){
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
