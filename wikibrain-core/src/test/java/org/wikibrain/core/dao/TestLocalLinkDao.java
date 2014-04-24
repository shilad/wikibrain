package org.wikibrain.core.dao;

import org.junit.Test;
import org.wikibrain.core.dao.sql.LocalLinkSqlDao;
import org.wikibrain.core.dao.sql.TestDaoUtil;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.model.LocalLink;

import java.io.IOException;

public class TestLocalLinkDao {
    @Test
    public void testLink() throws ClassNotFoundException, IOException, DaoException {
        WpDataSource ds = TestDaoUtil.getWpDataSource();

        LanguageInfo lang = LanguageInfo.getByLangCode("en");
        LocalLinkSqlDao dao = new LocalLinkSqlDao(ds);
        dao.beginLoad();
        LocalLink link = new LocalLink(
                lang.getLanguage(),
                "I am an anchor text",
                1,
                2,
                true,
                0,
                true,
                LocalLink.LocationType.FIRST_PARA
        );
        dao.save(link);
        dao.endLoad();

        Iterable<LocalLink> savedLinks = dao.getLinks(lang.getLanguage(), 1, true);
        assert (savedLinks!=null);
        LocalLink savedLink = savedLinks.iterator().next();
        assert (savedLink.isOutlink());
        assert (savedLink.getLanguage() == link.getLanguage());
        assert (savedLink.getAnchorText().equals(link.getAnchorText()));
        assert (savedLink.getSourceId() == link.getSourceId());
        assert (savedLink.getDestId() == link.getDestId());
        assert (savedLink.getLocation() == link.getLocation());
        assert (savedLink.isParseable() == link.isParseable());
        assert (savedLink.getLocType() == link.getLocType());

        savedLinks = dao.getLinks(lang.getLanguage(), 2, false);
        assert (savedLinks!=null);
        savedLink = savedLinks.iterator().next();
        assert (!savedLink.isOutlink());
        assert (savedLink.getLanguage() == link.getLanguage());
        assert (savedLink.getAnchorText().equals(link.getAnchorText()));
        assert (savedLink.getSourceId() == link.getSourceId());
        assert (savedLink.getDestId() == link.getDestId());
        assert (savedLink.getLocation() == link.getLocation());
        assert (savedLink.isParseable() == link.isParseable());
        assert (savedLink.getLocType() == link.getLocType());
    }
}
