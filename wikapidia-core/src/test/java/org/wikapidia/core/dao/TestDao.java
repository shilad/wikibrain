package org.wikapidia.core.dao;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;
import junit.framework.Test;
import org.wikapidia.core.model.Article;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: research
 * Date: 6/5/13
 * Time: 11:43 AM
 * To change this template use File | Settings | File Templates.
 */
public class TestDao {
    @Test
    public void testArticle() throws ClassNotFoundException, IOException, SQLException {
        Class.forName("org.h2.Driver");
        File tmpDir = File.createTempFile("wikapidia-h2", null);
        tmpDir.delete();
        tmpDir.deleteOnExit();
        tmpDir.mkdirs();
        try {
            BoneCPDataSource ds = new BoneCPDataSource();
            ds.setJdbcUrl("jdbc:h2:"+new File(tmpDir,"db").getAbsolutePath());
            ds.setUsername("sa");
            ds.setPassword("");
            ArticleDao ad = new ArticleDao(ds);
            Article article = new Article(1,"test", Article.NameSpace.MAIN,Article.PageType.STANDARD);
            ad.save(article);

        }
    }
}
