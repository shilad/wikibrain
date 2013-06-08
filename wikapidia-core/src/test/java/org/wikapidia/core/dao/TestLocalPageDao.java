package org.wikapidia.core.dao;


import com.jolbox.bonecp.BoneCPDataSource;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.Article;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.PageType;
import org.wikapidia.core.model.Title;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class TestLocalPageDao {
    @Test
    public void testArticle() throws ClassNotFoundException, IOException, SQLException {
        Class.forName("org.h2.Driver");
        File tmpDir = File.createTempFile("wikapidia-h2", null);
        tmpDir.delete();
        tmpDir.deleteOnExit();
        tmpDir.mkdirs();

        BoneCPDataSource ds = new BoneCPDataSource();
        ds.setJdbcUrl("jdbc:h2:"+new File(tmpDir,"db").getAbsolutePath());
        ds.setUsername("sa");
        ds.setPassword("");

        Connection conn = ds.getConnection();
        conn.createStatement().execute(
                FileUtils.readFileToString(new File("src/main/resources/schema.sql"))
        );
        conn.close();


        LanguageInfo lang = LanguageInfo.getByLangCode("en");
        LocalPageDao ad = new LocalPageDao(ds);
        LocalPage page = new LocalPage(lang.getLanguage(),
                7, new Title("test", false, lang), PageType.ARTICLE
        );
        ad.save(page);

        LocalPage savedPage = ad.get(lang.getLanguage(), 7);
        assert (savedPage != null);
        assert (page.getLocalId() == savedPage.getLocalId());
        assert (page.getTitle().equals(savedPage.getTitle()));
        assert (page.getPageType().equals(savedPage.getPageType()));
//
//        WikapidiaIterable<Article> articles = ad.query("test");
//        assert (articles != null);
//        savedArticle = articles.iterator().next();
//        assert (savedArticle.getId()==1);
//        assert (savedArticle.getTitle().equals(article.getTitle()));
//        assert (savedArticle.getNs().equals(article.getNs()));
//        assert (savedArticle.getType().equals(article.getType()));
//        assert (savedArticle.getText().equals(article.getText()));
//
//        articles = ad.query(Article.NameSpace.MAIN);
//        savedArticle = articles.iterator().next();
//        assert (savedArticle.getId()==1);
//        assert (savedArticle.getTitle().equals(article.getTitle()));
//        assert (savedArticle.getNs().equals(article.getNs()));
//        assert (savedArticle.getType().equals(article.getType()));
//        assert (savedArticle.getText().equals(savedArticle.getText()));
//
//        articles = ad.query("test",Article.NameSpace.MAIN);
//        savedArticle = articles.iterator().next();
//        assert (savedArticle.getId()==1);
//        assert (savedArticle.getTitle().equals(article.getTitle()));
//        assert (savedArticle.getNs().equals(article.getNs()));
//        assert (savedArticle.getType().equals(article.getType()));
//        assert (savedArticle.getText().equals(savedArticle.getText()));
//
//        articles = ad.query("wrong");
//        assert (articles.iterator().hasNext()==false);
    }
}
