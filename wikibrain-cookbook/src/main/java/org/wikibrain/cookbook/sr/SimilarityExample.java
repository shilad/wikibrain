package org.wikibrain.cookbook.sr;

import com.vividsolutions.jts.geom.Geometry;
import gnu.trove.map.TIntIntMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.lang3.StringUtils;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.lang.LocalString;
import org.wikibrain.core.model.*;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.sr.*;
import org.wikibrain.sr.utils.ExplanationFormatter;
import org.wikibrain.wikidata.WikidataDao;

import java.io.*;
import java.util.*;

/**
 * @author Shilad Sen
 */
public class SimilarityExample {
    public static Env env;
    public static Configurator conf;
    public static LocalPageDao lpDao;
    public static Language simple;
    public static SpatialDataDao sdDao;
    public static UniversalPageDao upDao;
    public static WikidataDao wDao;
    public static Map<Integer, Geometry> allGeometries;
    public static ExplanationFormatter formatter;
    public static ArrayList<Integer> topList;
    public static Map<Integer, String> getTitle;

    public static void main(String args[]) throws ConfigurationException, DaoException, IOException, WikiBrainException {
        // Initialize the WikiBrain environment and get the local page dao
        //Env env = new EnvBuilder().build();
        env= EnvBuilder.envFromArgs(args);
        conf = env.getConfigurator();
        lpDao = conf.get(LocalPageDao.class);
        simple = Language.getByLangCode("simple");
        sdDao = conf.get(SpatialDataDao.class);
        upDao= conf.get(UniversalPageDao.class);
        wDao= conf.get(WikidataDao.class);
        allGeometries = sdDao.getAllGeometriesInLayer("wikidata", "earth");
        formatter= new ExplanationFormatter(lpDao);
//        LocalIDToUniversalID.init(conf);
        topList= new ArrayList<Integer>();
        getTitle= new HashMap<Integer, String>();
        buildSet();


        String srMetric= "ESA"; // Retrieve the indicated sr metric for simple english

        MonolingualSRMetric sr = conf.get(
                MonolingualSRMetric.class, srMetric,
                "language", simple.getLangCode());

        //Similarity between strings
        String pairs[][] = new String[][] {
                { "Kitty", "Cat" },
                { "obama", "president" },
                { "tires", "car" },
                { "Java", "Computer" },
                { "Dog", "Computer" },
                { "Barack Obama", "Japan"},
                { "Obama", "Japan"},
                { "Dog", "New York City"},
                { "Montana", "United States of America"},
                { "Statue of Liberty", "New York City" }
        };

//        SRResult result;
//        for (int i = 0; i <topList.size()-1 ; i++) {
//            for (int j = i+1; j < topList.size() ; j++) {
//                UniversalPage page1= upDao.getById(topList.get(i));
//                UniversalPage page2= upDao.getById(topList.get(j));
//                int id1= page1.getLocalId(Language.SIMPLE);
//                int id2= page2.getLocalId(Language.SIMPLE);
//
//                    System.out.println(page1.getBestEnglishTitle(lpDao,true));
//
//                result=sr.similarity(page1.getLocalId(Language.SIMPLE),page2.getLocalId(Language.SIMPLE),false);
//                if(result.getScore()>.95){
//                    System.out.println(getTitle.get(topList.get(i))+" "+ getTitle.get(topList.get(j)));
//                }
//            }
//        }

        Map<Integer, String> map= new HashMap<Integer, String>();
        Scanner scanner= new Scanner(new File("PageHitListFullEnglish.txt"));
        while(scanner.hasNextLine()){
            StringTokenizer st= new StringTokenizer(scanner.nextLine(),"\t", false);
            if(st.hasMoreTokens()){
                int id= Integer.parseInt(st.nextToken());
                if(st.hasMoreTokens()){
                    map.put(id,st.nextToken());
                }
            }
        }

        scanner.close();

        PrintWriter pw= new PrintWriter(new FileWriter("IDsToTitles.txt"));
        for(Integer id:map.keySet()){
            pw.println(id+":"+map.get(id));
        }
        pw.close();


//        for (String pair[] : pairs) {
//                SRResult s = sr.similarity(pair[0], pair[1], true);
//                System.out.println(s.getScore() + ": '" + pair[0] + "', '" + pair[1] + "'");
//                for (Explanation e:s.getExplanations()){
//                    System.out.println(formatter.formatExplanation(e));
//                }
//            }

    }

    private static void buildSet() throws FileNotFoundException{
        Scanner s= new Scanner(new File("IDsToTitles.txt"));
        StringTokenizer st;
        while (s.hasNextLine()){
            st= new StringTokenizer(s.nextLine(),"\t", false);
            int id= Integer.parseInt(st.nextToken());
            String name= st.nextToken();
            getTitle.put(id,name);
        }
        s.close();
    }

}

