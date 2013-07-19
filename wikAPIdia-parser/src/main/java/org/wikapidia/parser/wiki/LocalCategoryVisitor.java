package org.wikapidia.parser.wiki;

import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalCategoryMemberDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalCategoryMember;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 */
public class LocalCategoryVisitor extends ParserVisitor {

    private static final Logger LOG = Logger.getLogger(LocalCategoryVisitor.class.getName());

    private final LocalPageDao pageDao;
    private final LocalCategoryMemberDao catMemDao;
    private AtomicInteger counter = new AtomicInteger();

    public LocalCategoryVisitor(LocalPageDao pageDao, LocalCategoryMemberDao catMemDao) {
        this.pageDao = pageDao;
        this.catMemDao = catMemDao;
    }

    @Override
    public void category(ParsedCategory cat) throws WikapidiaException {
        try{
            Language lang = cat.category.getLanguage();
            LanguageInfo langInfo = LanguageInfo.getByLanguage(lang);

            int c = counter.getAndIncrement();
            if(c % 100000 == 0) LOG.info("Visited category #" + c);

            String catText = cat.category.getCanonicalTitle().split("\\|")[0]; //piped cat link
            catText = catText.split("#")[0]; //cat subsection

            Title catTitle = new Title(catText, langInfo);
            if(!isCategory(catText, langInfo) && !catTitle.getNamespace().equals(NameSpace.CATEGORY))  {
                throw new WikapidiaException("Thought it was a category, was not a category.");
            }

            int catId = pageDao.getIdByTitle(catTitle.getCanonicalTitle(), lang, NameSpace.CATEGORY);
            catMemDao.save(
                    new LocalCategoryMember(
                            catId,
                            cat.location.getXml().getLocalId(),
                            lang
                    ));
        } catch (DaoException e) {
            throw new WikapidiaException(e);
        }

    }
    private boolean isCategory(String link, LanguageInfo lang){
        for(String categoryName : lang.getCategoryNames()){
            if(link.length()>categoryName.length()&&
                    link.substring(0, categoryName.length() + 1).toLowerCase().equals(categoryName.toLowerCase() + ":")){
                return true;
            }
        }
        return false;
    }
}
