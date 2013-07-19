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
import java.util.Date;

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
        RawPageSqlDao rpDao = new RawPageSqlDao(ds);
        lpDao.beginLoad();
        rpDao.beginLoad();

        LocalPage page = new LocalPage(
                lang.getLanguage(),
                7,
                new Title("test", lang),
                NameSpace.ARTICLE
        );
        lpDao.save(page);
        String body = "foo bar \000baz\n\n\324";
        RawPage rawPage = new RawPage(
                7, 3242, "test", body, new Date(), lang.getLanguage(), NameSpace.ARTICLE, true, false, "POOP"
        );
        rpDao.save(rawPage);

        lpDao.endLoad();
        rpDao.endLoad();

        LocalArticle savedPage = lpDao.getById(lang.getLanguage(), 7);
        assert (savedPage != null);
        assert (page.getLocalId() == savedPage.getLocalId());
        assert (page.getTitle().equals(savedPage.getTitle()));
        assert (page.getNameSpace().equals(savedPage.getNameSpace()));

        RawPage rawSaved = rpDao.getById(lang.getLanguage(), 7);
        assert (rawSaved != null);
        assert (page.getLocalId() == rawSaved.getLocalId());
        assert (page.getTitle().equals(rawSaved.getTitle()));
        assert (page.getNameSpace().equals(rawSaved.getNamespace()));
        assert (body.equals(rawSaved.getBody()));
        assert (rawSaved.isRedirect());

        Iterable<RawPage> savedRaws = rpDao.get(new DaoFilter().setLanguages(lang.getLanguage()));
        assert (savedRaws!=null);
        RawPage savedRaw = savedRaws.iterator().next();
        assert (savedRaw.getNamespace().getValue() == rawPage.getNamespace().getValue());
        assert (savedRaw.getLastEdit().equals(rawPage.getLastEdit()));
        assert (savedRaw.getRevisionId() == rawPage.getRevisionId());
        assert (savedRaw.getBody().equals(body));
        assert (savedRaw.getLanguage().equals(rawPage.getLanguage()));
        assert (savedRaw.getTitle().equals(rawPage.getTitle()));
        assert (savedRaw.isRedirect());
    }
}
