package org.wikibrain.spatial.cookbook.tflevaluate;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.google.gson.internal.LinkedTreeMap;
import org.jooq.util.derby.sys.Sys;
import org.wikibrain.core.lang.Language;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

/**
 * Created by toby on 6/26/14.
 */
public class CalculateKValue{
    public static void main(String[] args) throws Exception {

        List<Language> langList = new LinkedList<Language>();

        //langList.add(Language.getByLangCode("en"));

        //langList.add(Language.getByLangCode("ar"));
        langList.add(Language.getByLangCode("ca"));
        //langList.add(Language.getByLangCode("ceb"));
       // langList.add(Language.getByLangCode("cs"));
        langList.add(Language.getByLangCode("de"));
        langList.add(Language.getByLangCode("es"));
        //langList.add(Language.getByLangCode("fa"));
        langList.add(Language.getByLangCode("fi"));
        langList.add(Language.getByLangCode("fr"));
        langList.add(Language.getByLangCode("hu"));
        //langList.add(Language.getByLangCode("id"));
        langList.add(Language.getByLangCode("it"));
        //langList.add(Language.getByLangCode("ja"));
        //langList.add(Language.getByLangCode("ko"));
        langList.add(Language.getByLangCode("nl"));
        langList.add(Language.getByLangCode("no"));
        //langList.add(Language.getByLangCode("pl"));
        langList.add(Language.getByLangCode("pt"));
        //langList.add(Language.getByLangCode("ru"));
        langList.add(Language.getByLangCode("sv"));
        //langList.add(Language.getByLangCode("uk"));
       // langList.add(Language.getByLangCode("vi"));
        //langList.add(Language.getByLangCode("war"));
        //langList.add(Language.getByLangCode("zh"));


        Map<String, Double> languagePopulationMap = new HashMap<String, Double>();
        languagePopulationMap.put("en", 335148.0);
        languagePopulationMap.put("ar", 246000.0);
        languagePopulationMap.put("ca", 4079.0);
        languagePopulationMap.put("cs", 10619.0);
        languagePopulationMap.put("de", 78245.0);
        languagePopulationMap.put("es", 414170.0);
        languagePopulationMap.put("fi", 5392.0);
        languagePopulationMap.put("fr", 74980.0);
        languagePopulationMap.put("hu", 12606.0);
        languagePopulationMap.put("it", 63655.0);
        languagePopulationMap.put("ja", 122056.0);
        languagePopulationMap.put("ko", 77166.0);
        languagePopulationMap.put("nl", 21944.0);
        languagePopulationMap.put("no", 4741.0);
        languagePopulationMap.put("pl", 38663.0);
        languagePopulationMap.put("pt", 203349.0);
        languagePopulationMap.put("ru", 167332.0);
        languagePopulationMap.put("sv", 9197.0);
        languagePopulationMap.put("vi", 67778.0);
        languagePopulationMap.put("zh", 847808.0);



        Map<Double, Double> globalTotalSRMap = new HashMap<Double, Double>();
        Map<Double, Integer> globalCountMap = new HashMap<Double, Integer>();
        List<Double> globalDistList = new ArrayList<Double>();

        Map<Double, Double> globalTotalSRacrossLangMap = new LinkedTreeMap<Double, Double>();
        Map<Double, Integer> globalTotalCountMap = new HashMap<Double, Integer>();
        String outputFile = "/Users/toby/Dropbox/Tobler2/Data/Kvalue_population_normalized_western_euro.csv";
        CSVWriter writer = new CSVWriter(new FileWriter(outputFile), ',');

        for(Language langu : langList){


            String inputFile = "/Users/toby/Dropbox/Tobler2/Data/Basic TFL_bin/TFL-" + langu.getLangCode() + "_bin.csv";
            //String inputFile = "/Users/toby/Dropbox/Tobler2/Data/SESData/SES-en_dist_normalized.csv";


            Integer SRColumn = 7;
            Integer DistColumn = 5;
            CSVReader reader = new CSVReader(new FileReader(inputFile), ',');


            String[] temp;
            String[] header;
            Map<Double, Double> distTotalSRMap = new HashMap<Double, Double>();
            Map<Double, Double> distAverageSRMap = new HashMap<Double, Double>();
            Map<Double, Integer> distCountMap = new HashMap<Double, Integer>();

            header = reader.readNext();

            while((temp = reader.readNext()) != null){
                Double dist = Double.valueOf(temp[DistColumn]);
                Double SR = Double.valueOf(temp[SRColumn]);
                Double population = languagePopulationMap.get(langu.getLangCode());
                if(dist.intValue() ==  -1)
                    continue;

                if(distTotalSRMap.keySet().contains(dist)){
                    distCountMap.put(dist, distCountMap.get(dist) + 1);
                    distTotalSRMap.put(dist, distTotalSRMap.get(dist) + SR);
                    globalCountMap.put(dist, distCountMap.get(dist) + population.intValue());
                    globalTotalSRMap.put(dist, distTotalSRMap.get(dist) + SR * population);
                }
                else{
                    distCountMap.put(dist, 1);
                    distTotalSRMap.put(dist, SR);
                    globalCountMap.put(dist, population.intValue());
                    globalTotalSRMap.put(dist, population * SR);

                }

            }

            List<Double> distList = new ArrayList<Double>();


            for(Double d : distTotalSRMap.keySet()){
                distList.add(d);
            }
            Collections.sort(distList);
            boolean flag = false;

            for(Double d : distList){
                /*
                Double totalSRd1 = 0.0;
                Double totalCountd1 = 0.0;
                for(Double d1 : distList){
                    if(d1 >= d)
                        continue;
                    totalSRd1 += distTotalSRMap.get(d1);
                    totalCountd1 += distCountMap.get(d1);

                }
                if(totalCountd1 == 0)
                    continue;
                Double avgNear = totalSRd1 / totalCountd1;
                Double totalSRd2 = 0.0;
                Double totalCountd2 = 0.0;
                for(Double d2: distList){
                    if(d2 <= d)
                        continue;
                    totalSRd2 += distTotalSRMap.get(d2);
                    totalCountd2 += distCountMap.get(d2);

                }
                if(totalCountd2 == 0)
                    continue;
                Double avgFar = totalSRd2 / totalCountd2;
                int a = 0;
/*
                if(avgNear < avgFar){
                    System.out.printf("Fail %s at %f\n", langu.getEnLangName(), d.doubleValue());
                    flag = true;
                    break;
                }
                */


                Double avgSR = distTotalSRMap.get(d) / distCountMap.get(d);
                Double population = languagePopulationMap.get(langu.getLangCode());

                if(globalTotalSRacrossLangMap.containsKey(d)){
                    globalTotalSRacrossLangMap.put(d, globalTotalSRacrossLangMap.get(d) + avgSR * population);
                    globalTotalCountMap.put(d, globalTotalCountMap.get(d) + (int)population.doubleValue());
                }
                else{
                    globalTotalSRacrossLangMap.put(d, avgSR * population );
                    globalTotalCountMap.put(d, (int)population.doubleValue());
                }

                distAverageSRMap.put(d, avgSR);
                Double totalSRBeyond = 0.0;
                Double totalCountBeyond = 0.0;
                for(Double m : distList){
                    if(m > d){
                        totalSRBeyond += distTotalSRMap.get(m);
                        totalCountBeyond += distCountMap.get(m);
                    }
                }
                if(avgSR < (totalSRBeyond / totalCountBeyond) && flag == false){
                    //System.out.printf("The k value for %s is %.2f  \n", langu.getEnLangName(), d);
                    //flag = true;
                    //break;
                }




            }
            if(flag == false)
                System.out.printf("OK %s\n", langu.getEnLangName());

            flag = false;
        }

/*
            for(Double d : distList){


                Double avgSR = distTotalSRMap.get(d) / distCountMap.get(d);




                Double totalSRBeyond = 0.0;
                Double totalCountBeyond = 0.0;

                for(int i = 0 ; i < 10; i ++){
                    if(distTotalSRMap.containsKey(d + (i + 1) * 0.01)){
                        totalSRBeyond += distTotalSRMap.get(d + (i + 1) * 0.01);
                        totalCountBeyond += distCountMap.get(d + (i + 1) * 0.01);
                    }

                }

                if(avgSR < (totalSRBeyond / totalCountBeyond) && flag == false){
                    System.out.printf("The m value for %s is %.2f  \n", langu.getEnLangName(), d);
                    flag = true;
                    break;
                }

            }



        }
        */
        String[] Entries = new String[2];
        Entries[0] = "DIST";
        Entries[1] = "SR";
        writer.writeNext(Entries);

        for(Double d : globalTotalSRMap.keySet()){
            globalDistList.add(d);
        }
        Collections.sort(globalDistList);

        boolean flag = false;
        for(Double d : globalDistList){
            Double avgSR = globalTotalSRacrossLangMap.get(d) / globalTotalCountMap.get(d);
            Double totalSRBeyond = 0.0;
            Double totalCountBeyond = 0.0;
            for(Double m : globalDistList){
                if(m > d && globalTotalSRacrossLangMap.keySet().contains(m)){
                    totalSRBeyond += globalTotalSRacrossLangMap.get(m);
                    totalCountBeyond += globalTotalCountMap.get(m);
                }
            }
            if(avgSR < (totalSRBeyond / totalCountBeyond) && flag == false){
                //System.out.printf("The k value for %s is %.2f  \n", langu.getEnLangName(), d);
                flag = true;
                //break;
            }
            if(flag == true){
                System.out.printf("The global K value is %f\n", d.doubleValue());
                break;
            }

            //System.out.printf("%.2f  : %f \n", d, globalTotalSRMap.get(d) / globalCountMap.get(d));
            Entries[0] = d.toString();
            Entries[1] = String.format("%.2f", globalTotalSRMap.get(d) / globalCountMap.get(d));
           // writer.writeNext(Entries);
        }
        flag = false;
        for(Double d : globalDistList){

            Double avgSR = globalTotalSRacrossLangMap.get(d) / globalTotalCountMap.get(d);




            Double totalSRBeyond = 0.0;
            Double totalCountBeyond = 0.0;

            for(int i = 0 ; i < 10; i ++){
                if(globalTotalSRacrossLangMap.containsKey(d + (i + 1) * 50)){
                    totalSRBeyond += globalTotalSRacrossLangMap.get(d + (i + 1) * 50);
                    totalCountBeyond += globalTotalCountMap.get(d + (i + 1) * 50);
                }

            }

            if(avgSR < (totalSRBeyond / totalCountBeyond) && flag == false){
                System.out.printf("The Global m value for  is %.2f  \n",  d);
                flag = true;
                break;
            }

        }
        //writer.flush();


        for(Double d : globalTotalSRacrossLangMap.keySet()){
            Entries[0] = String.format("%.2f", d.doubleValue());
            Entries[1] = String.format("%.2f", globalTotalSRacrossLangMap.get(d).doubleValue() / globalTotalCountMap.get(d).doubleValue());
            writer.writeNext(Entries);
            writer.flush();
            //System.out.printf("%.2f km  - %.2f\n", d, globalTotalSRacrossLangMap.get(d).doubleValue() / globalTotalCountMap.get(d).doubleValue());

        }


    }










}
