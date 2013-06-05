package org.wikapidia.core.dao;


import com.jolbox.bonecp.BoneCPDataSource;
import org.junit.Test;
import org.wikapidia.core.model.Article;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class TestDao {
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
        ArticleDao ad = new ArticleDao(ds);
        Article article = new Article(1,"test", Article.NameSpace.MAIN,Article.PageType.STANDARD);
        ad.save(article);
        Article saved = ad.get(1);
        assert (article.getId()==saved.getId());
        assert (article.getTitle().equals(saved.getTitle()));
        assert (article.getNs().equals(saved.getNs()));
        assert (article.getType().equals(saved.getType()));
        List<Article> articles = ad.query("test");
        assert (articles.size()==1);
        assert (articles.get(0).getId()==1);
        assert (articles.get(0).getTitle().equals(article.getTitle()));
        assert (articles.get(0).getNs().equals(article.getNs()));
        assert (articles.get(0).getType().equals(article.getType()));
        articles = ad.query(Article.NameSpace.MAIN);
        assert (articles.size()==1);
        assert (articles.get(0).getId()==1);
        assert (articles.get(0).getTitle().equals(article.getTitle()));
        assert (articles.get(0).getNs().equals(article.getNs()));
        assert (articles.get(0).getType().equals(article.getType()));
        articles = ad.query("test",Article.NameSpace.MAIN);
        assert (articles.size()==1);
        assert (articles.get(0).getId()==1);
        assert (articles.get(0).getTitle().equals(article.getTitle()));
        assert (articles.get(0).getNs().equals(article.getNs()));
        assert (articles.get(0).getType().equals(article.getType()));
        articles = ad.query("wrong");
        assert (articles==null);

    }
}
