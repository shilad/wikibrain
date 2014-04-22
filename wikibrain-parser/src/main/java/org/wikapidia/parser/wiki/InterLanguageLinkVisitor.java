package org.wikapidia.parser.wiki;

import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.*;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.InterLanguageLink;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.core.model.Title;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 */
public class InterLanguageLinkVisitor extends ParserVisitor {
    private static final Logger LOG = Logger.getLogger(InterLanguageLinkVisitor.class.getName());

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
    public void ill(ParsedIll ill) throws WikapidiaException {
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
            throw new WikapidiaException(e);
        }
    }

    @Override
    public void parseError(RawPage rp, Exception e) {
        metaDao.incrementErrorsQuietly(LocalLink.class, rp.getLanguage());
    }


}
