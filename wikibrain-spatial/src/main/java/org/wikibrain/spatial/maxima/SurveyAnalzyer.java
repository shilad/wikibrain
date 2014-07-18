package org.wikibrain.spatial.maxima;

import com.vividsolutions.jts.geom.Geometry;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalIDToUniversalID;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.spatial.core.dao.SpatialDataDao;
import org.wikibrain.sr.MonolingualSRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.utils.ExplanationFormatter;
import org.wikibrain.wikidata.WikidataDao;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by harpa003 on 7/14/14.
 */
public class SurveyAnalzyer {
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

    public static void main(String[] args)throws IOException, ConfigurationException, DaoException{
        env= EnvBuilder.envFromArgs(args);
        conf = env.getConfigurator();
        lpDao = conf.get(LocalPageDao.class);
        upDao= conf.get(UniversalPageDao.class);
        String srMetric= "ensemble"; // Retrieve the indicated sr metric for simple english


        Map<QuestionAgg,Set<Question>> map= new HashMap<QuestionAgg, Set<Question>>();
        List<QuestionAgg> listagg= new ArrayList<QuestionAgg>();
//        Scanner scanner= new Scanner(new File("BeccasTest.txt"));
//        while(scanner.hasNextLine()){
//            StringTokenizer st= new StringTokenizer(scanner.nextLine(),"\t", false);
//            QuestionAgg q= new QuestionAgg();
//            q.name1=st.nextToken();
//            q.id1=tryInt(st.nextToken());
//            q.name2=st.nextToken();
//            q.id2=tryInt(st.nextToken());
//            q.km=Double.parseDouble(st.nextToken());
//            st.nextToken(); //log km
//            q.graphDist= tryIntGraphDist(st.nextToken());
//            q.compSR=Double.parseDouble(st.nextToken());
//            q.expectedFF=tryInt(st.nextToken());
//            q.expectedFU=tryInt(st.nextToken());
//            q.expectedUU=tryInt(st.nextToken());
//            q.realFF=tryInt(st.nextToken());
//            q.realFU=tryInt(st.nextToken());
//            q.realUU=tryInt(st.nextToken());
//            q.avgFF=Double.parseDouble(st.nextToken());
//            q.avgFUsr=Double.parseDouble(st.nextToken());
//            q.avgUUsr=Double.parseDouble(st.nextToken());
//            q.diff=Double.parseDouble(st.nextToken());
//            listagg.add(q);
//        }
//        scanner.close();
//
        Scanner scanner= new Scanner(new File("questions.enhanced.tsv"));
        List<Question> list= new ArrayList<Question>();
        scanner.nextLine();
        while(scanner.hasNextLine()){
            StringTokenizer st= new StringTokenizer(scanner.nextLine(),"\t", false);
            Question q= new Question();
            q.userid=tryInt(st.nextToken());
            st.nextToken(); // turker id
            st.nextToken(); //page numb
            q.questionNumb=tryInt(st.nextToken());
            q.name1=st.nextToken();
            q.name2=st.nextToken();
            q.sr=tryInt(st.nextToken());
            q.fam1=tryInt(st.nextToken());
            q.fam2=tryInt(st.nextToken());
            st.nextToken(); //val 1
            st.nextToken(); // val 2
            q.km=Double.parseDouble(st.nextToken());
            q.graphDist=tryIntGraphDist(st.nextToken());
            q.compSR=Double.parseDouble(st.nextToken());
            q.id1=tryInt(st.nextToken());
            q.id2=tryInt(st.nextToken());
            q.scale1=tryInt(st.nextToken());
            q.scale2=tryInt(st.nextToken());
            q.pop1=tryInt(st.nextToken());
            q.pop2=tryInt(st.nextToken());
            q.setKnown1(st.nextToken());
            q.setKnown2(st.nextToken());
            list.add(q);

        }
        scanner.close();

//        catigories(list);
//        fu(list);
//        PercentWrongPerConcept(list);
//        Baln(list);
//        random(list);
//        numb(list);

        MonolingualSRMetric sr = conf.get(
                MonolingualSRMetric.class, srMetric,
                "language", Language.EN.getLangCode());

        //Similarity between strings
        List<QuestionAgg> aggList= createAgg(list, map);


        SRResult result;
        for(QuestionAgg a:aggList){
            Question quest=map.get(a).iterator().next();
            UniversalPage one= upDao.getById(quest.id1);
            UniversalPage two= upDao.getById(quest.id2);
            result=sr.similarity(one.getLocalId(Language.EN),two.getLocalId(Language.EN),false);
            a.compSR=result.getScore();
        }
        System.out.println("list size: "+list.size());
        avgs(map);
        writeOut(aggList);
    }

    private static void writeOut(List<QuestionAgg> aggList) {
        PrintWriter p=null;
        try{
            p= new PrintWriter(new FileWriter("output.txt"));
            p.println("name1"+"\t"+"name2"+"\t"+"expectedFF\texpectedFU\texpectedUU\trealFF\trealFU\trealUU\tAvg FF\tAvg FU\t Avg UU\tDifference\tComputer/Human FF Diff\tComputer/Human FU Diff\tComputer/Human UU Diff\tDiff Comp/Human");
        } catch (Exception e){
            System.out.println("no file");}
        int count=0;
        for(QuestionAgg q:aggList){
            if(q.realFF>5&&q.realUU>5){
                count++;
                p.println(q+"\t"+smallest(q));
            }
        }
        System.out.println(count);
        p.close();
    }

    private static String smallest(QuestionAgg quest){
        if(min(quest.diffCompHumFF,quest.diffCompHumFU,quest.diffCompHumUU)==quest.diffCompHumFF){
            return "FF";
        }
        if(min(quest.diffCompHumFF,quest.diffCompHumFU,quest.diffCompHumUU)==quest.diffCompHumFU){
            return "FU";
        }
        if(min(quest.diffCompHumFF,quest.diffCompHumFU,quest.diffCompHumUU)==quest.diffCompHumUU){
            return "UU";
        }
        return null;
    }

    private static double min(double one, double two, double three){
        double smallest;
        smallest=Math.min(one,two);
        smallest=Math.min(smallest,three);
        return smallest;
    }

    private static void avgs(Map<QuestionAgg, Set<Question>> map){
        for (QuestionAgg a: map.keySet()){
            int[] numbers = new int[3];
            double[] averages = new double[3];
            double overallavg=0.0;
            double count=0.0;
            for(Question quest: map.get(a)){
                if(quest.sr!=-100){
                    if(quest.fam1>1 && quest.fam1<=5 && quest.fam2>1 && quest.fam2<=5) {
                        if(quest.sr!=-1) {
                            overallavg += quest.sr;
                            count++;
                        }
                        int index;
                        if (quest.getType()==quest.FF) {
                            index = quest.FF;
                        } else if (quest.getType()==quest.UU) {
                            index = quest.UU;
                        } else if(quest.getType()==quest.FU){
                            index = quest.FU;
                        } else {
                            continue;
                        }
                        averages[index] = (numbers[index] * averages[index] + quest.sr) / (numbers[index] + 1.0);
                        numbers[index]++;
                    }
                }
            }
            a.avgFFsr=averages[0];
            a.avgFUsr=averages[1];
            a.avgUUsr=averages[2];
            a.overallAvg=overallavg/count;
            a.convertAvg();
            a.diffFFUU=Math.abs(a.avgFFsr-a.avgUUsr);
            a.diffCompHumFF=Math.abs(a.compSR-a.avgFFsr);
            a.diffCompHumFU=Math.abs(a.compSR-a.avgFUsr);
            a.diffCompHumUU=Math.abs(a.compSR-a.avgUUsr);
            a.diffCompHum=Math.abs(a.compSR-a.overallAvg);
        }
    }



    private static List<QuestionAgg> createAgg(List<Question> list, Map<QuestionAgg, Set<Question>> map) {
        List<QuestionAgg> agglist= new ArrayList<QuestionAgg>();
        for(Question q:list){
            QuestionAgg addTo= checkList(agglist,q);
            Set<Question> set= new HashSet<Question>();
            if(addTo==null){
                QuestionAgg a= new QuestionAgg(q);
                agglist.add(a);
                set.add(q);
                map.put(a,set);
            } else{
                set=map.get(addTo);
                addTo.increaseExpected(q);
                addTo.increaseReal(q);
                set.add(q);
                map.put(addTo,set);
            }
        }
        System.out.println(agglist.size());
        return agglist;
    }

    private static QuestionAgg checkList(List<QuestionAgg> agglist, Question q) {
        for(QuestionAgg a:agglist){
            if((a.id1==q.id1 && a.id2==q.id2) || (a.id1==q.id2 && a.id2==q.id1)){
                return a;
            }
        }
        return null;
    }

    private static void numb(List<Question> list) {
        Map<Question,Integer> set= new HashMap<Question, Integer>();
        for(Question q: list) {
            if (q.sr != -100) {
                if (!set.containsKey(q)) {
                    if (realKK(q)) {
                        q.numbOfKKTimesAsked++;
                    }
                    set.put(q, q.numbOfKKTimesAsked);
                } else {
                    int i = set.get(q);
                    if (realKK(q)) {
                        i++;
//                    } else if (realKU(q2)) {
//                        q2.numbOfKUTimesAsked++;
//                    } else if (realUU(q2)) {
//                        q2.numbOfUUTimesAsked++;
//                    }
                        set.put(q, i);
                    }
                }
            }
        }
        int count=0;
        for(Question q:set.keySet()){
            if(set.get(q)>8){
//                System.out.println(q.name1+"\t"+q.name2+"\tKKTime: "+q.numbOfKKTimesAsked+"\tUUTime: "+q.numbOfUUTimesAsked);
                count++;
            }
        }
        System.out.println(count);
        Map<Question,Integer> set2= new HashMap<Question, Integer>();
        for(Question q: list) {
            if (q.sr != -100) {
                if (!set2.containsKey(q)) {
                    if (realUU(q)) {
                        q.numbOfKKTimesAsked++;
                    }
                    set2.put(q, q.numbOfKKTimesAsked);
                } else {
                    int i = set2.get(q);
                    if (realUU(q)) {
                        i++;
//                    } else if (realKU(q2)) {
//                        q2.numbOfKUTimesAsked++;
//                    } else if (realUU(q2)) {
//                        q2.numbOfUUTimesAsked++;
//                    }
                        set2.put(q, i);
                    }
                }
            }
        }
        int count2=0;
        for(Question q:set2.keySet()){
            if(set2.get(q)>8){
//                System.out.println(q.name1+"\t"+q.name2+"\tKKTime: "+q.numbOfKKTimesAsked+"\tUUTime: "+q.numbOfUUTimesAsked);
                count2++;
            }
        }
        System.out.println(count2);
        int finalCount=0;
        for(Question q: set.keySet()){
            if(set2.keySet().contains(q)){
                if(set.get(q)>8 && set2.get(q)>8){
                    finalCount++;
                }
            }
        }
        System.out.println(finalCount);

    }

    private static void random(List<Question> list) {
        Set<Integer> usedSet= new HashSet<Integer>();
        for(Question q: list){
                usedSet.add(q.id1);
                usedSet.add(q.id2);
        }
        Set<Integer> set= new HashSet<Integer>();
        double wrong=0.0;
        double total=467.0*10000;


        for (int j = 0; j <10000 ; j++) {
            set.clear();
            for (int i = 0; i < 45; i++) {
                set.add((int) (Math.random()*2151));
            }
            for (int i = 0; i < 467; i++) {
                int get = (int) (Math.random() * 2151);
                if (!set.contains(get)) {
                    wrong++;
                }
            }

        } System.out.println("Random Wrong "+wrong / total);
    }

    private static void Baln(List<Question> list) {
        double balanced=0.0;
        double total=0.0;
        Set<Question> used= new HashSet<Question>();
        for(Question q:list){
            if(expectedKK(q)&& !realKK(q)){
                total++;
                for(Question q2: list){
                    double preBal=balanced;
                    if(q.equals(q2) && !expectedKK(q2) && realKK(q2) && !used.contains(q2)){
                        balanced++;
                        used.add(q2);
                    }
                    if(preBal!=balanced){
                        break;
                    }
                }
            }
        }
        System.out.println("Balanced for KK: "+balanced/total);

        double balanced2=0.0;
        double total2=0.0;
        Set<Question> used2= new HashSet<Question>();
        for(Question q:list){
            if(expectedUU(q)&& !realUU(q)){
                total2++;
                for(Question q2: list){
                    double preBal=balanced2;
                    if(q.equals(q2) && !expectedUU(q2) && realUU(q2) && !used2.contains(q2)){
                        balanced2++;
                        used2.add(q2);
                    }
                    if(preBal!=balanced2){
                        break;
                    }
                }
            }
        }
        System.out.println("Balanced for UU: "+balanced2/total2);
    }

    private static void PercentWrongPerConcept(List<Question> list) {
        Map<Integer,Set<Integer>> map = new HashMap<Integer, Set<Integer>>();
        double wrong=0.0;
        double total=0.0;
        for(Question q: list){
            if(!map.containsKey(q.userid) && q.sr!=-100){
                Set<Integer> set= new HashSet<Integer>();
                if(q.known1){
                    total++;
                    set.add(q.id1);
                    if(!(q.fam1>3)){
                        wrong++;
                    }
                }
                if(q.known2){
                    total++;
                    set.add(q.id2);
                    if(!(q.fam2>=3)){
                        wrong++;
                    }
                }
                map.put(q.userid,set);
            }
            else if(q.sr!=-100){
                Set<Integer> set= map.get(q.userid);
                if(q.known1 && !set.contains(q.id1)){
                    total++;
                    set.add(q.id1);
                    if(!(q.fam1>=3)){
                        wrong++;
                    }
                }
                if(q.known2 && !set.contains(q.id2)){
                    total++;
                    set.add(q.id2);
                    if(!(q.fam2>=3)){
                        wrong++;
                    }
                }
                map.put(q.userid,set);
            }
        }
        System.out.println("Percent thought a concept was known and it wasn't "+wrong/total);

        Map<Integer,Set<Integer>> map2 = new HashMap<Integer, Set<Integer>>();
        double wrong2=0.0;
        double total2=0.0;
        for(Question q: list){
            if(!map2.containsKey(q.userid)){
                Set<Integer> set= new HashSet<Integer>();
                if(!q.known1){
                    total2++;
                    set.add(q.id1);
                    if((q.fam1>=3)||q.sr==-1){
                        wrong2++;
                    }
                }
                if(!q.known2){
                    total2++;
                    set.add(q.id2);
                    if((q.fam2>=3)||q.sr==-1){
                        wrong2++;
                    }
                }
                map2.put(q.userid,set);
            }
            else{
                Set<Integer> set= map2.get(q.userid);
                if(!q.known1 && !set.contains(q.id1)){
                    total2++;
                    set.add(q.id1);
                    if((q.fam1>=3)||q.sr==-1){
                        wrong2++;
                    }
                }
                if(!q.known2 && !set.contains(q.id2)){
                    total2++;
                    set.add(q.id2);
                    if((q.fam1>=3)||q.sr==-1){
                        wrong2++;
                    }
                }
                map2.put(q.userid,set);
            }
        }
        System.out.println("Percent thought a concept was unknown and it wasn't "+wrong2/total2);
    }

    private static void fu(List<Question> list) {
        System.out.println("KK Percent wrong: "+ kkPercentWrong(list));
        System.out.println("KU Percent wrong: "+ kuPercentWrong(list));
        System.out.println("UU Percent wrong: "+ uuPercentWrong(list));
    }

    private static String kuPercentWrong(List<Question> list) {
        double wrong=0.0;
        double total=0.0;
        for(Question q:list){
            if(expectedKU(q)){
                total++;
                if(!realKU(q)){
                    wrong++;
                }
            }
        }
        return ""+wrong/total;
    }
    private static String uuPercentWrong(List<Question> list) {
        double wrong=0.0;
        double total=0.0;
        for(Question q:list){
            if(expectedUU(q)){
                total++;
                if(!realUU(q)){
                    wrong++;
                }
            }
        }
        return ""+wrong/total;
    }

    private static String kkPercentWrong(List<Question> list) {
        double wrong=0.0;
        double total=0.0;
        for(Question q:list){
            if(expectedKK(q)){
                total++;
                if(!realKK(q)){
                    wrong++;
                }
            }
        }
        return ""+wrong/total;
    }
    private static boolean expectedKU(Question q){
        return (q.known1 && !q.known2) || (!q.known1 && q.known2);
    }
    private static boolean expectedUU(Question q){
        return !q.known1 && !q.known2;
    }
    private static boolean expectedKK(Question q){
        return q.known1 && q.known2;
    }
    private static boolean realKU(Question q){
        return ((q.fam1>3 && q.fam2<=3) || (q.fam1<=3 && q.fam2>3)) && q.sr!=-1 ;
    }
    private static boolean realUU(Question q){
        return q.fam1<3 && q.fam2<3 && q.sr!=-1 ;
    }
    private static boolean realKK(Question q){
        return q.fam1>3 && q.fam2>3 && q.sr!=-1 ;
    }

    private static void catigories(List<Question> list) {
        System.out.println("Thor KK: "+thormKK(list));
        System.out.println("Real KK: " + realKK(list)+" excluding 3");
        System.out.println("Real KK: " + realKK2(list)+" including 3");
        System.out.println("Thor UU: "+theoUU(list));
        System.out.println("Real UU: "+realUU(list)+" excluding 3");
        System.out.println("Real UU: "+realUU2(list)+" including 3");
        System.out.println("Thor KU: "+theoKU(list));
        System.out.println("Real KU: "+realKU(list));
    }

    private static int realKU(List<Question> list) {
        int count=0;
        for(Question q:list){

            if((q.fam1<=3 && q.fam2 >3) || (q.fam1>3 && q.fam2<=3)){
                count++;
            }
        }
        return count;
    }

    private static int theoKU(List<Question> list) {
        int count=0;
        for(Question q:list){
            if((!q.known1 && q.known2) || (q.known1 && !q.known2)){
                count++;
            }
        }
        return count;
    }

    private static int realUU(List<Question> list) {
        int count=0;
        for(Question q:list){
            if(q.fam1<3  && q.sr!=-1 && q.fam2 <3 && q.sr!=-100){
                count++;
            }
        }
        return count;
    }
    private static int realUU2(List<Question> list) {
        int count=0;
        for(Question q:list){
            if(q.fam1<=3  && q.sr!=-1 && q.fam2 <=3 && q.sr!=-100){
                count++;
            }
        }
        return count;
    }

    private static int theoUU(List<Question> list) {
        int count=0;
        for(Question q:list){
            if(!q.known1 && !q.known2 && q.sr!=-100){
                count++;
            }
        }
        return count;
    }

    private static int realKK(List<Question> list) {
        int count=0;
        for(Question q:list){
            if(q.fam1>3 && q.fam2 >3 && q.sr!=-1 && q.sr!=-100 ){
                count++;
            }
        }
        return count;
    }

    private static int thormKK(List<Question> list) {
        int count=0;
        for(Question q:list){
            if(q.known1 && q.known2 && q.sr!=-100){
                count++;
            }
        }
        return count;
    }
    private static int realKK2(List<Question> list) {
        int count=0;
        for(Question q:list){
            if(q.fam1>=3 && q.fam2 >=3 && q.sr!=-1 && q.sr!=-100 ){
                count++;
            }
        }
        return count;
    }



    private static int tryInt(String s) {
       if(s.equalsIgnoreCase("null")){
           return -100;
       }
       double d= Double.parseDouble(s);
       return (int) d;
    }



    private static int tryIntGraphDist(String s) {
        if(s.equalsIgnoreCase("infinity")){
            return 15;
        } double d= Double.parseDouble(s);
        return (int) d;
    }


}
