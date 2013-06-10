package org.wikapidia.core.dao.sql;

import org.wikapidia.core.model.LocalArticle;

import javax.sql.DataSource;
import java.sql.SQLException;

public class LocalArticleSqlDao extends LocalPageSqlDao<LocalArticle>{
    public LocalArticleSqlDao(DataSource dataSource) throws SQLException {
        super(dataSource);
    }


}
