package org.wikapidia.core.dao.sql;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.wikapidia.core.WikapidiaException;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.JooqUtils;
import org.wikapidia.core.dao.LocalCategoryDao;
import org.wikapidia.core.jooq.Tables;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalCategory;
import org.wikapidia.core.model.PageType;
import org.wikapidia.core.model.Title;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class LocalCategorySqlDao extends LocalCategoryDao{

    public LocalCategorySqlDao(DataSource dataSource) throws SQLException {
        super(dataSource);
    }

    @Override
    public LocalCategory getByPageId(Language language, int pageId) throws DaoException{
        Connection conn=null;
        try {
            conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Record record = context.select().
                    from(Tables.LOCAL_PAGE).
                    where(Tables.LOCAL_PAGE.PAGE_ID.equal(pageId)).
                    and(Tables.LOCAL_PAGE.LANG_ID.equal((short) language.getId())).
                    fetchOne();
            return buildLocalCategory(record);
        } catch (SQLException e){
            throw new DaoException(e);
        }
        finally {
            if (!conn.equals(null))
            {conn.close();}
        }
    }


    @Override
    public LocalCategory getByTitle(Language language, Title title, PageType ns) throws DaoException{
        try {
            Connection conn = ds.getConnection();
            DSLContext context = DSL.using(conn, dialect);
            Record record = context.select().
                    from(Tables.LOCAL_PAGE).
                    where(Tables.LOCAL_PAGE.TITLE.equal(title.getCanonicalTitle())).
                    and(Tables.LOCAL_PAGE.LANG_ID.equal((short) title.getLanguage().getId())).
                    and(Tables.LOCAL_PAGE.NS.equal((short) PageType.CATEGORY.getNamespace().getValue())).
                    fetchOne();
            return buildLocalCategory(record);
        } catch (SQLException e){
            throw new DaoException(e);
        }
        finally {
            conn.close();
        }
    }


}
