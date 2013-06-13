package org.wikapidia.core.dao;

import com.jolbox.bonecp.BoneCPDataSource;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.wikapidia.core.dao.sql.LocalArticleSqlDao;
import org.wikapidia.core.lang.LanguageInfo;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.PageType;
import org.wikapidia.core.model.Title;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Benchmark {
    private int numArticles = 1000000;
    private int numLinks = 1000000;
    private int titleLength = 10;
    boolean shouldBuildDb = true;
    LanguageInfo lang;

    @Test
    public void articleBenchmark() throws DaoException {
        lang = LanguageInfo.getByLangCode("en");
        BoneCPDataSource ds = new BoneCPDataSource();
        System.out.println("Establishing new data source");
        ds.setJdbcUrl("jdbc:h2:~/benchmark-small-db");
        ds.setUsername("sa");
        ds.setPassword("");
        LocalArticleSqlDao ad = new LocalArticleSqlDao(ds);
        System.out.println("Data source established.");
        List<LocalArticle> list = null;
        if (shouldBuildDb){list = buildDb(ad);}

        long time = 0, start, stop;
        Random r = new Random();
        int j;
        int numQueries = 0;
        while(time < 1000){
            j=r.nextInt(numArticles);
            start=System.currentTimeMillis();
            ad.getById(lang.getLanguage(), j);
            stop=System.currentTimeMillis();
            time += stop - start;
            numQueries++;
        }
        System.out.println("" + numQueries + " Get_Queries completed in 1 s");


        if (list!=null){
            int id;
            time =0;
            start = System.currentTimeMillis();
            for (LocalArticle article : list){

                id = ad.getIdByTitle(article.getTitle().getCanonicalTitle(),
                                     article.getLanguage(),
                                     article.getPageType());
                assert(id == article.getLocalId());
            }
            stop = System.currentTimeMillis();
            System.out.println(list.size()+" ids from titles in "+(stop-start)+" ms");
        }

/*        start=System.currentTimeMillis();
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
        System.out.println("time to query by title and namespace: "+((stop-start)/1000)+"s");*/

    }

    public List<LocalArticle> buildDb(LocalArticleDao dao) throws DaoException {
        dao.beginLoad();
        long time = 0, start, stop;
        RandomHelper rh = new RandomHelper();
        String bigString = rh.generateBigString();
        ArrayList<LocalArticle> list = new ArrayList<LocalArticle>();
        for (int i=0; i<numArticles; i++){
            LocalArticle a = new LocalArticle(lang.getLanguage(),
                    i,
                    new Title(rh.string(titleLength),lang));
            start = System.currentTimeMillis();
            dao.save(a);
            stop = System.currentTimeMillis();
            time += stop-start;
            if(i%100000 == 0) {
                System.out.println("" + (time/1000) + " s ;" + i + " insertions completed");
                list.add(a);
            }
        }

        System.out.println("time to insert: "+(time/60000)+" m (" + (numArticles/(time/1000)) + " record/s)");
        start=System.currentTimeMillis();
        dao.endLoad();
        stop= System.currentTimeMillis();
        System.out.println("endLoad took "+(stop-start)+" ms");
        return list;

    }
}

class RandomHelper{
    Random random;
    String chars="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 ";
    private int textMinLength = 30;
    private int textMaxLength = 4000;

    public String generateBigString() {
        String s = "";
        for (int i = 0; i < textMaxLength; i++) {
            s += chars.charAt(random.nextInt(chars.length()));
        }
        System.out.println("The big string is generated.");
        return s;
    }

    RandomHelper(){
        random = new Random();
    }

    public String string(int textLength){
        String s="";
        for (int i=0; i<textLength; i++){
            s+=chars.charAt(random.nextInt(chars.length()));
        }
        return s;
    }

    public String getSmallString(String bigString) {
        int length = random.nextInt(textMaxLength - textMinLength);
        length += textMinLength;
        String smallString = bigString.substring(0, length);
        return smallString;
    }

    public PageType ns(){
        return PageType.values()[random.nextInt(PageType.values().length)];
    }

}