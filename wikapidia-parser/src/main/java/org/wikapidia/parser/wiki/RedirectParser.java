package org.wikapidia.parser.wiki;

import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.Title;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class RedirectParser {
    private final LanguageInfo language;

    public RedirectParser(Language language) {
        this.language = LanguageInfo.getByLanguage(language);
    }

    public boolean isRedirect(String body) {
        return extractSingleString(language.getRedirectPattern(), body, 1) != null;
    }

    public Title getRedirect(String body) {
        String title =  extractSingleString(language.getRedirectPattern(), body, 1);
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
