package org.wikapidia.download;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.wikapidia.core.lang.Language;

import java.util.*;

/**
 *
 * @author Ari Weiland
 *
 * This class stores a list of DumpLinkInfo, and provides iteration by language
 * so that during the download process, downloads proceed one language at a time.
 * The Iterator returns Multimaps of LinkMatchers mapped to DumpLinkInfo so that
 * the downloads can be additionally clustered by LinkMatcher.
 *
 */
public class DumpLinkCluster implements Iterable<Multimap<LinkMatcher, DumpLinkInfo>> {

    private final Set<Language> languages = new HashSet<Language>();
    private final List<DumpLinkInfo> links = new ArrayList<DumpLinkInfo>();

    public DumpLinkCluster() {}

    public void add(DumpLinkInfo link) {
        languages.add(link.getLanguage());
        links.add(link);
    }

    public Multimap<LinkMatcher, DumpLinkInfo> get(Language language) {
        Multimap<LinkMatcher, DumpLinkInfo> map = HashMultimap.create();
        for (DumpLinkInfo link : links) {
            if (link.getLanguage().equals(language)) {
                map.put(link.getLinkMatcher(), link);
            }
        }
        return map;
    }

    @Override
    public Iterator<Multimap<LinkMatcher, DumpLinkInfo>> iterator() {
        return new Iterator<Multimap<LinkMatcher, DumpLinkInfo>>() {

            Iterator<Language> local = languages.iterator();

            @Override
            public boolean hasNext() {
                return local.hasNext();
            }

            @Override
            public Multimap<LinkMatcher, DumpLinkInfo> next() {
                return get(local.next());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
