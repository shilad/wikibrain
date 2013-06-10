package org.wikapidia.dao.load;

import com.jolbox.bonecp.BoneCPDataSource;
import org.apache.commons.io.FileUtils;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.parser.wiki.ParserVisitor;
import org.wikapidia.parser.wiki.WikiTextDumpParser;
import org.wikapidia.parser.xml.PageXml;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 */
public class DumpParserMain {
    private static final Logger LOG = Logger.getLogger(DumpParserMain.class.getName());
    private final List<ParserVisitor> visitors;
    private final AtomicInteger counter = new AtomicInteger();

    public DumpParserMain(List<ParserVisitor> visitors) {
        this.visitors = new ArrayList<ParserVisitor>(visitors);
        this.visitors.add(0, new ParserVisitor() {
            @Override
            public void beginPage(PageXml page) {
                if (counter.incrementAndGet() % 100 == 0) {
                    LOG.info("processing article " + counter.get());
                }
            }
        });
    }

    /**
     * Expects file name format starting with lang + "wiki" for example, "enwiki"
     * @param file
     */
    public void load(File file) {
        int i = file.getName().indexOf("wiki");
        if (i < 0) {
            throw new IllegalArgumentException("invalid filename. Expected prefix, for example 'enwiki-...'");
        }
        String langCode = file.getName().substring(0, i);
        LanguageInfo lang = LanguageInfo.getByLangCode(langCode);
        WikiTextDumpParser parser = new WikiTextDumpParser(file, lang);
        parser.parse(visitors);
    }

    public static void main(String args[]) throws ClassNotFoundException, SQLException, IOException {
        // TODO: this "setup" would come from a configuration file
        Class.forName("org.h2.Driver");

        BoneCPDataSource ds = new BoneCPDataSource();
        ds.setJdbcUrl("jdbc:h2:db/local-page");
        ds.setUsername("sa");
        ds.setPassword("");

        Connection conn = ds.getConnection();
        conn.createStatement().execute(
                FileUtils.readFileToString(new File("../wikapidia-core/src/main/resources/schema.sql"))
        );
        conn.close();

        LocalPageDao dao = new LocalPageDao(ds);
        List<ParserVisitor> visitors = new ArrayList<ParserVisitor>();
        visitors.add(new LocalPageLoader(dao));


        String path = "/tmp/simplewiki.xml";
        DumpParserMain loader = new DumpParserMain(visitors);
        loader.load(new File(path));
    }
}
