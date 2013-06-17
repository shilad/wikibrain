package org.wikapidia.dao.load;

import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.WikapidiaIterable;
import org.wikapidia.core.dao.sql.LocalPageSqlDao;
import org.wikapidia.core.dao.sql.RawPageSqlDao;
import org.wikapidia.core.dao.sql.RedirectSqlDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.core.model.Title;
import org.wikapidia.parser.wiki.RedirectParser;

import javax.sql.DataSource;

/**
 *
 * Idea for changing the flow of parsing:
 * - First load all redirect page id -> page id into memory (TIntIntHashMap).
 * - Fix chaining redirects
 * - Then save.
 * - RedirectSqlDao.update goes away.
 */
public class RedirectLoader {

    private final Language language;
    private final TLongIntMap titlesToIds;
    private TIntIntHashMap redirectIdsToPageIds;
    private final DataSource ds;
    private final RedirectParser redirectParser;
    private final RawPageSqlDao rawPages;
    private final LocalPageSqlDao localPages;
    private final RedirectSqlDao redirects;

    public RedirectLoader(Language language, TLongIntMap titlesToIds, DataSource ds) throws DaoException{
        this.language = language;
        this.titlesToIds = titlesToIds;
        this.ds = ds;
        this.redirectParser = new RedirectParser(language);
        this.rawPages = new RawPageSqlDao(ds);
        this.localPages = new LocalPageSqlDao(ds,false);
        this.redirects = new RedirectSqlDao(ds);
    }

    private void loadRedirectIdsIntoMemory(Language language) throws DaoException{
        redirectIdsToPageIds = new TIntIntHashMap(10, 0.5f, -1, -1);
        WikapidiaIterable<RawPage> redirectPages = rawPages.getAllRedirects(language);
        for(RawPage p : redirectPages){
           Title pTitle = new Title(redirectParser.getRedirect(p.getBody()).getCanonicalTitle(), LanguageInfo.getByLanguage(language));
           redirectIdsToPageIds.put(p.getPageId(),
                    localPages.getIdByTitle(pTitle.getCanonicalTitle(), language, pTitle.getNamespace()));
        }
    }

    private int resolveRedirect(int src){
        int dest = redirectIdsToPageIds.get(src);
        for(int i = 0; i<4; i++){
            if (redirectIdsToPageIds.get(dest) == -1)
                return dest;
            dest = redirectIdsToPageIds.get(dest);
        }
        return -1;
    }

    private void resolveRedirectsInMemory(Language language) throws  DaoException{
        for (int src : redirectIdsToPageIds.keys()) {
            redirectIdsToPageIds.put(src, resolveRedirect(src));
        }
    }



}
