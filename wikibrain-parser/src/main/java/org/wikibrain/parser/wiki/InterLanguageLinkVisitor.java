package org.wikibrain.parser.wiki;

import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.InterLanguageLink;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.core.model.Title;

import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class InterLanguageLinkVisitor extends ParserVisitor {
    private static final Logger LOG = LoggerFactory.getLogger(InterLanguageLinkVisitor.class);

    private final LocalPageDao pageDao;
    private final MetaInfoDao metaDao;
    private final InterLanguageLinkDao illDao;
    private final LanguageSet destLangs;

    private AtomicInteger encountered = new AtomicInteger();
    private AtomicInteger retained = new AtomicInteger();

    public InterLanguageLinkVisitor(InterLanguageLinkDao illDao, LocalPageDao pageDao, MetaInfoDao metaDao) {
        this(illDao, pageDao, metaDao, LanguageSet.ALL);
    }

    public InterLanguageLinkVisitor(InterLanguageLinkDao illDao, LocalPageDao pageDao, MetaInfoDao metaDao, LanguageSet destLangs) {
        this.illDao = illDao;
        this.pageDao = pageDao;
        this.metaDao = metaDao;
        this.destLangs = destLangs;
    }

    @Override
    public void ill(ParsedIll ill) throws WikiBrainException {
        int c = encountered.getAndIncrement();
        if(c % 10000==0) LOG.info("Encountered ill #" + c + ", retained " + retained.get());
        Language srcLang = null;
        try {
            srcLang = ill.location.getXml().getLanguage();
            int srcId = pageDao.getIdByTitle(ill.location.getXml().getTitle());
            Language destLang = ill.title.getLanguage();
            int destId = pageDao.getIdByTitle(ill.title);
            if (srcId > 0 && destId > 0 && destLangs.containsLanguage(destLang)) {
                illDao.save(new InterLanguageLink(srcLang, srcId, destLang, destId));
                retained.incrementAndGet();
            }
            metaDao.incrementRecords(InterLanguageLinkDao.class, srcLang);
        } catch (DaoException e) {
            metaDao.incrementErrorsQuietly(InterLanguageLinkDao.class, srcLang);
            throw new WikiBrainException(e);
        }
    }

    @Override
    public void parseError(RawPage rp, Exception e) {
        metaDao.incrementErrorsQuietly(LocalLink.class, rp.getLanguage());
    }


}
