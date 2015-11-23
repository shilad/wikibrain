package org.wikibrain.core.cmd;

import org.wikibrain.core.lang.Language;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Yulun Li, Shilad Sen
 *
 * Matches database dump files published by Wikipedia at:
 * http://dumps.wikimedia.org based on their names.
 *
 * Extracts counter number (if a certain type of file is split into multiple pieces)
 * and language.
 *
 */
public enum FileMatcher {

    MULTISTREAM       (
            "multistream",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-pages-articles-multistream.xml.bz2")),
    MULTISTREAM_INDEX (
            "multistream_index",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-pages-articles-multistream-index\\.txt\\.bz2")),
    EDIT_HISTORY_7z  (
            "edit_history_7z",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-pages-meta-history(\\d+).xml-.+\\.7z"),
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-pages-meta-history.xml.7z")),
    EDIT_HISTORY_bz2 (
            "edit_history_bz2",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-pages-meta-history(\\d+).xml-.+\\.bz2"),
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-pages-meta-history.xml.bz2")),
    LOG (
            "log_events",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-pages-logging.xml.gz")),
    META_CURRENT (
            "current_meta",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-pages-meta-current(\\d+).xml-.+\\.bz2"),
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-pages-meta-current.xml.bz2")),
    ARTICLES          ("articles",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-pages-articles(\\d+)\\.xml-.+\\.bz2"),
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-pages-articles.xml.bz2")),
    STUB_ARTICLES     (
            "stub_articles",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-stub-articles\\d*.xml.gz")),
    STUB_META_CURRENT (
            "stub_meta_current",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-stub-meta-current\\d*.xml.gz")),
    STUB_META_HISTORY (
            "stub_meta_histories",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-stub-meta-history\\d*.xml.gz")),
    ABSTRACT          (
            "abstracts",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-abstract(\\d+)\\.xml"),
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-abstract.*(?<!(\\.xml\\-rss))\\.xml")),
    TITLES            (
            "titles",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-all-titles-in-ns0.gz")),
    INTERLINK         (
            "interwiki_links",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-iwlinks.sql.gz")),
    REDIRECT_LIST     (
            "redirect_lists",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-redirect.sql.gz")),
    PROTECTED_TITLES  (
            "protected_titles",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-protected_titles.sql.gz")),
    NAME_PAIRS        (
            "name-pairs",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-page_props.sql.gz")),
    PAGE_RESTRICTIONS (
            "page_restrictions",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-page_restrictions.sql.gz")),
    PAGE_BASE         (
            "base_page_datas",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-page.sql.gz")),
    CATEGORY          (
            "categories",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-category.sql.gz")),
    USER_GROUP        (
            "user_groups",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-user_groups.sql.gz")),
    INTERWIKI         (
            "interwiki_prefixes",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-interwiki.sql.gz")),
    INTERLANG_LINKS   (
            "interlang_links",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-langlinks.sql.gz")),
    EXTERNAL_LINKS    (
            "external_links",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-externallinks.sql.gz")),
    TEMPLATE_LINKS    (
            "template_links",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-templatelinks.sql.gz")),
    IMAGE_LINKS       (
            "image_links",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-imagelinks.sql.gz")),
    CATEGORY_LINKS    (
            "category_links",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-categorylinks.sql.gz")),
    LINK_SQL          (
            "links",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-pagelinks.sql.gz")),
    OLD_MEDIA_META    (
            "old_media_metas",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-oldimage.sql.gz")),
    CURRENT_MEDIA_META(
            "current_media_metas",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-image.sql.gz")),
    SITE_STATS        (
            "site_stats",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-site_stats.sql.gz")),
    FLAGGED_REVISION  (
            "flagged_revisions",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-flaggedrevs.sql.gz")),
    FLAGGED_PAGES     (
            "flagged_pages",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-flaggedpages.sql.gz")),
    WIKIDATA_ITEMS (
            "wikidata_items",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-wb_items_per_site.sql.gz")),
    MD5               (
            "md5_checksums",
            Pattern.compile(".*?([a-zA-Z_-]+)wiki.+-md5sums.txt"));


    private String name;
    private Pattern[] patterns;

    /**
     * Constructs a new link finder.
     * @param name Name of the link finder
     * @param patterns A set of patterns that is tried in consecutive order until
     *                 at least one match is found.
     */
    FileMatcher(String name, Pattern... patterns) {
        this.name = name;
        this.patterns = patterns;
    }

    /**
     * Given a set of Strings containing urls, returns the subset of strings
     * that match at least one of the specified regular expressions.
     */
    public List<String> match(List<String> links) {
        List<String> result = new ArrayList<String>();
        for (Pattern p : patterns) {
            for (String link : links) {
                if (p.matcher(link).matches()) {
                    result.add(link);
                }
            }
            if (!result.isEmpty()) {
                break;
            }
        }
        return result;
    }

    /**
     * Return all files whose names match at least one of the specified regexes.
     */
    public List<File> matchFiles(List<File> paths) {
        List<File> result = new ArrayList<File>();
        for (Pattern p : patterns) {
            for (File file : paths) {
                if (p.matcher(file.getAbsolutePath()).matches()) {
                    result.add(file);
                }
            }
            if (!result.isEmpty()) {
                break;
            }
        }
        return result;
    }

    /**
     * @param link A link as returned by this.match() (e.g. enwiki-latest-abstract10.xml-rss.xml)
     * @return The index of the file (e.g. 10). If there is no index, returns 1.
     */
    public int getNumber(String link) {
        for (Pattern p : patterns) {
            Matcher m = p.matcher(link);
            if (m.matches()) {
                if (m.groupCount() >= 2) {
                    return Integer.valueOf(m.group(m.groupCount()));    // get last group
                } else {
                    return 1;   // Wikipedia file indexes start at 1
                }
            }
        }
        throw new IllegalStateException();
    }

    public Language getLanguage(String link) {
        int end = link.lastIndexOf("wiki");
        if (end < 1) {
            throw new IllegalStateException("No language detected for " + link);
        }
        int beg;
        for (beg = end-1; beg >=0 && isLangChar(link.charAt(beg)); beg--) {
            // All work is done in loop condition.
        }
        return Language.getByLangCode(link.substring(beg + 1, end));
    }

    private boolean isLangChar(char c) {
        return Character.isLetter(c) || Character.isDigit(c) || c == '_' || c == '-';
    }

    public String getName() {
        return name;
    }

    public static FileMatcher getByName(String name) {
        for (FileMatcher linkMatcher : FileMatcher.values()) {
            if (linkMatcher.getName().equalsIgnoreCase(name)) {
                return linkMatcher;
            }
        }
        return null;
    }

    public static List<FileMatcher> getListByNames(List<String> listNames) {
        List<FileMatcher> listMatchers = new ArrayList<FileMatcher>();
        for (String name : listNames) {
            listMatchers.add(getByName(name));
        }
        return listMatchers;
    }

    static public List<String> getAllNames() {
        List<String> result = new ArrayList<String>();
        for (FileMatcher matcher : FileMatcher.values()) {
            result.add(matcher.getName());
        }
        return result;
    }
}
