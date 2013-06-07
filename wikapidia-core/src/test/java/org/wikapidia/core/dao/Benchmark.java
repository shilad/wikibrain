package org.wikapidia.core.dao;

import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.wikapidia.core.model.Article;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Random;

public class Benchmark {
    private int numArticles = 40000000;
    private int numLinks = 1000000;
    private int titleLength = 10;
    private int textMinLength = 30;
    private int textMaxLength = 2500;
    boolean shouldBuildDb = false;
    boolean shouldBuildArticleDb = false;

    @Test
    public void articleBenchmark() throws IOException, SQLException {
        BoneCPDataSource ds = new BoneCPDataSource();
        ds.setJdbcUrl("jdbc:h2:~/benchmark-db");
        ds.setUsername("sa");
        ds.setPassword("");
        ArticleDao ad = new ArticleDao(ds);

        if (shouldBuildDb) buildDb(ds);
        if (shouldBuildArticleDb) buildArticleDb(ad);

        long time = 0, start, stop;
        Random r = new Random();
        int j;
        for (int i=0; i<50; i++){
            j=r.nextInt(numArticles);
            start=System.currentTimeMillis();
            ad.get(j);
            stop=System.currentTimeMillis();
            System.out.println("time to get by id: "+(stop-start)+"ms");
        }

        start=System.currentTimeMillis();
        ad.query("aa%");
        stop=System.currentTimeMillis();
        System.out.println("time to query by title: "+((stop-start)/1000)+"s");

        start=System.currentTimeMillis();
        ad.query(Article.NameSpace.MAIN);
        stop=System.currentTimeMillis();
        System.out.println("time to query by namespace: "+((stop-start)/1000)+"s");

        start=System.currentTimeMillis();
        ad.query("aa%",Article.NameSpace.MAIN);
        stop=System.currentTimeMillis();
        System.out.println("time to query by title and namespace: "+((stop-start)/1000)+"s");

    }


    public void buildArticleDb(ArticleDao ad) throws IOException, SQLException {
        long time = 0, start, stop;
        for (int i=0; i<numArticles; i++){
            Article a = new Article(
                    i,
                    RandomHelper.string(titleLength),
                    RandomHelper.ns(),
                    RandomHelper.type(),
                    RandomHelper.string(textMinLength, textMaxLength)
            );
            start = System.currentTimeMillis();
            ad.save(a);
            stop = System.currentTimeMillis();
            time += stop-start;
            if (i%100000==0) System.out.println(i);
        }

        System.out.println("time to insert: "+(time/60000)+" m (" + ((time/1000)/(long)numArticles) + " record/s");
    }

    public void buildDb(BoneCPDataSource ds) throws SQLException, IOException {
        Connection conn = ds.getConnection();
        conn.createStatement().execute(
                FileUtils.readFileToString(new File("src/main/resources/schema.sql"))
        );
        conn.close();

    }
}

class RandomHelper{
    static Random random = new Random();
    static final String chars="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 ";

    public static String string(int textLength){
        String s="";
        for (int i=0; i<textLength; i++){
            s+=chars.charAt(random.nextInt(chars.length()));
        }
        return s;
    }

    public static String string(int min, int max) {
        String s = "";
        int length = random.nextInt(max);
        length += min;
        for (int i = 0; i < length; i++) {
            s += chars.charAt(random.nextInt(chars.length()));
        }
        return s;
    }

    public static Article.NameSpace ns(){
        return Article.NameSpace.values()[random.nextInt(Article.NameSpace.values().length)];
    }

    public static Article.PageType type(){
        return Article.PageType.values()[random.nextInt(Article.PageType.values().length)];
    }
}