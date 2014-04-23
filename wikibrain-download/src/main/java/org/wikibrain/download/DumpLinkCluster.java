package org.wikibrain.download;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.wikibrain.core.cmd.FileMatcher;
import org.wikibrain.core.lang.Language;

import java.util.*;

/**
 *
 * This class wraps a complex map of DumpLinkInfo, and provides iteration by language
 * so that during the download process, downloads proceed one language at a time.
 * The Iterator returns Multimaps of LinkMatchers mapped to DumpLinkInfo so that
 * the downloads can be additionally clustered by FileMatcher.
 *
 * @author Ari Weiland
 *
 */
public class DumpLinkCluster implements Iterable<Language> {

    private final Map<Language, Multimap<FileMatcher, DumpLinkInfo>> links = new HashMap<Language, Multimap<FileMatcher, DumpLinkInfo>>();

    public DumpLinkCluster() {}

    public void add(DumpLinkInfo link) {
        Language language = link.getLanguage();
        if (!links.containsKey(language)) {
            Multimap<FileMatcher, DumpLinkInfo> temp = HashMultimap.create();
            links.put(language, temp);
        }
        links.get(language).put(link.getLinkMatcher(), link);
    }

    public Multimap<FileMatcher, DumpLinkInfo> get(Language language) {
        return links.get(language);
    }

    public int size() {
        int counter = 0;
        for (Language language : this) {
            counter += this.get(language).size();
        }
        return counter;
    }

    @Override
    public Iterator<Language> iterator() {
        return links.keySet().iterator();
    }
}
