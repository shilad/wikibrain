package org.wikapidia.core.dao;


import com.jolbox.bonecp.BoneCPDataSource;
import org.junit.Test;
import org.wikapidia.core.dao.sql.LocalArticleSqlDao;
import org.wikapidia.core.dao.sql.LocalCategorySqlDao;
import org.wikapidia.core.dao.sql.WpDataSource;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.*;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestLocalPageDao {
    @Test
    public void testArticle() throws ClassNotFoundException, IOException, SQLException, DaoException {
        WpDataSource wpDs = TestDaoUtil.getWpDataSource();
        DataSource ds = wpDs.getDataSource();

        LanguageInfo lang = LanguageInfo.getByLangCode("en");
        LocalArticleSqlDao dao = new LocalArticleSqlDao(wpDs);
        dao.beginLoad();
        LocalPage page = new LocalPage(
                lang.getLanguage(),
                7,
                new Title("test", lang),
                NameSpace.ARTICLE
        );
        dao.save(page);
        dao.endLoad();

        LocalArticle savedPage = dao.getByTitle(lang.getLanguage(), new Title("test", lang));
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
        Map<Integer, LocalArticle> pages = dao.getByIds(lang.getLanguage(), pageIds);
        assert (pages.size() == 1);
        assert (pages.get(7).equals(page));
        assert (pages.get(7).equals(savedPage));

        List<Title> titles = new ArrayList<Title>();
        titles.add(new Title("test", lang));
        Map<Title, LocalArticle> morePages = dao.getByTitles(lang.getLanguage(), titles);
        assert (morePages.size() == 1);
        assert (morePages.get(new Title("test", lang)).equals(page));
        assert (morePages.get(new Title("test", lang)).equals(savedPage));

        int savedId = dao.getIdByTitle("Test", lang.getLanguage(), NameSpace.ARTICLE);
        assert (savedId==7);
    }

    @Test
    public void TestCategory () throws ClassNotFoundException, IOException, SQLException, DaoException {
        WpDataSource wpDs = TestDaoUtil.getWpDataSource();

        LanguageInfo lang = LanguageInfo.getByLangCode("en");
        LocalCategorySqlDao dao = new LocalCategorySqlDao(wpDs);
        dao.beginLoad();
        LocalPage page = new LocalPage(
                lang.getLanguage(),
                7,
                new Title("test", lang),
                NameSpace.CATEGORY
        );
        dao.save(page);
        dao.endLoad();

        LocalCategory savedPage = dao.getByTitle(lang.getLanguage(), new Title("test", lang));
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
        Map<Integer, LocalCategory> pages = dao.getByIds(lang.getLanguage(), pageIds);
        assert (pages.size() == 1);
        assert (pages.get(7).equals(page));
        assert (pages.get(7).equals(savedPage));

        List<Title> titles = new ArrayList<Title>();
        titles.add(new Title("test", lang));
        Map<Title, LocalCategory> morePages = dao.getByTitles(lang.getLanguage(), titles);
        assert (morePages.size() == 1);
        assert (morePages.get(new Title("test", lang)).equals(page));
        assert (morePages.get(new Title("test", lang)).equals(savedPage));

        int savedId = dao.getIdByTitle("Test", lang.getLanguage(), NameSpace.CATEGORY);
        assert (savedId==7);
    }
}
