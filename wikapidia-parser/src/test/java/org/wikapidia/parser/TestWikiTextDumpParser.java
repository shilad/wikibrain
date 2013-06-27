package org.wikapidia.parser;

import com.jolbox.bonecp.BoneCPDataSource;
import org.junit.Ignore;
import org.junit.Test;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalCategoryMemberDao;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.sql.LocalCategoryMemberSqlDao;
import org.wikapidia.core.dao.sql.LocalLinkSqlDao;
import org.wikapidia.core.dao.sql.LocalPageSqlDao;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.parser.wiki.*;
import org.wikapidia.core.model.RawPage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 */
public class TestWikiTextDumpParser {
    public static final File SIMPLE_DUMP = new File("../wikapidia/wikapidia-loader/simplewiki-20130608-pages-articles.xml");
    public static final LanguageInfo SIMPLE = LanguageInfo.getByLangCode("simple");

    @Ignore
    @Test
    public void test1() throws DaoException {
        List<String> allowedIllLangs = new ArrayList<String>();
        allowedIllLangs.add("simple");

        // Scans for ILLs in all languages
        WikiTextDumpParser wtdp = new WikiTextDumpParser(null, null);

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
        LocalLinkDao linkDao = new LocalLinkSqlDao(ds);
        LocalPageDao pageDao = new LocalPageSqlDao(ds);
        LocalCategoryMemberDao catMemDao = new LocalCategoryMemberSqlDao(ds);

        linkDao.beginLoad();
        catMemDao.beginLoad();

        ParserVisitor linkVisitor = new LocalLinkVisitor(linkDao, pageDao);
        ParserVisitor catVisitor = new LocalCategoryVisitor(pageDao, catMemDao);

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
