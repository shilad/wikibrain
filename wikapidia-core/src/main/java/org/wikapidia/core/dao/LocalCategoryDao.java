package org.wikapidia.core.dao;

import org.jooq.Record;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalCategory;
import org.wikapidia.core.model.PageType;
import org.wikapidia.core.model.Title;

import javax.sql.DataSource;

public abstract class LocalCategoryDao extends LocalPageDao<LocalCategory> {

    public LocalCategoryDao(DataSource dataSource) throws DaoException {
        super(dataSource);
    }

    /**
     * Build a LocalCategory from a database record representation
     *
     * @param record a database record
     * @return a LocalCategory representation of the given database record
     * @throws DaoException if the record is not a Category
     */
    protected LocalCategory buildLocalCategory(Record record) throws DaoException {
        if (record == null) {
            return null;
        }
        Language lang = Language.getById(record.getValue(Tables.LOCAL_PAGE.LANG_ID));
        Title title = new Title(
                record.getValue(Tables.LOCAL_PAGE.TITLE), true,
                LanguageInfo.getByLanguage(lang));
        PageType ptype = PageType.values()[record.getValue(Tables.LOCAL_PAGE.PAGE_TYPE)];
        if (!ptype.equals(PageType.CATEGORY)){
            throw new DaoException("Tried to get CATEGORY, but found "+ptype.name());
        }
        else {
            return new LocalCategory(
                    lang,
                    record.getValue(Tables.LOCAL_PAGE.PAGE_ID),
                    title
            );
        }
    }
}
