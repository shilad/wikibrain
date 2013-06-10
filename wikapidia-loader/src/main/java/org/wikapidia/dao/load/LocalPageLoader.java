package org.wikapidia.dao.load;

import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.parser.wiki.ParserVisitor;
import org.wikapidia.parser.xml.PageXml;

import java.sql.SQLException;

/**
 */
public class LocalPageLoader extends ParserVisitor {
    private final LocalPageSqlDao dao;

    public LocalPageLoader(LocalPageSqlDao dao) {
        this.dao = dao;
    }

    @Override
    public void beginPage(PageXml xml) throws WikapidiaException {
        try {
            dao.save(new LocalPage(xml.getLang(), xml.getPageId(), xml.getTitle(), xml.getType()));
        } catch (SQLException e) {
            throw new WikapidiaException(e);
        }
    }
}
