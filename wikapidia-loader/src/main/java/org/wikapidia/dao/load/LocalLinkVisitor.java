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
public class LocalLinkVisitor extends ParserVisitor {
    private final LocalLinkDao linkDao;
    private final LocalPageDao pageDao;
    private final LocalCategoryMemberDao catMemDao;

    public LocalLinkVisitor(LocalLinkDao linkDao, LocalPageDao pageDao, LocalCategoryMemberDao catMemDao) {
        this.linkDao = linkDao;
        this.pageDao = pageDao;
        this.catMemDao = catMemDao;
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
            LanguageInfo langInfo = LanguageInfo.getByLanguage(lang);

            String linkText = link.text.split("|")[0]; //piped link
            linkText = linkText.split("#")[0]; //subsection

            //Wikipedia ignores colons at the beginning of links
            // and uses them to overcome technical restrictions
            if (linkText.length()>0&&linkText.charAt(0)==':'){
                linkText = linkText.substring(1,linkText.length());
            }
            Title linkTitle = new Title(linkText, langInfo);
            int destId = pageDao.getIdByTitle(linkTitle.getCanonicalTitle(), lang, linkTitle.getNamespace());
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
