package org.wikibrain.core.nlp;

import java.util.regex.Pattern;

/**
 * The WikiMarkup cleaner as implemented in FastText:
 * https://github.com/facebookresearch/fastText/blob/master/get-wikimedia.sh
 */
public class MarkupCleaner {


    private static Pattern PATTERN_REF = Pattern.compile("<ref[^<]*</ref>");
    private static Pattern PATTERN_XHTML = Pattern.compile("<[^>]*>");
    private static Pattern PATTERN_URL = Pattern.compile("\\[http:[^] ]*");
    private static Pattern PATTERN_THUMB = Pattern.compile("\\|(thumb|left|right|(\\d+px))", Pattern.CASE_INSENSITIVE);
    private static Pattern PATTERN_IMAGE = Pattern.compile("\\[\\[image:[^\\[\\]]*\\|", Pattern.CASE_INSENSITIVE);
    private static Pattern PATTERN_CATEGORIES = Pattern.compile("\\[\\[category:([^|\\]]*)[^]]*\\]\\]");
    private static Pattern PATTERN_ILL = Pattern.compile("\\[\\[[a-z\\-]*:[^\\]]*\\]\\]/");
    private static Pattern PATTERN_WIKI_URL = Pattern.compile("\\[\\[[^|\\]]*\\|");
    private static Pattern PATTERN_DOUBLE_CURLY = Pattern.compile("\\{\\{[^}]*\\}\\}");
    private static Pattern PATTERN_CURLY = Pattern.compile("\\{[^}]*\\}");
    private static Pattern PATTERN_URLENC = Pattern.compile("&[^;]*;");
    private static Pattern PATTERN_WHITESPACE = Pattern.compile("\\s+");

    public static String cleanText(String text) {

        // remove references <ref...> ... </ref>
        text = PATTERN_REF.matcher(text).replaceAll("");

        // remove xhtml tags
        text = PATTERN_XHTML.matcher(text).replaceAll("");

        // remove normal url, preserve visible text
        text = PATTERN_URL.matcher(text).replaceAll("[");

        // remove images
        text = PATTERN_THUMB.matcher(text).replaceAll("");
        text = PATTERN_IMAGE.matcher(text).replaceAll("");

        // Remove categories without markup
        text = PATTERN_CATEGORIES.matcher(text).replaceAll("[[$1]]");

        // Remove links to other languages
        text = PATTERN_ILL.matcher(text).replaceAll(" ");

        // Remove wiki url, preserve visible text
        text = PATTERN_WIKI_URL.matcher(text).replaceAll("[[");

        // Remove {{icons}} and {tables}
        text = PATTERN_DOUBLE_CURLY.matcher(text).replaceAll(" ");
        text = PATTERN_CURLY.matcher(text).replaceAll(" ");

        // Remove [ and ]
        text = text.replace("[", "");
        text = text.replace("]", "");

        // remove URL encoded chars
        text = PATTERN_URLENC.matcher(text).replaceAll(" ");

        // FastText code:
        //     sed -e "s/’/'/g" -e "s/′/'/g" -e "s/''/ /g" -e "s/'/ ' /g" -e "s/“/\"/g" -e "s/”/\"/g" \
        //     -e 's/"/ " /g' -e 's/\./ \. /g' -e 's/<br \/>/ /g' -e 's/, / , /g' -e 's/(/ ( /g' -e 's/)/ ) /g' -e 's/\!/ \! /g' \
        //     -e 's/\?/ \? /g' -e 's/\;/ /g' -e 's/\:/ /g' -e 's/-/ - /g' -e 's/=/ /g' -e 's/=/ /g' -e 's/*/ /g' -e 's/|/ /g' \
        //            -e 's/«/ /g' | tr 0-9 " "

        text = text
                .replace('’', '\'')
                .replace('′', '\'')
                .replace("''", " ")
                .replace("'", " ' ")
                .replace('“', '"')
                .replace('”', '"');

        text = text
                .replace("\"", " \" ")
                .replace(".", " . ")
                .replace("<br />", " ")
                .replace(", ", " , ")
                .replace("(", " ( ")
                .replace(")", " ) ")
                .replace("!", " ! ");

        text = text
                .replace("?", " ? ")
                .replace(";", " ")
                .replace(":", " ")
                .replace("-", " - ")
                .replace("=", " ")
                .replace("*", " ")
                .replace("|", " ")
                .replace("«", " ")
                .replaceAll("[0-9]", " ");

        text = PATTERN_WHITESPACE.matcher(text).replaceAll(" ");

        return text.trim();
    }

}
