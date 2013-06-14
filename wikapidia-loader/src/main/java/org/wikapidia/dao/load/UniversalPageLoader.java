package org.wikapidia.dao.load;

import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.UniversalPageDao;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.parser.wiki.ParserVisitor;

/**
 */
public class UniversalPageLoader extends ParserVisitor{
    private final UniversalPageDao dao;

    public UniversalPageLoader(UniversalPageDao dao) {
        this.dao = dao;
    }

    @Override
    public void beginPage(RawPage xml) throws WikapidiaException {
        try {
            dao.save(new LocalPage(xml.getLang(), xml.getPageId(), xml.getTitle(), xml.getNamespace()));
        } catch (DaoException e) {
            throw new WikapidiaException(e);
        }
    }
}
