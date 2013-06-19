package org.wikapidia.dao.load;

import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;
import org.wikapidia.parser.sql.TranscludedLinkParser;

/**
 */
public class TranscludedLinkLoader {
    private final LocalLinkDao dao;
    private final Language language;
    private final LocalPageDao pageDao;
    private TLongSet existing = new TLongHashSet();

    public TranscludedLinkLoader(LocalLinkDao dao, LocalPageDao pageDao, Language language) {
        this.dao = dao;
        this.pageDao = pageDao;
        this.language = language;

        // TODO: make sure LocalPageDao follows redirects
        // TODO: drop indexes
    }

    public void loadExisting() throws DaoException {
        existing.clear();
        for (LocalLink ll : dao.getLinks(language)) {
            existing.add(ll.longHashCode());
        }
    }

    public void addNewLinks(Iterable<TranscludedLinkParser.RawLink> links) throws DaoException {
        for (TranscludedLinkParser.RawLink link : links) {
            Title title = new Title(link.destTitle, LanguageInfo.getByLanguage(language));
            NameSpace ns = NameSpace.getNameSpaceById(link.destNamespace);
            LocalPage lp = pageDao.getByTitle(language, title, ns);
            if (lp == null) {
                // Handle red link
            } else {
                LocalLink ll = new LocalLink(language, null,
                        link.srcPageId, lp.getLocalId(),
                        true, -1, false, LocalLink.LocationType.NONE);
                if (!existing.contains(ll.longHashCode())) {
                    dao.save(ll);
                }
            }
        }
    }
}
