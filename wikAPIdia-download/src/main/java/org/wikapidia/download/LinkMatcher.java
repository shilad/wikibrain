package org.wikapidia.download;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 * @author Yulun Li
 *
 * Given a list of urls as strings, finds a certain type of links
 */
public enum LinkMatcher {

    MULTISTREAM       ("multistream", Pattern.compile(".+-pages-articles-multistream.xml.bz2")),
    MULTISTREAM_INDEX ("multistream_index", Pattern.compile(".+-pages-articles-multistream-index\\.txt\\.bz2")),
    EDIT_HISTORY_7z      ("edit_history_7z",
            Pattern.compile(".+-pages-meta-history\\d+.xml-.+\\.7z"),
            Pattern.compile(".+-pages-meta-history.xml.7z")),
    EDIT_HISTORY_bz2      ("edit_history_bz2",
            Pattern.compile(".+-pages-meta-history\\d+.xml-.+\\.bz2"),
            Pattern.compile(".+-pages-meta-history.xml.bz2")),
    LOG               ("log_events", Pattern.compile(".+-pages-logging.xml.gz")),
    META_CURRENT      ("current_meta",
            Pattern.compile(".+-pages-meta-current\\d+.xml-.+\\.bz2"),
            Pattern.compile(".+-pages-meta-current.xml.bz2")),
    ARTICLES          ("articles",
            Pattern.compile(".+-pages-articles\\d+\\.xml-.+\\.bz2"),
            Pattern.compile(".+-pages-articles.xml.bz2")),
    STUB_ARTICLES     ("stub_articles", Pattern.compile(".+-stub-articles\\d*.xml.gz")),
    STUB_META_CURRENT ("stub_meta_current", Pattern.compile(".+-stub-meta-current\\d*.xml.gz")),
    STUB_META_HISTORY ("stub_meta_histories", Pattern.compile(".+-stub-meta-history\\d*.xml.gz")),
    ABSTRACT          ("abstracts",
            Pattern.compile(".+-abstract\\d+\\.xml"),
            Pattern.compile(".+-abstract.*(?<!(\\.xml\\-rss))\\.xml")),
    TITLES            ("titles", Pattern.compile(".+-all-titles-in-ns0.gz")),
    INTERLINK         ("interwiki_links", Pattern.compile(".+-iwlinks.sql.gz")),
    REDIRECT_LIST     ("redirect_lists", Pattern.compile(".+-redirect.sql.gz")),
    PROTECTED_TITLES  ("protected_titles", Pattern.compile(".+-protected_titles.sql.gz")),
    NAME_PAIRS        ("name-pairs", Pattern.compile(".+-page_props.sql.gz")),
    PAGE_RESTRICTIONS ("page_restrictions", Pattern.compile(".+-page_restrictions.sql.gz")),
    PAGE_BASE         ("base_page_datas", Pattern.compile(".+-page.sql.gz")),
    CATEGORY          ("categories", Pattern.compile(".+-category.sql.gz")),
    USER_GROUP        ("user_groups", Pattern.compile(".+-user_groups.sql.gz")),
    INTERWIKI         ("interwiki_prefixes", Pattern.compile(".+-interwiki.sql.gz")),
    INTERLANG_LINKS   ("interlang_links", Pattern.compile(".+-langlinks.sql.gz")),
    EXTERNAL_LINKS    ("external_links", Pattern.compile(".+-externallinks.sql.gz")),
    TEMPLATE_LINKS    ("template_links", Pattern.compile(".+-templatelinks.sql.gz")),
    IMAGE_LINKS       ("image_links", Pattern.compile(".+-imagelinks.sql.gz")),
    CATEGORY_LINKS    ("category_links", Pattern.compile(".+-categorylinks.sql.gz")),
    LINK_SQL          ("links", Pattern.compile(".+-pagelinks.sql.gz")),
    OLD_MEDIA_META    ("old_media_metas", Pattern.compile(".+-oldimage.sql.gz")),
    CURRENT_MEDIA_META("current_media_metas", Pattern.compile(".+-image.sql.gz")),
    SITE_STATS        ("site_stats", Pattern.compile(".+-site_stats.sql.gz")),
    FLAGGED_REVISION  ("flagged_revisions", Pattern.compile(".+-flaggedrevs.sql.gz")),
    FLAGGED_PAGES     ("flagged_pages", Pattern.compile(".+-flaggedpages.sql.gz")),
    MD5               ("md5_checksums", Pattern.compile(".+-md5sums.txt"));


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

    public static LinkMatcher getByName(String name) {
        for (LinkMatcher linkMatcher : LinkMatcher.values()) {
            if (linkMatcher.getName().equalsIgnoreCase(name)) {
                return linkMatcher;
            }
        }
        return null;
    }

    public static List<LinkMatcher> getListByNames(List<String> listNames) {
        List<LinkMatcher> listMatchers = new ArrayList<LinkMatcher>();
        for (String name : listNames) {
            listMatchers.add(getByName(name));
        }
        return listMatchers;
    }

    static public List<String> getAllNames() {
        List<String> result = new ArrayList<String>();
        for (LinkMatcher matcher : LinkMatcher.values()) {
            result.add(matcher.getName());
        }
        return result;
    }

}