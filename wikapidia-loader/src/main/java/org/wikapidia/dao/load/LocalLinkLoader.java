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
    private final LocalPageDao pageDao;
    private final LocalCategoryMemberDao catMemDao;

    public LocalLinkLoader(LocalLinkDao linkDao, LocalPageDao pageDao, LocalCategoryMemberDao catMemDao) {
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
            if(isCategory(link.text, langInfo)){
                catMemDao.save(
                        new LocalCategoryMember(pageDao.getIdByTitle(Title.canonicalize(link.text, langInfo),
                                lang, NameSpace.CATEGORY),
                                link.location.getXml().getPageId(),
                                lang)
                );
            }
            else{
                if(isLinkToCategory(link.text, langInfo))
                    link.text = link.text.substring(1,link.text.length());
                Title linkTitle = new Title(link.text, langInfo);
                linkDao.save(
                        new LocalLink(
                                lang,
                                link.text,
                                link.location.getXml().getPageId(),
                                pageDao.getIdByTitle(linkTitle.getCanonicalTitle(), lang, linkTitle.getNamespace()),
                                true,
                                link.location.getLocation(),
                                true,
                                loc
                        ));
            }
        } catch (DaoException e) {
            throw new WikapidiaException(e);
        }
    }

    public boolean isCategory(String link, LanguageInfo languageInfo){
        for(String categoryName : languageInfo.getCategoryNames()){
            if(link.substring(categoryName.length()+1).toLowerCase().equals(categoryName+":")){
                return true;
            }
        }
        return false;
    }

    public boolean isLinkToCategory(String link, LanguageInfo languageInfo){
        for(String categoryName : languageInfo.getCategoryNames()){
            if(link.substring(categoryName.length()+2).toLowerCase().equals(":" + categoryName+":")){
                return true;
            }
        }
        return false;
    }
}
