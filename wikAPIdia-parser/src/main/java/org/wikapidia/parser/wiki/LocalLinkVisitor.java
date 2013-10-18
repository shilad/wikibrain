package org.wikapidia.parser.wiki;

import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.MetaInfoDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 */
public class LocalLinkVisitor extends ParserVisitor {
    private static final Logger LOG = Logger.getLogger(LocalLinkVisitor.class.getName());

    private final LocalLinkDao linkDao;
    private final LocalPageDao pageDao;
    private final MetaInfoDao metaDao;
    private AtomicInteger counter = new AtomicInteger();

    public LocalLinkVisitor(LocalLinkDao linkDao, LocalPageDao pageDao, MetaInfoDao metaDao) {
        this.linkDao = linkDao;
        this.pageDao = pageDao;
        this.metaDao = metaDao;
    }

    @Override
    public void link(ParsedLink link) throws WikapidiaException {
        Language lang = link.target.getLanguage();
        LanguageInfo langInfo = LanguageInfo.getByLanguage(lang);

        int c = counter.getAndIncrement();
        if(c % 100000==0) LOG.info("Visited link #" + c);
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
            linkDao.save(
                    new LocalLink(
                            lang,
                            link.text,
                            link.location.getXml().getLocalId(),
                            destId,
                            true,
                            link.location.getLocation(),
                            true,
                            loc
                    ));
            metaDao.incrementRecords(LocalLink.class, lang);
        } catch (DaoException e) {
            metaDao.incrementErrorsQuietly(LocalLink.class, lang);
            throw new WikapidiaException(e);
        }
    }

}
