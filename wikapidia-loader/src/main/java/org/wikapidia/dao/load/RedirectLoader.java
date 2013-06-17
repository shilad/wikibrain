package org.wikapidia.dao.load;

import gnu.trove.map.TLongIntMap;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.WikapidiaIterable;
import org.wikapidia.core.dao.sql.LocalPageSqlDao;
import org.wikapidia.core.dao.sql.RawPageSqlDao;
import org.wikapidia.core.dao.sql.RedirectSqlDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.RawPage;
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

    public void loadAllRedirects() throws DaoException{
        WikapidiaIterable<RawPage> redirectPages = rawPages.getAllRedirects(language);
        for(RawPage p : redirectPages)
        {
            redirects.save(language,
                    p.getPageId(),
                    localPages.getIdByTitle(redirectParser.getRedirect(p.getBody()).getCanonicalTitle(),
                            language,
                            p.getNamespace()) //this can be wrong sometimes CROSS NAMESPACE REDIRECTS SUCK
            );
        }
    }

    public void resolveAllRedirects() throws DaoException{
        WikapidiaIterable<RawPage> redirectPages = rawPages.getAllRedirects(language);
        for(RawPage p : redirectPages)
        {
            int currPage = p.getPageId();
            boolean found = false;
            for (int i=0; i<4; i++){
                currPage = redirects.resolveRedirect(language, currPage);
                if (!redirects.isRedirect(language, currPage)){
                    found = true;
                    break;
                }
            }
            if (found){
               redirects.update(language,p.getPageId(),currPage);
            }
            else {
               redirects.update(language,p.getPageId(),currPage);
            }

        }
    }

}
