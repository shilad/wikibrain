package org.wikapidia.core.dao;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.jolbox.bonecp.BoneCPDataSource;
import org.junit.Test;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.sql.*;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.*;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 */
public class TestUniversalPageDao {

    @Test
    public void testPageGet() throws ConfigurationException, DaoException {
        Configurator configurator = new Configurator(new Configuration());
        UniversalPageDao dao = configurator.get(UniversalPageDao.class);

        Iterable<UniversalPage> iterable = dao.get(new DaoFilter());
        int i = 0;
        for (UniversalPage page : iterable) {
            System.out.println(page.getUnivId());
            i++;
        }
        System.out.println("Page count: " + i);
    }

    @Test
    public void testLinkGet() throws ConfigurationException, DaoException {
        Configurator configurator = new Configurator(new Configuration());
        UniversalLinkDao dao = configurator.get(UniversalLinkDao.class);

        Iterable<UniversalLink> iterable = dao.get(new DaoFilter().setSourceIds(0));
        int i = 0;
        for (UniversalLink link : iterable) {
            System.out.println(link.getSourceUnivId() + " : " + link.getDestUnivId());
            i++;
        }
        System.out.println("Page count: " + i);
    }

//    @Test
//    public void testArticle() throws ClassNotFoundException, IOException, SQLException, DaoException {
//        Class.forName("org.h2.Driver");
//        File tmpDir = File.createTempFile("wikapidia-h2", null);
//        tmpDir.delete();
//        tmpDir.deleteOnExit();
//        tmpDir.mkdirs();
//
//        BoneCPDataSource ds = new BoneCPDataSource();
//        ds.setJdbcUrl("jdbc:h2:"+new File(tmpDir,"db").getAbsolutePath());
//        ds.setUsername("sa");
//        ds.setPassword("");
//
//        Multimap<Language, LocalArticle> map = HashMultimap.create();
//        LocalArticleSqlDao localDao = new LocalArticleSqlDao(ds);
//        localDao.beginLoad();
//        for (Language language : Language.LANGUAGES) {
//            LocalArticle temp = new LocalArticle(
//                    language,
//                    language.hashCode(),
//                    new Title(language.getEnLangName(), LanguageInfo.getByLanguage(language))
//            );
//            map.put(language, temp);
//            localDao.save(temp);
//        }
//        localDao.endLoad();
//        UniversalPage page = new UniversalArticle(23, 0, map);
//
//        UniversalArticleSqlDao dao = new UniversalArticleSqlDao(ds, new LocalArticleSqlDao(ds));
//        dao.beginLoad();
//        dao.save(page);
//        dao.endLoad();
//
//        UniversalPage savedPage = dao.getById(23, 0);
//        assert (savedPage != null);
//        assert (page.equals(savedPage));
//        assert (page.getNumberOfEntities() == savedPage.getNumberOfEntities());
//        assert (page.getLanguageSetOfExistsInLangs().equals(savedPage.getLanguageSetOfExistsInLangs()));
//        assert (page.getClarity() == savedPage.getClarity());
//
//        List<Integer> pageIds = new ArrayList<Integer>();
//        pageIds.add(23);
//        Map<Integer, UniversalArticle> pages = dao.getByIds(pageIds, 0);
//        assert (pages.size() == 1);
//        assert (pages.get(23).equals(page));
//        assert (pages.get(23).equals(savedPage));
//
//    }
//
//    @Test
//    public void TestCategory () throws ClassNotFoundException, IOException, SQLException, DaoException {
//        Class.forName("org.h2.Driver");
//        File tmpDir = File.createTempFile("wikapidia-h2", null);
//        tmpDir.delete();
//        tmpDir.deleteOnExit();
//        tmpDir.mkdirs();
//
//        BoneCPDataSource ds = new BoneCPDataSource();
//        ds.setJdbcUrl("jdbc:h2:"+new File(tmpDir,"db").getAbsolutePath());
//        ds.setUsername("sa");
//        ds.setPassword("");
//
//        Multimap<Language, LocalCategory> map = HashMultimap.create();
//        LocalCategorySqlDao localDao = new LocalCategorySqlDao(ds);
//        localDao.beginLoad();
//        for (Language language : Language.LANGUAGES) {
//            LocalCategory temp = new LocalCategory(
//                    language,
//                    language.hashCode(),
//                    new Title(language.getEnLangName(), LanguageInfo.getByLanguage(language))
//            );
//            map.put(language, temp);
//            localDao.save(temp);
//        }
//        localDao.endLoad();
//        UniversalPage page = new UniversalCategory(23, 0, map);
//
//        UniversalCategorySqlDao dao = new UniversalCategorySqlDao(ds, new LocalCategorySqlDao(ds));
//        dao.beginLoad();
//        dao.save(page);
//        dao.endLoad();
//
//        UniversalPage savedPage = dao.getById(23, 0);
//        assert (savedPage != null);
//        assert (page.equals(savedPage));
//        assert (page.getNumberOfEntities() == savedPage.getNumberOfEntities());
//        assert (page.getLanguageSetOfExistsInLangs().equals(savedPage.getLanguageSetOfExistsInLangs()));
//        assert (page.getClarity() == savedPage.getClarity());
//
//        List<Integer> pageIds = new ArrayList<Integer>();
//        pageIds.add(23);
//        Map<Integer, UniversalCategory> pages = dao.getByIds(pageIds, 0);
//        assert (pages.size() == 1);
//        assert (pages.get(23).equals(page));
//        assert (pages.get(23).equals(savedPage));
//    }
}
