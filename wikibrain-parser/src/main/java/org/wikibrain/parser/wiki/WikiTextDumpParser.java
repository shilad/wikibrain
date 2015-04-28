package org.wikibrain.parser.wiki;

import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.RawPageDao;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpThreadUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class
        WikiTextDumpParser {
    public static final Logger LOG = LoggerFactory.getLogger(WikiTextDumpParser.class);

    // maximum number of raw pages in the parsing buffer
    public static final int MAX_QUEUE = 1000;

    private final LanguageInfo language;
    private final RawPageDao rawPageDao;
    private final LanguageSet allowedLanguages;
    private int maxThreads = WpThreadUtils.getMaxThreads();


    public WikiTextDumpParser(RawPageDao rawPageDao, LanguageInfo language) {
        this(rawPageDao, language, null);
    }

    public WikiTextDumpParser(RawPageDao rawPageDao, LanguageInfo language, LanguageSet allowedIllLangs) {
        this.language = language;
        this.allowedLanguages = allowedIllLangs;
        this.rawPageDao = rawPageDao;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    /**
     * Parses the input file completely. First splits the file into individual PageXmls via
     * DumpPageXmlParser, then parses each page via WikiTextParser
     *
     * @param visitor extracts data from side effects
     */
    public void parse(ParserVisitor visitor) throws DaoException {
        parse(Arrays.asList(visitor));
    }

    public synchronized void parse(List<ParserVisitor> visitors) throws DaoException {

        DaoFilter daoFilter = new DaoFilter().setLanguages(language.getLanguage());
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
                parser = new WikiTextParser(language, allowedLanguages, visitors);
                parserHolder.set(parser);
            }

            try {
                parser.parse(rp);
            } catch (Exception e) {
                String title = "unknown";
                LOG.warn("exception while parsing " + title, e);
            }
        }
    }
}
