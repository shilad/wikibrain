package org.wikapidia.core.dao;


import com.jolbox.bonecp.BoneCPDataSource;
import org.junit.Test;
import org.wikapidia.core.dao.sql.LocalArticleSqlDao;
import org.wikapidia.core.dao.sql.RawPageSqlDao;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.*;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class TestRawPageDao {
    @Test
    public void test() throws ClassNotFoundException, IOException, SQLException, DaoException {
        Class.forName("org.h2.Driver");
        File tmpDir = File.createTempFile("wikapidia-h2", null);
        tmpDir.delete();
        tmpDir.deleteOnExit();
        tmpDir.mkdirs();

        BoneCPDataSource ds = new BoneCPDataSource();
        ds.setJdbcUrl("jdbc:h2:"+new File(tmpDir,"db").getAbsolutePath());
        ds.setUsername("sa");
        ds.setPassword("");

        LanguageInfo lang = LanguageInfo.getByLangCode("en");
        LocalArticleSqlDao lpDao = new LocalArticleSqlDao(ds);
        RawPageSqlDao rpDao = new RawPageSqlDao(ds, lpDao);
        lpDao.beginLoad();
        rpDao.beginLoad();

        LocalPage page = new LocalPage(
                lang.getLanguage(),
                7,
                new Title("test", lang),
                PageType.ARTICLE
        );
        lpDao.save(page);
        String body = "foo bar \000baz\n\n\324";
        RawPage rawPage = new RawPage(
                7, 3242, "test", body, null, PageType.ARTICLE, lang.getLanguage()
        );
        rpDao.save(rawPage);

        lpDao.endLoad();
        rpDao.endLoad();

        LocalArticle savedPage = lpDao.getById(lang.getLanguage(), 7);
        assert (savedPage != null);
        assert (page.getLocalId() == savedPage.getLocalId());
        assert (page.getTitle().equals(savedPage.getTitle()));
        assert (page.getPageType().equals(savedPage.getPageType()));

        RawPage rawSaved = rpDao.get(lang.getLanguage(), 7);
        assert (rawSaved != null);
        assert (page.getLocalId() == rawSaved.getPageId());
        assert (page.getTitle().equals(rawSaved.getTitle()));
        assert (page.getPageType().equals(rawSaved.getType()));
        assert (body.equals(rawSaved.getBody()));
    }
}
