package org.wikapidia.dao.load;

import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalCategoryMemberDao;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.*;
import org.wikapidia.parser.wiki.ParsedLink;
import org.wikapidia.parser.wiki.ParserVisitor;

/**
 */
public class LocalLinkLoader extends ParserVisitor {
    private final LocalLinkDao linkDao;

    public LocalLinkLoader(LocalLinkDao linkDao) {
        this.linkDao = linkDao;
    }

    @Override
    public void link(ParsedLink link) throws WikapidiaException {
        try {
            LocalLink.LocationType loc = LocalLink.LocationType.NONE;
            if (link.location.getParagraph() == 0) {
                loc = LocalLink.LocationType.FIRST_PARA;
            } else if (link.location.getSection() == 0) {
                loc = LocalLink.LocationType.FIRST_SEC;
            }
            Language lang = link.target.getLanguage();
            linkDao.save(
                    new LocalLink(
                            lang,
                            link.text,
                            link.location.getXml().getPageId(),
                            -1,
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
