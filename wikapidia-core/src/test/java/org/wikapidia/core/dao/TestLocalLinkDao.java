package org.wikapidia.core.dao;

import com.jolbox.bonecp.BoneCPDataSource;
import org.junit.Test;
import org.wikapidia.core.dao.sql.LocalLinkSqlDao;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalLink;

import java.io.File;
import java.io.IOException;

public class TestLocalLinkDao {
    @Test
    public void testLink() throws ClassNotFoundException, IOException, DaoException {
        Class.forName("org.h2.Driver");
        File tmpDir = File.createTempFile("wikapidia-h2", null);
        tmpDir.delete();
        tmpDir.deleteOnExit();
        tmpDir.mkdir();

        BoneCPDataSource ds = new BoneCPDataSource();
        ds.setJdbcUrl("jdbc:h2:"+new File(tmpDir,"db").getAbsolutePath());
        ds.setUsername("sa");
        ds.setPassword("");

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
