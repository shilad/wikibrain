package org.wikibrain.parser.wiki;

import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.model.Title;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class RedirectParser {
    private final LanguageInfo language;
    private static final Pattern redirectPattern = Pattern.compile("<redirect title=\"(.*?)\" />");

    public RedirectParser(Language language) {
        this.language = LanguageInfo.getByLanguage(language);
    }

    public boolean isRedirect(String body) {
        return extractSingleString(redirectPattern, body, 1) != null;
    }

    public Title getRedirect(String body) {
        String title =  extractSingleString(redirectPattern, body, 1);
        return new Title(title, language);
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
