package org.wikapidia.parser.wiki;

import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.RawPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.utils.ParallelForEach;
import org.wikapidia.utils.Procedure;
import org.wikapidia.utils.WpThreadUtils;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
public class WikiTextDumpParser {
    public static final Logger LOG = Logger.getLogger(WikiTextDumpParser.class.getName());

    // maximum number of raw pages in the parsing buffer
    public static final int MAX_QUEUE = 1000;

    private final Language language;
    private final RawPageDao rawPageDao;
    private final WikiTextParser.Factory parserFactory;
    private int maxThreads = WpThreadUtils.getMaxThreads();


    public WikiTextDumpParser(RawPageDao rawPageDao, Language language, WikiTextParser.Factory parserFactory) {
        this.language = language;
        this.rawPageDao = rawPageDao;
        this.parserFactory = parserFactory;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    /**
     * Parses the input file completely. First splits the file into individual PageXmls via
     * DumpPageXmlParser, then parses each page via JwplWikiTextParser
     *
     * @param visitor extracts data from side effects
     */
    public void parse(ParserVisitor visitor) throws DaoException {
        parse(Arrays.asList(visitor));
    }

    public synchronized void parse(List<ParserVisitor> visitors) throws DaoException {

        DaoFilter daoFilter = new DaoFilter().setLanguages(language);
        ParallelForEach.iterate(
                rawPageDao.get(daoFilter).iterator(),
                maxThreads,
                MAX_QUEUE,
                new ParserProcedure(visitors),
                10000
        );
    }

    class ParserProcedure implements Procedure<RawPage> {
        private final ThreadLocal<WikiTextParser> parserHolder = new ThreadLocal<WikiTextParser>();
        private final List<ParserVisitor> visitors;

        ParserProcedure(List<ParserVisitor> visitors) {
            this.visitors = visitors;
        }

        @Override
        public void call(RawPage rp) {
            if (rp == null) {
                return;
            }

            WikiTextParser parser = parserHolder.get();
            if (parser == null) {
                parser = parserFactory.create(language);
                parserHolder.set(parser);
            }

            try {
                parser.parse(rp, visitors);
            } catch (Exception e) {
                String title = "unknown";
                LOG.log(Level.WARNING, "exception while parsing " + title, e);
            }
        }
    }
}
