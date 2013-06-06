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

    @Test
    public void articleBenchmark() throws ClassNotFoundException, IOException, SQLException {
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

        ArticleDao ad = new ArticleDao(ds);

        //build
        long time = 0, start, stop;
        RandomHelper rh = new RandomHelper();
        for (int i=0; i<numArticles; i++){
            Article a = new Article(i,rh.string(),rh.ns(),rh.type());
            start = System.currentTimeMillis();
            ad.save(a);
            stop = System.currentTimeMillis();
            time += stop-start;
            if(i%100000 == 0) {System.out.println(i);}
        }

        System.out.println("time to insert: "+(time/60000)+"m");

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
        ad.query("a%");
        stop=System.currentTimeMillis();
        System.out.println("time to query by title: "+((stop-start)/1000)+"s");

        start=System.currentTimeMillis();
        ad.query(Article.NameSpace.MAIN);
        stop=System.currentTimeMillis();
        System.out.println("time to query by namespace: "+((stop-start)/1000)+"s");

        start=System.currentTimeMillis();
        ad.query("a%",Article.NameSpace.MAIN);
        stop=System.currentTimeMillis();
        System.out.println("time to query by title and namespace: "+((stop-start)/1000)+"s");

    }
}

class RandomHelper{
    Random random;
    String chars="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 ";


    RandomHelper(){
        random = new Random();
    }

    public String string(){
        String s="";
        for (int i=0; i<10; i++){
            s+=chars.charAt(random.nextInt(chars.length()));
        }
        return s;
    }

    public Article.NameSpace ns(){
        return Article.NameSpace.values()[random.nextInt(Article.NameSpace.values().length)];
    }

    public Article.PageType type(){
        return Article.PageType.values()[random.nextInt(Article.PageType.values().length)];
    }
}