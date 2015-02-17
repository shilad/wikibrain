package org.wikibrain.sr.wikify;

import com.typesafe.config.Config;
import org.h2.util.StringUtils;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalLinkDao;
import org.wikibrain.core.dao.RawPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.model.RawPage;

import java.util.*;

/**
 * A wikifier that just returns existing hyperliks.
 *
 * This is a bit tricky, because the locations of the hyperlinks are based on "raw" wikitext,
 * but the text used by this class is "plaintext" stripped of wikimarkup.
 *
 * Most of the logic in this class realigns the wikimarkup locations
 * with plain text locations.
 *
 * @author Shilad Sen
 */
public class IdentityWikifier implements Wikifier {
    private final RawPageDao pageDao;
    private final LocalLinkDao linkDao;
    private final Language language;

    public IdentityWikifier(Language language, RawPageDao pageDao, LocalLinkDao linkDao) {
        this.language = language;
        this.pageDao = pageDao;
        this.linkDao = linkDao;
    }

    @Override
    public List<LocalLink> wikify(int wpId) throws DaoException {
        RawPage rp = pageDao.getById(language, wpId);
        if (rp == null) {
            return new ArrayList<LocalLink>();
        }
        return wikify(wpId, rp.getPlainText(false));
    }

    @Override
    public List<LocalLink> wikify(int wpId, String text) throws DaoException {
        if (text == null || text.isEmpty()) {
            return new ArrayList<LocalLink>();
        }

        List<LocalLink> links = new ArrayList<LocalLink>();
        for (LocalLink ll : linkDao.getLinks(language, wpId, true)) {
            if (ll.getLocation() >= 0 && ll.isParseable() && !StringUtils.isNullOrEmpty(ll.getAnchorText())) {
                links.add(ll);
            }
        }
        Collections.sort(links);

        return align(links, text);
    }

    private LocalLink cloneLinkWithLocation(LocalLink ll, int location) {
        return new LocalLink(
                ll.getLanguage(),
                ll.getAnchorText(),
                ll.getSourceId(),
                ll.getDestId(),
                ll.isOutlink(),
                location,
                ll.isParseable(),
                ll.getLocType()
        );
    }

    private List<LocalLink> align(List<LocalLink> anchors, String text) {
        List<LocalLink> alignment = new ArrayList<LocalLink>();
        if (anchors.isEmpty()) {
            return alignment;
        }

        BitSet used = new BitSet(text.length());

        anchors = new LinkedList<LocalLink>(anchors);   // for perfomance
        int i = 0;
        Iterator<LocalLink> iter = anchors.iterator();
        while (iter.hasNext()) {
            LocalLink ll = iter.next();
            String a = ll.getAnchorText();
            int j = text.indexOf(a, i);

            // if not found, skip to the next anchor but don't advance pointer
            if (j < 0) {
                continue;
            }

            alignment.add(cloneLinkWithLocation(ll, j));

            // Mark bits as set
            used.set(j, j + a.length());

            i = j + a.length();
            iter.remove();
        }

        // Stop if we're done (typical case)
        if (anchors.isEmpty()) {
            return alignment;
        }

        // Look for unused matches
        for (LocalLink ll : anchors) {
            String a = ll.getAnchorText();
            i = findNextUnused(text, a, i, used);
            if (i >= 0) {
                alignment.add(cloneLinkWithLocation(ll, i));
                used.set(i, i + a.length());
            }
        }

        Collections.sort(alignment);

        return alignment;
    }

    private int findNextUnused(String text, String query, int begin, BitSet used) {
        int i = begin;
        while (i < text.length()) {
            i = text.indexOf(query, i);
            if (i < 0) {
                return -1;
            }
            if (used.get(i, i + query.length()).isEmpty()) {
                return i;
            }
            i++;    // skip to next index
        }
        return -1;
    }

    @Override
    public List<LocalLink> wikify(String text) throws DaoException {
        return new ArrayList<LocalLink>();
    }

    public static class Provider extends org.wikibrain.conf.Provider<Wikifier> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class<Wikifier> getType() {
            return Wikifier.class;
        }

        @Override
        public String getPath() {
            return "sr.wikifier";
        }

        @Override
        public Wikifier get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (runtimeParams == null || !runtimeParams.containsKey("language")) {
                throw new IllegalArgumentException("Wikifier requires 'language' runtime parameter.");
            }
            if (!config.getString("type").equals("identity")) {
                return null;
            }

            Language language = Language.getByLangCode(runtimeParams.get("language"));
            String linkName = config.getString("localLinkDao");
            return new IdentityWikifier(language,
                    getConfigurator().get(RawPageDao.class),
                    getConfigurator().get(LocalLinkDao.class,
                            linkName));
        }
    }
}
