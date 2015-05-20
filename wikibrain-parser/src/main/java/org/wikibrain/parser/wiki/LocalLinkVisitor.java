package org.wikibrain.parser.wiki;

import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalLinkDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.model.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class LocalLinkVisitor extends ParserVisitor {
    private static final Logger LOG = LoggerFactory.getLogger(LocalLinkVisitor.class);

    private final LocalLinkDao linkDao;
    private final LocalPageDao pageDao;
    private final MetaInfoDao metaDao;
    private AtomicInteger counter = new AtomicInteger();
    private Listener linkListener = null;

    public LocalLinkVisitor(LocalLinkDao linkDao, LocalPageDao pageDao, MetaInfoDao metaDao) {
        this.linkDao = linkDao;
        this.pageDao = pageDao;
        this.metaDao = metaDao;
    }

    public void setLinkListener(Listener linkListener) {
        this.linkListener = linkListener;
    }

    @Override
    public void link(ParsedLink link) throws WikiBrainException {
        Language lang = link.target.getLanguage();
        LanguageInfo langInfo = LanguageInfo.getByLanguage(lang);

        int c = counter.getAndIncrement();
        if(c % 1000000 == 0) LOG.info("Visited link #" + c);
        try {
            LocalLink.LocationType loc = LocalLink.LocationType.NONE;
            if (link.location.getParagraph() == 0) {
                loc = LocalLink.LocationType.FIRST_PARA;
            } else if (link.location.getSection() == 0) {
                loc = LocalLink.LocationType.FIRST_SEC;
            }

            String targetText = link.target.getCanonicalTitle();

            //Wikipedia ignores colons at the beginning of links
            // and uses them to overcome technical restrictions
            if (!targetText.isEmpty() && targetText.charAt(0)==':'){
                targetText = targetText.substring(1,targetText.length());
                link.target = new Title(targetText, langInfo);
            }
            int destId = pageDao.getIdByTitle(targetText, lang, link.target.getNamespace());
            LocalLink ll = new LocalLink(
                    lang,
                    link.text,
                    link.location.getXml().getLocalId(),
                    destId,
                    true,
                    link.location.getLocation(),
                    true,
                    loc
            );
            linkDao.save(ll);
            metaDao.incrementRecords(LocalLink.class, lang);
            if (linkListener != null) {
                linkListener.notify(ll);
            }
        } catch (DaoException e) {
            metaDao.incrementErrorsQuietly(LocalLink.class, lang);
            throw new WikiBrainException(e);
        }
    }

    @Override
    public void parseError(RawPage rp, Exception e) {
        metaDao.incrementErrorsQuietly(LocalLink.class, rp.getLanguage());
    }


    public static interface Listener {
        public void notify(LocalLink link);
    }
}
