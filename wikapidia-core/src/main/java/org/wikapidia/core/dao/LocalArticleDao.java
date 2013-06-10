package org.wikapidia.core.dao;

import org.jooq.Record;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.PageType;
import org.wikapidia.core.model.Title;

import javax.sql.DataSource;

public abstract class LocalArticleDao extends LocalPageDao<LocalArticle> {

    public LocalArticleDao(DataSource ds) {
        super(ds);
    }

    protected LocalArticle buildLocalArticle(Record record) throws DaoException {
        if (record == null ) { return null; }
        if (record.getValue(Tables.LOCAL_PAGE.NS) != PageType.ARTICLE.getNamespace().getValue()) {
            throw new DaoException("Page type mismatch"); // or some other text
        }

        short langId = record.getValue(Tables.LOCAL_PAGE.LANG_ID);
        Language language = Language.getById(langId);
        int pageId = record.getValue(Tables.LOCAL_PAGE.PAGE_ID);
        Title title = new Title(record.getValue(Tables.LOCAL_PAGE.TITLE), LanguageInfo.getById(langId));

        return new LocalArticle(language, pageId, title);
    }
}
