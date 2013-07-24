package org.wikapidia.sr;

import com.jolbox.bonecp.BoneCPDataSource;
import org.junit.Test;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.sql.LocalArticleSqlDao;
import org.wikapidia.core.dao.sql.LocalLinkSqlDao;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.disambig.TopResultDisambiguator;
import org.wikapidia.sr.utils.ExplanationFormatter;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: Benjamin Hillmann, Matt Lesicko
 * Date: 7/1/13
 * Time: 2:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestMilneWitten {

    private static void printResult(SRResult result, ExplanationFormatter expf) throws DaoException {
        if (result == null){
            System.out.println("Result was null");
        }
        else {
            System.out.println("Similarity value: "+result.getNormalized());
            int explanationsSeen = 0;
            for (Explanation explanation : result.getExplanations()){
                System.out.println(expf.formatExplanation(explanation));
                if (++explanationsSeen>5){
                    break;
                }
            }
        }

    }

    @Test
    public void testArticle() throws ClassNotFoundException, IOException, SQLException, DaoException, ConfigurationException {
        Class.forName("org.h2.Driver");
        File tmpDir = File.createTempFile("wikapidia-h2", null);
        tmpDir.delete();
        tmpDir.deleteOnExit();
        tmpDir.mkdirs();

        BoneCPDataSource ds = new BoneCPDataSource();
        ds.setJdbcUrl("jdbc:h2:"+new File(tmpDir,"db").getAbsolutePath());
        ds.setUsername("sa");
        ds.setPassword("");

        LanguageInfo lang = LanguageInfo.getByLangCode("simple");
        LocalArticleSqlDao dao = new LocalArticleSqlDao(ds);
        LocalLinkSqlDao linkDao = new LocalLinkSqlDao(ds);
        ExplanationFormatter expf = new ExplanationFormatter(dao);

        dao.beginLoad();
        LocalPage page1 = new LocalPage(
                lang.getLanguage(),
                1,
                new Title("test1", lang),
                NameSpace.ARTICLE
        );
        dao.save(page1);
        LocalPage page2 = new LocalPage(
                lang.getLanguage(),
                2,
                new Title("test2", lang),
                NameSpace.ARTICLE
        );
        dao.save(page2);
        LocalPage page3 = new LocalPage(
                lang.getLanguage(),
                3,
                new Title("test3", lang),
                NameSpace.ARTICLE
        );
        dao.save(page3);
        LocalPage page4 = new LocalPage(
                lang.getLanguage(),
                4,
                new Title("test4", lang),
                NameSpace.ARTICLE
        );
        dao.save(page4);
        LocalPage page5 = new LocalPage(
                lang.getLanguage(),
                5,
                new Title("test5", lang),
                NameSpace.ARTICLE
        );
        dao.save(page5);
        LocalPage page6 = new LocalPage(
                lang.getLanguage(),
                6,
                new Title("test6", lang),
                NameSpace.ARTICLE
        );
        dao.save(page6);
        dao.endLoad();

        linkDao.beginLoad();
        LocalLink link1 = new LocalLink(lang.getLanguage(), "", 3, 1, false, 0, false, LocalLink.LocationType.NONE);
        linkDao.save(link1);
        LocalLink link2 = new LocalLink(lang.getLanguage(), "", 4, 1, false, 0, false, LocalLink.LocationType.NONE);
        linkDao.save(link2);
        LocalLink link3 = new LocalLink(lang.getLanguage(), "", 4, 2, false, 0, false, LocalLink.LocationType.NONE);
        linkDao.save(link3);
        LocalLink link4 = new LocalLink(lang.getLanguage(), "", 5, 2, false, 0, false, LocalLink.LocationType.NONE);
        linkDao.save(link4);
        LocalLink link5 = new LocalLink(lang.getLanguage(), "", 6, 2, false, 0, false, LocalLink.LocationType.NONE);
        linkDao.save(link5);
        LocalLink link6 = new LocalLink(lang.getLanguage(), "", 5, 1, false, 0, false, LocalLink.LocationType.NONE);
        linkDao.save(link6);
        LocalLink link7 = new LocalLink(lang.getLanguage(), "", 6, 1, false, 0, false, LocalLink.LocationType.NONE);
        linkDao.save(link7);
        linkDao.endLoad();

        Disambiguator disambiguator = new TopResultDisambiguator(null);

        BaseLocalSRMetric srIn = new LocalMilneWitten(disambiguator,linkDao,dao);
        BaseLocalSRMetric srOut =  new LocalMilneWitten(disambiguator,linkDao,dao,true);

        double rIn = srIn.similarity(page1, page2, true).getValue();
        assert((1-((Math.log(4)-Math.log(3)) / (Math.log(6) - Math.log(3))))==rIn);
        assert(srIn.similarity(page1,page1,true).getValue()==1);

        double rOut = srOut.similarity(page3,page4,true).getValue();
        assert((1-((Math.log(2)-Math.log(1)) / (Math.log(6) - Math.log(1))))==rOut);
        assert(srOut.similarity(page3,page3,true).getValue()==1);
    }


    }
