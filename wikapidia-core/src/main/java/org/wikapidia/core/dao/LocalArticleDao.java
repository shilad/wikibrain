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

    public LocalArticleDao(DataSource ds) throws DaoException {
        super(ds);
    }

    /**
     * Build a LocalArticle from a database record representation
     * @param record a database record
     * @return a LocalArticle representation of the given database record
     * @throws org.wikapidia.core.dao.DaoException if the record is not an Article
     */
    protected LocalArticle buildLocalArticle(Record record) throws DaoException {
        if (record == null ) { return null; }

        short langId = record.getValue(Tables.LOCAL_PAGE.LANG_ID);
        Language language = Language.getById(langId);
        int pageId = record.getValue(Tables.LOCAL_PAGE.PAGE_ID);
        Title title = new Title(record.getValue(Tables.LOCAL_PAGE.TITLE), LanguageInfo.getById(langId));

        PageType pagetype = PageType.values()[record.getValue(Tables.LOCAL_PAGE.PAGE_TYPE)];
        if (!pagetype.equals(PageType.ARTICLE)){
            throw new DaoException("Tried to get ARTICLE, but found "+pagetype.name());
        }
        else {
            return new LocalArticle(language, pageId, title);
        }
    }
}
