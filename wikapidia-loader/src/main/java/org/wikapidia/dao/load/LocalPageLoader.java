package org.wikapidia.dao.load;

import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.parser.wiki.ParserVisitor;
import org.wikapidia.core.model.RawPage;

/**
 */
public class LocalPageLoader extends ParserVisitor {
    private final LocalPageDao dao;

    public LocalPageLoader(LocalPageDao dao) {
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
