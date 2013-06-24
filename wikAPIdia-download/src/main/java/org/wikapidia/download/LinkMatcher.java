package org.wikapidia.download;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 * @author Yulun Li
 *
 * Given a list of urls, finds a certain type of links
 */
public enum LinkMatcher {

    MULTISTREAM       ("multistream", Pattern.compile(".+wiki-latest-pages-articles-multistream.xml.bz2")),
    MULTISTREAM_INDEX ("multistream_index", Pattern.compile(".+wiki-latest-pages-articles-multistream-index\\.txt\\.bz2")),
    EDIT_HISTORY_7z      ("edit_history_7z",
            Pattern.compile(".+wiki-latest-pages-meta-history\\d+.xml-.+\\.7z"),
            Pattern.compile(".+wiki-latest-pages-meta-history.xml.7z")),
    EDIT_HISTORY_bz2      ("edit_history_bz2",
            Pattern.compile(".+wiki-latest-pages-meta-history\\d+.xml-.+\\.bz2"),
            Pattern.compile(".+wiki-latest-pages-meta-history.xml.bz2")),
    LOG               ("log_events", Pattern.compile(".+wiki-latest-pages-logging.xml.gz")),
    META_CURRENT      ("current_meta",
            Pattern.compile(".+wiki-latest-pages-meta-current\\d+.xml-.+\\.bz2"),
            Pattern.compile(".+wiki-latest-pages-meta-current.xml.bz2")),
    ARTICLES          ("articles",
            Pattern.compile(".+wiki-latest-pages-articles\\d+\\.xml-.+\\.bz2"),
            Pattern.compile(".+wiki-latest-pages-articles.xml.bz2")),
    STUB_ARTICLES     ("stub_articles", Pattern.compile(".+wiki-latest-stub-articles\\d*.xml.gz")),
    STUB_META_CURRENT ("stub_meta_current", Pattern.compile(".+wiki-latest-stub-meta-current\\d*.xml.gz")),
    STUB_META_HISTORY ("stub_meta_histories", Pattern.compile(".+wiki-latest-stub-meta-history\\d*.xml.gz")),
    ABSTRACT          ("abstracts",
            Pattern.compile(".+wiki-latest-abstract\\d+\\.xml"),
            Pattern.compile(".+wiki-latest-abstract.*(?<!(\\.xml\\-rss))\\.xml")),
    TITLES            ("titles", Pattern.compile(".+wiki-latest-all-titles-in-ns0.gz")),
    INTERLINK         ("interwiki_links", Pattern.compile(".+wiki-latest-iwlinks.sql.gz")),
    REDIRECT_LIST     ("redirect_lists", Pattern.compile(".+wiki-latest-redirect.sql.gz")),
    PROTECTED_TITLES  ("protected_titles", Pattern.compile(".+wiki-latest-protected_titles.sql.gz")),
    NAME_PAIRS        ("name-pairs", Pattern.compile(".+wiki-latest-page_props.sql.gz")),
    PAGE_RESTRICTIONS ("page_restrictions", Pattern.compile(".+wiki-latest-page_restrictions.sql.gz")),
    PAGE_BASE         ("base_page_datas", Pattern.compile(".+wiki-latest-page.sql.gz")),
    CATEGORY          ("categories", Pattern.compile(".+wiki-latest-category.sql.gz")),
    USER_GROUP        ("user_groups", Pattern.compile(".+wiki-latest-user_groups.sql.gz")),
    INTERWIKI         ("interwiki_prefixes", Pattern.compile(".+wiki-latest-interwiki.sql.gz")),
    INTERLANG_LINKS   ("interlang_links", Pattern.compile(".+wiki-latest-langlinks.sql.gz")),
    EXTERNAL_LINKS    ("external_links", Pattern.compile(".+wiki-latest-externallinks.sql.gz")),
    TEMPLATE_LINKS    ("template_links", Pattern.compile(".+wiki-latest-templatelinks.sql.gz")),
    IMAGE_LINKS       ("image_links", Pattern.compile(".+wiki-latest-imagelinks.sql.gz")),
    CATEGORY_LINKS    ("category_links", Pattern.compile(".+wiki-latest-categorylinks.sql.gz")),
    LINK_SQL          ("links", Pattern.compile(".+wiki-latest-pagelinks.sql.gz")),
    OLD_MEDIA_META    ("old_media_metas", Pattern.compile(".+wiki-latest-oldimage.sql.gz")),
    CURRENT_MEDIA_META("current_media_metas", Pattern.compile(".+wiki-latest-image.sql.gz")),
    SITE_STATS        ("site_stats", Pattern.compile(".+wiki-latest-site_stats.sql.gz")),
    FLAGGED_REVISION  ("flagged_revisions", Pattern.compile(".+wiki-latest-flaggedrevs.sql.gz")),
    FLAGGED_PAGES     ("flagged_pages", Pattern.compile(".+wiki-latest-flaggedpages.sql.gz")),
    MD5               ("md5_checksums", Pattern.compile(".+wiki-latest-md5sums.txt"));


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

    static public List<String> getAllNames() {
        List<String> result = new ArrayList<String>();
        for (LinkMatcher matcher : LinkMatcher.values()) {
            result.add(matcher.getName());
        }
        return result;
    }

}