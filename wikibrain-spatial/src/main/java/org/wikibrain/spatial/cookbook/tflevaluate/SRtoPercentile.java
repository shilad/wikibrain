package org.wikibrain.spatial.cookbook.tflevaluate;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

/**
 * Created by toby on 6/20/14.
 */
public class SRToPercentile {
    public static void main(String[] args) throws Exception {


        String inputFile = "/Users/toby/Dropbox/Tobler2/Data/SESData/SES-en.csv";
        String outputFile = "/Users/toby/Dropbox/Tobler2/Data/SESData/SES-en_new.csv";
        Integer SRColumn = 8;
        CSVReader reader = new CSVReader(new FileReader(inputFile), ',');
        CSVWriter writer = new CSVWriter(new FileWriter(outputFile), ',');

        String[] temp;
        String[] header;
        List<String[]> entryList = new ArrayList<String[]>();
        List<Double> SRList = new ArrayList<Double>();
        Map<Double, Double> SRMap = new HashMap<Double, Double>();
        Map<Double, Integer> SRCounter = new HashMap<Double, Integer>();
        header = reader.readNext();
        while((temp = reader.readNext()) != null){

            SRList.add(Double.valueOf(temp[SRColumn]));
            entryList.add(temp);
        }
        Collections.sort(SRList);
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

        for(Double d : SRMap.keySet()){
            SRMap.put(d, Double.valueOf(SRMap.get(d).doubleValue() / SRCounter.get(d).doubleValue()));
        }

        String[] headerEntries = new String[header.length + 1];
        for(int i = 0; i < header.length; i++){
            headerEntries[i] = header[i];
        }
        headerEntries[header.length] = "Percentile";
        writer.writeNext(headerEntries);
        writer.flush();


        for(String[] e : entryList){

            String[] rowEntries = new String[e.length + 1];
            for(int i = 0; i < e.length; i++){
                rowEntries[i] = e[i];
            }
            rowEntries[e.length] = String.format("%.2f", SRMap.get(Double.valueOf(e[SRColumn]))/SRList.size());
            writer.writeNext(rowEntries);
            writer.flush();
        }

        reader.close();
        writer.close();
    }



}
