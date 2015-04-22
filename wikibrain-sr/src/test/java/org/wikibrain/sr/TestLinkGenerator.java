package org.wikibrain.sr;

import com.jolbox.bonecp.BoneCPDataSource;
import org.junit.Test;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.sql.LocalLinkSqlDao;
import org.wikibrain.core.dao.sql.LocalPageSqlDao;
import org.wikibrain.core.dao.sql.WpDataSource;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.Title;
import org.wikibrain.sr.disambig.Disambiguator;
import org.wikibrain.sr.disambig.TopResultDisambiguator;
import org.wikibrain.sr.utils.ExplanationFormatter;
import org.wikibrain.sr.vector.GoogleSimilarity;
import org.wikibrain.sr.vector.LinkGenerator;
import org.wikibrain.sr.vector.SparseVectorSRMetric;
import org.wikibrain.sr.vector.SparseVectorGenerator;
import org.wikibrain.utils.WpIOUtils;

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
public class TestLinkGenerator {
    private static final Language SIMPLE = Language.getByLangCode("simple");

    private static void printResult(SRResult result, ExplanationFormatter expf) throws DaoException {
        if (result == null){
            System.out.println("Result was null");
        }
        else {
            System.out.println("Similarity score: "+result.getScore());
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
        File tmpDir = WpIOUtils.createTempDirectory("wikibrain-h2");
        BoneCPDataSource ds = new BoneCPDataSource();
        ds.setJdbcUrl("jdbc:h2:"+new File(tmpDir,"db").getAbsolutePath());
        ds.setUsername("sa");
        ds.setPassword("");
        WpDataSource wpDs = new WpDataSource(ds);

        LanguageInfo lang = LanguageInfo.getByLangCode("simple");
        LocalPageDao dao = new LocalPageSqlDao(wpDs);
        LocalLinkSqlDao linkDao = new LocalLinkSqlDao(wpDs);
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

        SparseVectorGenerator generator = new LinkGenerator(SIMPLE, linkDao, dao, false, null);
        BaseSRMetric srIn = new SparseVectorSRMetric("srIn", SIMPLE, dao, disambiguator,generator, new GoogleSimilarity(6));;
        generator = new LinkGenerator(SIMPLE, linkDao, dao, true, null);
        BaseSRMetric srOut =  new SparseVectorSRMetric("srIn", SIMPLE, dao, disambiguator,generator, new GoogleSimilarity(6));;

        double rIn = srIn.similarity(page1.getLocalId(), page2.getLocalId(), true).getScore();
        assert((1-((Math.log(4)-Math.log(3)) / (Math.log(6) - Math.log(3))))==rIn);
        assert(srIn.similarity(page1.getLocalId(),page1.getLocalId(),true).getScore()==1);

        double rOut = srOut.similarity(page3.getLocalId(),page4.getLocalId(),true).getScore();
        assert((1-((Math.log(2)-Math.log(1)) / (Math.log(6) - Math.log(1))))==rOut);
        assert(srOut.similarity(page3.getLocalId(),page3.getLocalId(),true).getScore()==1);
    }


}
