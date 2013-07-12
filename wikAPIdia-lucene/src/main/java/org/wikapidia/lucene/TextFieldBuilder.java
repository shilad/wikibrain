package org.wikapidia.lucene;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.RawPageDao;
import org.wikapidia.core.dao.RedirectDao;
import org.wikapidia.core.model.LocalPage;

/**
 * @author Ari Weiland
 */
public class TextFieldBuilder {

    private final LocalPageDao localPageDao;
    private final RawPageDao rawPageDao;
    private final RedirectDao redirectDao;
    private final LuceneOptions options;

    public TextFieldBuilder(LocalPageDao localPageDao, RawPageDao rawPageDao, RedirectDao redirectDao, LuceneOptions options) {
        this.localPageDao = localPageDao;
        this.rawPageDao = rawPageDao;
        this.redirectDao = redirectDao;
        this.options = options;
        try {
            localPageDao.setFollowRedirects(false);
        } catch (DaoException e) {
            throw new RuntimeException(e);
        }
    }

    public String getESAText(LocalPage page) throws DaoException {
        String title = page.getTitle().getCanonicalTitle();
        TIntSet redirects = redirectDao.getRedirects(page);
        String plainText = rawPageDao.getById(page.getLanguage(), page.getLocalId()).getPlainText();
        StringBuilder sb = new StringBuilder();
        sb.append(title);
        TIntIterator iterator = redirects.iterator();
        while (iterator.hasNext()) {
            sb.append(localPageDao
                    .getById(page.getLanguage(), iterator.next())
                    .getTitle()
                    .getCanonicalTitle());
        }
        sb.append(plainText);
        return sb.toString();
    }
}
