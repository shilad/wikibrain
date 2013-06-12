package org.wikapidia.dao.load;

import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.parser.wiki.ParsedLink;
import org.wikapidia.parser.wiki.ParserVisitor;

/**
 */
public class LocalLinkLoader extends ParserVisitor {
    private final LocalLinkDao dao;

    public LocalLinkLoader(LocalLinkDao dao) {
        this.dao = dao;
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
            dao.save(
                    new LocalLink(
                            link.target.getLanguage(),
                            link.text,
                            link.location.getXml().getPageId(),
                            link.target.hashCode(), // FIXME.. YUCK!
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
