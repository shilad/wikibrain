//package org.wikapidia.core.dao;
//
//import com.jolbox.bonecp.BoneCPConfig;
//import com.jolbox.bonecp.BoneCPDataSource;
//import org.apache.commons.io.FileUtils;
//import org.junit.Test;
//import org.wikapidia.core.lang.LanguageInfo;
//import org.wikapidia.core.model.Article;
//
//import java.io.File;
//import java.io.IOException;
//import java.sql.Connection;
//import java.sql.SQLException;
//import java.util.Random;
//
//public class Benchmark {
//    private int numArticles = 500000;
//    private int numLinks = 1000000;
//    private int titleLength = 10;
//    boolean shouldBuildDb = true;
//    boolean shouldBuildArticleDb = true;
//
//    @Test
//    public void articleBenchmark() throws IOException, SQLException {
//        BoneCPDataSource ds = new BoneCPDataSource();
//        System.out.println("Establishing new data source");
//        ds.setJdbcUrl("jdbc:h2:~/benchmark-db");
//        ds.setUsername("sa");
//        ds.setPassword("");
//        ArticleDao ad = new ArticleDao(ds);
//        System.out.println("Data source established.");
//
//        if (shouldBuildDb){buildDb(ds);}
//        if (shouldBuildArticleDb){buildArticleDb(ad);}
//
//        long time = 0, start, stop;
//        Random r = new Random();
//        int j;
//        int numQueries = 0;
//        while(time < 1000){
//            j=r.nextInt(numArticles);
//            start=System.currentTimeMillis();
//            ad.get(j);
//            stop=System.currentTimeMillis();
//            time += stop - start;
//            numQueries++;
//        }
//        System.out.println("" + numQueries + " Get_Queries completed in 1 s");
//
//        start=System.currentTimeMillis();
//        ad.query("aa%");
//        stop=System.currentTimeMillis();
//        System.out.println("time to query by title: "+((stop-start)/1000)+"s");
//
//        start=System.currentTimeMillis();
//        ad.query(Article.NameSpace.MAIN);
//        stop=System.currentTimeMillis();
//        System.out.println("time to query by namespace: "+((stop-start)/1000)+"s");
//
//        start=System.currentTimeMillis();
//        ad.query("aa%",Article.NameSpace.MAIN);
//        stop=System.currentTimeMillis();
//        System.out.println("time to query by title and namespace: "+((stop-start)/1000)+"s");
//
//    }
//
//
//    public void buildArticleDb(ArticleDao ad) throws IOException, SQLException {
//        long time = 0, start, stop;
//        RandomHelper rh = new RandomHelper();
//        String bigString = rh.generateBigString();
//
//        for (int i=0; i<numArticles; i++){
//            Article a = new Article(i,rh.string(titleLength), Article.NameSpace.MAIN,rh.type(), rh.getSmallString(bigString), LanguageInfo.getByLangCode("en"));
//            start = System.currentTimeMillis();
//            ad.save(a);
//            stop = System.currentTimeMillis();
//            time += stop-start;
//            if(i%100000 == 0) {System.out.println("" + (time/1000) + " s ;" + i + " insertions completed");}
//        }
//
//        System.out.println("time to insert: "+(time/60000)+" m (" + ((time/1000)/(long)numArticles) + " record/s");
//    }
//
//    public void buildDb(BoneCPDataSource ds) throws SQLException, IOException {
//        System.out.println("Beginning Build DB");
//        Connection conn = ds.getConnection();
//        System.out.println("Got connection");
//        conn.createStatement().execute(
//                FileUtils.readFileToString(new File("src/main/resources/schema.sql"))
//        );
//        System.out.println("Connection established.");
//        conn.close();
//    }
//}
//
//class RandomHelper{
//    Random random;
//    String chars="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 ";
//    private int textMinLength = 30;
//    private int textMaxLength = 4000;
//
//    public String generateBigString() {
//        String s = "";
//        for (int i = 0; i < textMaxLength; i++) {
//            s += chars.charAt(random.nextInt(chars.length()));
//        }
//        System.out.println("The big string is generated.");
//        return s;
//    }
//
//    RandomHelper(){
//        random = new Random();
//    }
//
//    public String string(int textLength){
//        String s="";
//        for (int i=0; i<textLength; i++){
//            s+=chars.charAt(random.nextInt(chars.length()));
//        }
//        return s;
//    }
//
//    public String getSmallString(String bigString) {
//        int length = random.nextInt(textMaxLength - textMinLength);
//        length += textMinLength;
//        String smallString = bigString.substring(0, length);
//        return smallString;
//    }
//
//    public Article.NameSpace ns(){
//        return Article.NameSpace.values()[random.nextInt(Article.NameSpace.values().length)];
//    }
//
//    public Article.PageType type(){
//        return Article.PageType.values()[random.nextInt(Article.PageType.values().length)];
//    }
//}