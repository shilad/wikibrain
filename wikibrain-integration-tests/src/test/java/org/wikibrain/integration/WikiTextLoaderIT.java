package org.wikibrain.integration;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.model.LocalCategoryMember;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.model.MetaInfo;
import org.wikibrain.loader.WikiTextLoader;

import java.io.IOException;
import java.sql.SQLException;

import static org.junit.Assert.*;

/**
 * @author Shilad Sen
 */
public class WikiTextLoaderIT {

    @BeforeClass
    public static void prepareDump() throws ConfigurationException, IOException, SQLException {
        TestDB db = TestUtils.getTestDb();
        db.restoreRedirect();
    }

    @Ignore
    @Test
    public void testLoader() throws ClassNotFoundException, SQLException, DaoException, ConfigurationException, IOException {
        WikiTextLoader.main(TestUtils.getArgs("-d"));
        Env env = TestUtils.getEnv();
        LocalLinkDao lldao = env.getConfigurator().get(LocalLinkDao.class);
        MetaInfoDao metaDao = env.getConfigurator().get(MetaInfoDao.class);
        assertEquals(lldao.getLoadedLanguages().size(), 2);
//        assertTrue(lldao.getCount(new DaoFilter()) > 3000000);
        MetaInfo info = metaDao.getInfo(LocalLink.class);
        assertTrue(info.getNumRecords() > 3000000);
        assertEquals(info.getNumErrors(), 0);

        LocalCategoryMemberDao lcdao = env.getConfigurator().get(LocalCategoryMemberDao.class);
        assertEquals(lcdao.getLoadedLanguages().size(), 2);
        assertTrue(lcdao.getCount(new DaoFilter()) > 100000);
        info = metaDao.getInfo(LocalCategoryMember.class);
        assertEquals(info.getNumRecords(), lcdao.getCount(new DaoFilter()));
        assertEquals(info.getNumErrors(), 0);
    }
}
