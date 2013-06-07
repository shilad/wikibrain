//package org.wikapidia.core.dao;
//
//
//import com.jolbox.bonecp.BoneCPDataSource;
//import org.apache.commons.io.FileUtils;
//import org.junit.Test;
//import org.wikapidia.core.model.*;
//
//import java.io.File;
//import java.io.IOException;
//import java.sql.Connection;
//import java.sql.SQLException;
//import java.util.List;
//
//public class TestDao {
//    @Test
//    public void testArticle() throws ClassNotFoundException, IOException, SQLException {
//        Class.forName("org.h2.Driver");
//        File tmpDir = File.createTempFile("wikapidia-h2", null);
//        tmpDir.delete();
//        tmpDir.deleteOnExit();
//        tmpDir.mkdirs();
//
//        BoneCPDataSource ds = new BoneCPDataSource();
//        ds.setJdbcUrl("jdbc:h2:"+new File(tmpDir,"db").getAbsolutePath());
//        ds.setUsername("sa");
//        ds.setPassword("");
//
//        Connection conn = ds.getConnection();
//        conn.createStatement().execute(
//                FileUtils.readFileToString(new File("src/main/resources/schema.sql"))
//        );
//        conn.close();
//
//
//        //Article
//            ArticleDao ad = new ArticleDao(ds);
//            Article article = new Article(1,"test", Article.NameSpace.MAIN,
//                    Article.PageType.STANDARD, "This is the text!");
//            ad.save(article);
//
//            Article savedArticle = ad.get(1);
//            assert (savedArticle != null);
//            assert (article.getId()==savedArticle.getId());
//            assert (article.getTitle().equals(savedArticle.getTitle()));
//            assert (article.getNs().equals(savedArticle.getNs()));
//            assert (article.getType().equals(savedArticle.getType()));
//            assert (article.getText().equals(savedArticle.getText()));
//
//            List<Article> articles = ad.query("test");
//            assert (articles != null);
//            assert (articles.size()==1);
//            assert (articles.get(0).getId()==1);
//            assert (articles.get(0).getTitle().equals(article.getTitle()));
//            assert (articles.get(0).getNs().equals(article.getNs()));
//            assert (articles.get(0).getType().equals(article.getType()));
//            assert (articles.get(0).getText().equals(article.getText()));
//
//            articles = ad.query(Article.NameSpace.MAIN);
//            assert (articles.size()==1);
//            assert (articles.get(0).getId()==1);
//            assert (articles.get(0).getTitle().equals(article.getTitle()));
//            assert (articles.get(0).getNs().equals(article.getNs()));
//            assert (articles.get(0).getType().equals(article.getType()));
//            assert (articles.get(0).getText().equals(savedArticle.getText()));
//
//            articles = ad.query("test",Article.NameSpace.MAIN);
//            assert (articles.size()==1);
//            assert (articles.get(0).getId()==1);
//            assert (articles.get(0).getTitle().equals(article.getTitle()));
//            assert (articles.get(0).getNs().equals(article.getNs()));
//            assert (articles.get(0).getType().equals(article.getType()));
//            assert (articles.get(0).getText().equals(savedArticle.getText()));
//
//            articles = ad.query("wrong");
//            assert (articles.size()==0);
//
//
//        //Link
//            LinkDao ld = new LinkDao(ds);
//            Link link = new Link("zelda.com", 1, false);
//            ld.save(link);
//
//            Link savedLink = ld.get(1);
//            assert (savedLink != null);
//            assert (link.getText().equals(savedLink.getText()));
//            assert (link.getId()==savedLink.getId());
//            assert (link.isSubsec()==savedLink.isSubsec());
//
//            List<Link> links = ld.query("zelda.com");
//            assert (links != null);
//            assert (links.size()==1);
//            assert (links.get(0).getText().equals(link.getText()));
//            assert (links.get(0).getId()==1);
//            assert (links.get(0).isSubsec()==false);
//
//            links = ld.query("ganondorf.com");
//            assert (links.size()==0);
//    }
//}
