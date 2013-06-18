package org.wikapidia.download;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Given an Html Page, finds a certain type of links
 */
public enum LinkMatcher {

    ARTICLES ("articles",
               Pattern.compile(".*wiki-latest-pages-articles[0-9]+.xml-.*.bz2"),
               Pattern.compile(".*wiki-latest-pages-articles.xml.*.bz2")),
    LINK_SQL ("links", Pattern.compile(".*wiki-latest-pagelinks.sql.gz")),
    ABSTRACT ("abstract", Pattern.compile(".*wiki-latest-abstract[0-9]*.xml")),
    TITLES   ("titles", Pattern.compile(".*wiki-latest-all-titles-in-ns0.gz")),
    CATEGORY ("category", Pattern.compile(".*wiki-latest-category.sql.gz"));

    private String name;
    private Pattern[] patterns;

    /**
     * Constructs a new link finder.
     * @param name Name of the link finder
     * @param patterns A set of patterns that is tried in consecutive order until
     *                 at least one match is found.
     */
    LinkMatcher(String name, Pattern... patterns) {
        this.name = name;
        this.patterns = patterns;
    }

    /**
     * Given a set of Strings containing urls, returns the subset of strings
     * that match one of the specified regular expressions.
     * @param links
     * @return
     */
    public List<String> match(List<String> links) {
        List<String> result = new ArrayList<String>();
        for (Pattern p : patterns) {
            for (String link : links) {
                if (p.matcher(link).matches()) {
                    result.add(link);
                }
            }
            if (result.size() > 0) {
                break;
            }
        }
        return result;
    }

    public String getName() {
        return name;
    }
}
