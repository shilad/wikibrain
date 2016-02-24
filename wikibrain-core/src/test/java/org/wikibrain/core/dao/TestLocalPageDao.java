package org.wikibrain.core.dao;


import org.junit.Test;
import org.wikibrain.core.dao.sql.LocalPageSqlDao;
import org.wikibrain.core.dao.sql.TestDaoUtil;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.model.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class TestLocalPageDao {
    @Test
    public void testArticle() throws ClassNotFoundException, IOException, SQLException, DaoException {
        WpDataSource wpDs = TestDaoUtil.getWpDataSource();
        LanguageInfo lang = LanguageInfo.getByLangCode("en");
        LocalPageSqlDao dao = new LocalPageSqlDao(wpDs);
        dao.beginLoad();
        LocalPage page = new LocalPage(
                lang.getLanguage(),
                7,
                new Title("test", lang),
                NameSpace.ARTICLE
        );
        dao.save(page);
        dao.endLoad();

        LocalPage savedPage = dao.getByTitle(new Title("test", lang), NameSpace.ARTICLE);
        assert (savedPage != null);
        assert (page.getLocalId() == savedPage.getLocalId());
        assert (page.getTitle().equals(savedPage.getTitle()));
        assert (page.getNameSpace().equals(savedPage.getNameSpace()));

        savedPage = dao.getById(lang.getLanguage(), 7);
        assert (savedPage != null);
        assert (page.getLocalId() == savedPage.getLocalId());
        assert (page.getTitle().equals(savedPage.getTitle()));
        assert (page.getNameSpace().equals(savedPage.getNameSpace()));

        List<Integer> pageIds = new ArrayList<Integer>();
        pageIds.add(7);
        Map<Integer, LocalPage> pages = dao.getByIds(lang.getLanguage(), pageIds);
        assert (pages.size() == 1);
        assert (pages.get(7).equals(page));
        assert (pages.get(7).equals(savedPage));

        List<Title> titles = new ArrayList<Title>();
        titles.add(new Title("test", lang));
        Map<Title, LocalPage> morePages = dao.getByTitles(lang.getLanguage(), titles, NameSpace.ARTICLE);
        assert (morePages.size() == 1);
        assert (morePages.get(new Title("test", lang)).equals(page));
        assert (morePages.get(new Title("test", lang)).equals(savedPage));

        int savedId = dao.getIdByTitle("Test", lang.getLanguage(), NameSpace.ARTICLE);
        assert (savedId==7);
    }

    @Test
    public void testURL() {
        LocalPage lp = new LocalPage(Language.EN, 3, "Barack Obama");
        String url = lp.getCompactUrl();

        assertEquals("/w/en/3/Barack_Obama", url);
        assertFalse(LocalPage.isCompactUrl("s" + url));
        assertTrue(LocalPage.isCompactUrl(url));

        LocalPage lp2 = LocalPage.fromCompactUrl(url);
        assertNotNull(lp2);
        assertEquals(lp, lp2);
    }
}
