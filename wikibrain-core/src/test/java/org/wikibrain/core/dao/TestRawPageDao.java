package org.wikibrain.core.dao;


import org.junit.Test;
import org.wikibrain.core.dao.sql.LocalPageSqlDao;
import org.wikibrain.core.dao.sql.RawPageSqlDao;
import org.wikibrain.core.dao.sql.TestDaoUtil;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.model.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

public class TestRawPageDao {
    @Test
    public void test() throws ClassNotFoundException, IOException, SQLException, DaoException {
        WpDataSource wpDs = TestDaoUtil.getWpDataSource();
        LanguageInfo lang = LanguageInfo.getByLangCode("en");
        LocalPageSqlDao lpDao = new LocalPageSqlDao(wpDs);
        RawPageDao rpDao = new RawPageSqlDao(wpDs);
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

        LocalPage savedPage = lpDao.getById(lang.getLanguage(), 7);
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
