package org.wikibrain.spatial.cookbook.tflevaluate;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import org.wikibrain.core.lang.Language;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

/**
 * Created by toby on 6/20/14.
 */
public class SRToPercentile {
    public static void main(String[] args) throws Exception {

        List<Language> langList = new LinkedList<Language>();

        langList.add(Language.getByLangCode("en"));
        /*
        langList.add(Language.getByLangCode("ar"));
        langList.add(Language.getByLangCode("ca"));
        langList.add(Language.getByLangCode("ceb"));
        langList.add(Language.getByLangCode("cs"));
        langList.add(Language.getByLangCode("de"));
        langList.add(Language.getByLangCode("es"));
        langList.add(Language.getByLangCode("fa"));
        langList.add(Language.getByLangCode("fi"));
        langList.add(Language.getByLangCode("fr"));
        langList.add(Language.getByLangCode("hu"));
        langList.add(Language.getByLangCode("id"));
        langList.add(Language.getByLangCode("it"));
        langList.add(Language.getByLangCode("ja"));
        langList.add(Language.getByLangCode("ko"));
        langList.add(Language.getByLangCode("nl"));
        langList.add(Language.getByLangCode("no"));
        langList.add(Language.getByLangCode("pl"));
        langList.add(Language.getByLangCode("pt"));
        langList.add(Language.getByLangCode("ru"));
        langList.add(Language.getByLangCode("sv"));
        langList.add(Language.getByLangCode("uk"));
        langList.add(Language.getByLangCode("vi"));
        langList.add(Language.getByLangCode("war"));
        langList.add(Language.getByLangCode("zh"));
        */

        for(Language lang : langList){
            /*
            String inputFile = "/Users/toby/Dropbox/Tobler2/Data/Ranking/rank-" + lang.getLangCode() + ".csv";
            String outputFile = "/Users/toby/Dropbox/Tobler2/Data/Ranking/rank-" + lang.getLangCode() + "_new.csv";
            */
            String inputFile = "/Users/toby/Dropbox/Tobler2/Data/Basic TFL/TFL-en.csv";
            String outputFile = "/Users/toby/Dropbox/Tobler2/Data/Basic TFL/TFL-en_test.csv";
            Integer SRColumn = 5;
            Integer DistColumn = 4;
            CSVReader reader = new CSVReader(new FileReader(inputFile), ',');
            CSVWriter writer = new CSVWriter(new FileWriter(outputFile), ',');

            String[] temp;
            String[] header;
            List<String[]> entryList = new ArrayList<String[]>();
            List<Double> SRList = new ArrayList<Double>();
            List<Double> DistList = new ArrayList<Double>();
            Map<Double, Double> SRMap = new HashMap<Double, Double>();
            Map<Double, Integer> SRCounter = new HashMap<Double, Integer>();
            Map<Double, Double> DistMap = new HashMap<Double, Double>();
            Map<Double, Integer> DistCounter = new HashMap<Double, Integer>();
            header = reader.readNext();
            while((temp = reader.readNext()) != null){

                SRList.add(Double.valueOf(temp[SRColumn]));
                DistList.add(Double.valueOf(temp[DistColumn]));
                entryList.add(temp);
            }
            Collections.sort(SRList);
            Collections.sort(DistList);
            int counter = 0;
            for(Double d : SRList){
                if(SRMap.containsKey(d)){
                    SRMap.put(d, counter + SRMap.get(d));
                    SRCounter.put(d, SRCounter.get(d) + 1);
                }
                else{
                    SRMap.put(d, (double)counter);
                    SRCounter.put(d, 1);
                }
                counter ++;
                if(counter % 10000 == 0){
                    counter = counter;
                }
            }
            counter = 0;
            for(Double d : DistList){
                if(DistMap.containsKey(d)){
                    DistMap.put(d, counter + DistMap.get(d));
                    DistCounter.put(d, DistCounter.get(d) + 1);
                }
                else{
                    DistMap.put(d, (double)counter);
                    DistCounter.put(d, 1);
                }
                counter ++;
                if(counter % 10000 == 0){
                    counter = counter;
                }
            }

            for(Double d : SRMap.keySet()){
                SRMap.put(d, Double.valueOf(SRMap.get(d).doubleValue() / SRCounter.get(d).doubleValue()));
            }

            for(Double d : DistMap.keySet()){
                DistMap.put(d, Double.valueOf(DistMap.get(d).doubleValue() / DistCounter.get(d).doubleValue()));
            }

            String[] headerEntries = new String[header.length + 2];
            for(int i = 0; i < header.length; i++){
                headerEntries[i] = header[i];
            }
            headerEntries[header.length ] = "SR_Percentile";
            headerEntries[header.length + 1] = "Dist_Percentile";
            writer.writeNext(headerEntries);
            writer.flush();


            for(String[] e : entryList){

                String[] rowEntries = new String[e.length + 2];
                for(int i = 0; i < e.length; i++){
                    rowEntries[i] = e[i];
                }
                rowEntries[e.length ] = String.format("%.2f", SRMap.get(Double.valueOf(e[SRColumn]))/SRList.size());
                rowEntries[e.length + 1] = String.format("%.2f", DistMap.get(Double.valueOf(e[DistColumn]))/DistList.size());
                writer.writeNext(rowEntries);
                writer.flush();
            }

            reader.close();
            writer.close();
        }
    }



}
