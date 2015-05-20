package org.wikibrain.parser.wiki;

import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalCategoryMemberDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageInfo;
import org.wikibrain.core.model.LocalCategoryMember;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.core.model.Title;

import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class LocalCategoryVisitor extends ParserVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(LocalCategoryVisitor.class);

    private final LocalPageDao pageDao;
    private final LocalCategoryMemberDao catMemDao;
    private final MetaInfoDao metaDao;
    private AtomicInteger counter = new AtomicInteger();

    public LocalCategoryVisitor(LocalPageDao pageDao, LocalCategoryMemberDao catMemDao, MetaInfoDao metaDao) {
        this.pageDao = pageDao;
        this.catMemDao = catMemDao;
        this.metaDao = metaDao;
    }

    @Override
    public void category(ParsedCategory cat) throws WikiBrainException {
        Language lang = cat.category.getLanguage();
        try{
            LanguageInfo langInfo = LanguageInfo.getByLanguage(lang);

            int c = counter.getAndIncrement();
            if(c % 100000 == 0) LOG.info("Visited category #" + c);

            String catText = cat.category.getCanonicalTitle().split("\\|")[0]; //piped cat link
            catText = catText.split("#")[0]; //cat subsection

            Title catTitle = new Title(catText, langInfo);
            if(!isCategory(catText, langInfo) && !catTitle.getNamespace().equals(NameSpace.CATEGORY))  {
                throw new WikiBrainException("Thought it was a category, was not a category.");
            }

            int catId = pageDao.getIdByTitle(catTitle.getCanonicalTitle(), lang, NameSpace.CATEGORY);
            catMemDao.save(
                    new LocalCategoryMember(
                            catId,
                            cat.location.getXml().getLocalId(),
                            lang
                    ));
            metaDao.incrementRecords(LocalCategoryMember.class, lang);
        } catch (DaoException e) {
            metaDao.incrementErrorsQuietly(LocalCategoryMember.class, lang);
            throw new WikiBrainException(e);
        }

    }

    @Override
    public void parseError(RawPage rp, Exception e) {
        metaDao.incrementErrorsQuietly(LocalCategoryMember.class, rp.getLanguage());
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
