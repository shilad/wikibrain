package org.wikapidia.parser.wiki;

import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.*;

/**
 */
public class LocalLinkVisitor extends ParserVisitor {
    private final LocalLinkDao linkDao;
    private final LocalPageDao pageDao;
    private int counter;

    public LocalLinkVisitor(LocalLinkDao linkDao, LocalPageDao pageDao) {
        this.linkDao = linkDao;
        this.pageDao = pageDao;
        this.counter = 0;
    }

    @Override
    public void link(ParsedLink link) throws WikapidiaException {
        if(counter%10000==0)
            System.out.println("Visited link #" + counter);
        counter++;
        try {
            LocalLink.LocationType loc = LocalLink.LocationType.NONE;
            if (link.location.getParagraph() == 0) {
                loc = LocalLink.LocationType.FIRST_PARA;
            } else if (link.location.getSection() == 0) {
                loc = LocalLink.LocationType.FIRST_SEC;
            }
            Language lang = link.target.getLanguage();
            LanguageInfo langInfo = LanguageInfo.getByLanguage(lang);

            String targetText = link.target.getCanonicalTitle();

            //Wikipedia ignores colons at the beginning of links
            // and uses them to overcome technical restrictions
            if (targetText.length()>0&&targetText.charAt(0)==':'){
                targetText = targetText.substring(1,targetText.length());
                link.target = new Title(targetText, langInfo);
            }
            int destId = pageDao.getIdByTitle(targetText, lang, link.target.getNamespace());
            linkDao.save(
                    new LocalLink(
                            lang,
                            link.text,
                            link.location.getXml().getPageId(),
                            destId,
                            true,
                            link.location.getLocation(),
                            true,
                            loc
                    ));
        } catch (DaoException e) {
            throw new WikapidiaException(e);
        }
    }

}
