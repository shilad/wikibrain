package org.wikapidia.parser.wiki;

import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.RawPageDao;
import org.wikapidia.core.dao.SqlDaoIterable;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.parser.xml.DumpPageXmlParser;
import org.wikapidia.core.model.RawPage;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
public class WikiTextDumpParser {
    public static final Logger LOG = Logger.getLogger(WikiTextDumpParser.class.getName());

    private final LanguageInfo language;
    private final RawPageDao rawPageDao;
    private final LanguageSet allowedLanguages;

    public WikiTextDumpParser(RawPageDao rawPageDao, LanguageInfo language) {
        this(rawPageDao, language, null);
    }

    public WikiTextDumpParser(RawPageDao rawPageDao, LanguageInfo language, LanguageSet allowedIllLangs) {
        this.language = language;
        this.allowedLanguages = allowedIllLangs;
        this.rawPageDao = rawPageDao;
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

    public void parse(List<ParserVisitor> visitors) throws DaoException {
        WikiTextParser wtp = new WikiTextParser(language, allowedLanguages, visitors);
        DaoFilter daoFilter = new DaoFilter().setLanguages(language.getLanguage());
        Iterable<RawPage> pageIterator = rawPageDao.get(daoFilter);
        for(RawPage page : pageIterator) {
            try {
                wtp.parse(page);
            }
            catch (Exception e) {
                LOG.log(Level.WARNING, "parsing failed:", e);
            }
        }
    }
}
