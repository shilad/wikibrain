package org.wikibrain.core.dao;

import gnu.trove.map.TIntIntMap;
import gnu.trove.set.TIntSet;
import org.junit.Test;
import org.wikibrain.core.dao.sql.RedirectSqlDao;
import org.wikibrain.core.dao.sql.TestDaoUtil;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.Title;

import java.io.IOException;
import java.sql.SQLException;

/**
 */
public class TestRedirectDao {
    @Test
    public void  test() throws ClassNotFoundException, IOException, SQLException, DaoException{
        WpDataSource wpDs = TestDaoUtil.getWpDataSource();
        LanguageInfo langInfo = LanguageInfo.getByLangCode("en");
        Language lang = langInfo.getLanguage();
        RedirectSqlDao redirectDao = new RedirectSqlDao(wpDs);
        redirectDao.beginLoad();
        redirectDao.save(lang, 0, 5);
        redirectDao.save(lang, 1, 5);
        redirectDao.save(lang, 2, 6);
        redirectDao.save(LanguageInfo.getByLangCode("la").getLanguage(), 3, 5);
        redirectDao.endLoad();

        assert (redirectDao.isRedirect(lang, 0));
        assert (redirectDao.isRedirect(lang, 1));
        assert (redirectDao.isRedirect(lang, 2));
        assert (!redirectDao.isRedirect(lang, 3));

        assert (redirectDao.resolveRedirect(lang, 0)==5);
        assert (redirectDao.resolveRedirect(lang, 1)==5);
        assert (redirectDao.resolveRedirect(lang, 2)==6);

        LocalPage lp = new LocalPage(lang, 5, new Title("The Joy of Testing: The GLaDoS Story", langInfo), NameSpace.ARTICLE);
        TIntSet redirects = redirectDao.getRedirects(lp);
        assert (redirects.contains(0));
        assert (redirects.contains(1));
        assert (!redirects.contains(2));

        TIntIntMap allRedirects = redirectDao.getAllRedirectIdsToDestIds(lang);
        assert (allRedirects.get(0)==5);
        assert (allRedirects.get(1)==5);
        assert (allRedirects.get(2)==6);
        assert (allRedirects.get(3)==-1);
    }
}
