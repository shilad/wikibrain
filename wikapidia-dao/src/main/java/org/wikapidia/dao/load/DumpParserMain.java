package org.wikapidia.dao.load;

import com.jolbox.bonecp.BoneCPDataSource;
import org.apache.commons.io.FileUtils;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
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

    public static void main(String args[]) throws ClassNotFoundException, SQLException, IOException, ConfigurationException {

        // TODO: figure out command line idioms and spice this up.
        File pathConf = (args.length == 0) ? null : new File(args[0]);
        Configurator conf = new Configurator(new Configuration(pathConf));

        LocalPageDao dao = (LocalPageDao) conf.get(LocalPageDao.class);
        List<ParserVisitor> visitors = new ArrayList<ParserVisitor>();
        visitors.add(new LocalPageLoader(dao));

        String path = "/tmp/simplewiki.xml";
        DumpParserMain loader = new DumpParserMain(visitors);

        dao.beginLoad();
        loader.load(new File(path));
        dao.endLoad();
    }
}
