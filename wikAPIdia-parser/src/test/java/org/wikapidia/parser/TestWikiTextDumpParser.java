package org.wikapidia.parser;

import com.jolbox.bonecp.BoneCPDataSource;
import org.junit.Ignore;
import org.junit.Test;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.dao.sql.*;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.parser.wiki.*;
import org.wikapidia.core.model.RawPage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 */
public class TestWikiTextDumpParser {
    public static final File SIMPLE_DUMP = new File("../wikapidia/wikAPIdia-loader/simplewiki-20130608-pages-articles.xml");
    public static final LanguageInfo SIMPLE = LanguageInfo.getByLangCode("simple");

    @Ignore
    @Test
    public void test1() throws DaoException {
        List<String> allowedIllLangs = new ArrayList<String>();
        allowedIllLangs.add("simple");

        // Scans for ILLs in all languages
        WikiTextParser.Factory factory = new JwplWikiTextParser.JwplFactory(new LanguageSet(allowedIllLangs));
        WikiTextDumpParser wtdp = new WikiTextDumpParser(null, null, factory);

        // Scans for ILLs in languages specified above only
        //WikiTextDumpParser wtdp = new WikiTextDumpParser(EN_DUMP, EN, allowedIllLangs);

        final AtomicInteger pageCounter = new AtomicInteger();
        final ArrayList<ParsedCategory> categories = new ArrayList<ParsedCategory>();
        final ArrayList<ParsedIll> ills = new ArrayList<ParsedIll>();
        final ArrayList<ParsedLink> links = new ArrayList<ParsedLink>();
        final ArrayList<ParsedRedirect> redirects = new ArrayList<ParsedRedirect>();

        List<ParserVisitor> visitors = new ArrayList<ParserVisitor>();

        ParserVisitor visitor = new ParserVisitor() {
            @Override
            public void beginPage(RawPage xml) {
                pageCounter.incrementAndGet();
            }

            @Override
            public void category(ParsedCategory category) {
                categories.add(category);
            }

            @Override
            public void ill(ParsedIll ill) {
                ills.add(ill);
            }

            @Override
            public void link(ParsedLink link) {
                links.add(link);
            }

            @Override
            public void redirect(ParsedRedirect redirect) {
                redirects.add(redirect);
            }
        };

        BoneCPDataSource ds = new BoneCPDataSource();
        ds.setJdbcUrl("jdbc:h2:"+"db/h2");
        ds.setUsername("sa");
        ds.setPassword("");
        WpDataSource wpDs = new WpDataSource(ds);
        LocalLinkDao linkDao = new LocalLinkSqlDao(wpDs);
        LocalPageDao pageDao = new LocalPageSqlDao(wpDs);
        LocalCategoryMemberDao catMemDao = new LocalCategoryMemberSqlDao(
                wpDs, new LocalCategorySqlDao(wpDs), new LocalArticleSqlDao(wpDs));
        MetaInfoDao metaDao = new MetaInfoSqlDao(wpDs);

        linkDao.beginLoad();
        catMemDao.beginLoad();

        ParserVisitor linkVisitor = new LocalLinkVisitor(linkDao, pageDao, metaDao);
        ParserVisitor catVisitor = new LocalCategoryVisitor(pageDao, catMemDao, metaDao);

        visitors.add(visitor);
        visitors.add(linkVisitor);
        visitors.add(catVisitor);

        wtdp.parse(visitors);

        System.out.println("Categories: " + categories.size());
        System.out.println("ILLs: " + ills.size());
        System.out.println("Links: " + links.size());
        System.out.println("Redirects: " + redirects.size());

        linkDao.endLoad();
        catMemDao.endLoad();
    }
}
