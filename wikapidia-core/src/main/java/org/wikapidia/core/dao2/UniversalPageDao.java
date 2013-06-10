package org.wikapidia.core.dao2;

import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.UniversalArticle;
import org.wikapidia.core.model.UniversalCategory;
import org.wikapidia.core.model.UniversalPage;

/**
 * Created with IntelliJ IDEA.
 * User: bjhecht
 */
public abstract class UniversalPageDao {

    /**
     * Returns a UniversalPage instance corresponding to the input universal id and type.
     * @param univId
     * @param type The type of the UniversalPage (e.g. UniversalArticle.class or UniversalCategory.class)
     * @return
     * @throws WikapidiaException
     */
    public abstract UniversalPage getUniversalPage(int univId, Class<? extends UniversalPage> type) throws WikapidiaException;

    /**
     * Important convenience function for getUniversalPage(int,class)
     * @param univId
     * @return
     * @throws WikapidiaException
     */
    public UniversalArticle getUniversalArticle(int univId) throws WikapidiaException {
        return (UniversalArticle)getUniversalPage(univId, UniversalArticle.class);
    }

    /**
     * Important convenience function for getUniversalPage(int,class)
     * @param univId
     * @return
     * @throws WikapidiaException
     */
    public UniversalCategory getUniversalCategory(int univId) throws WikapidiaException {
        return (UniversalCategory)getUniversalPage(univId, UniversalArticle.class);
    }



}
