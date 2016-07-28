package org.wikibrain.cookbook.sr;

import java.io.*;
import java.util.*;

/**
 * Created by Qisheng on 7/15/16.
 */
public class readFile {

    public static void main(String[] args) throws Exception{
        BufferedReader test = new BufferedReader(new FileReader("/Users/research/wikibrain/dat/wikitovec/new_pop_full.tsv"));
        String[] name = new String[250792];
        int ct = 0;
        for (String line = test.readLine(); line != null; line = test.readLine()){
            String[] full = line.split("\t");
            String n = full[0];
            name[ct] = n;
            ct ++;
        }

        System.out.println(name.length);
        Set<String> N = new HashSet<String>(Arrays.asList(name));
        System.out.println(N.size());

        BufferedReader newPop = new BufferedReader(new FileReader("/Users/research/wikibrain/dat/wikitovec/newPopData/outSamplePop.txt"));
        BufferedReader oldPop = new BufferedReader(new FileReader("/Users/research/wikibrain/dat/wikitovec/article_pageview_full.tsv"));
//        String[] name = new String[250793];
//        int ct = 0;
//        for (String line = oldPop.readLine(); line != null; line = oldPop.readLine()){
//            String[] full = line.split("\t");
//            String n = full[0];
//            name[ct] = n;
//            ct ++;
//        }
//
//        System.out.println(name.length);
//
//        BufferedOutputStream newOut = new BufferedOutputStream(new FileOutputStream("/Users/research/wikibrain/dat/wikitovec/newPop1.txt"));
//
//        Set<String> N = new HashSet<String>(Arrays.asList(name));
//
//        System.out.println(N.size());
//
//        for (String line = newPop.readLine(); line != null; line = newPop.readLine()){
//            String[] lst = line.split("\t");
//            String n = lst[0];
//            if (N.contains(n)){
//                double pop = Double.parseDouble(lst[1]);
//                newOut.write((n + "\t").getBytes());
//                newOut.write((Double.toString(pop)+"\n").getBytes());
//            }
//        }
//
//        newOut.flush();
//---------------------------------------------------
//        ct = 0;
//        for (String line = pvFile.readLine(); line != null; line = pvFile.readLine()){
//            String[] arr = line.split("\t");
//            double pv = Double.parseDouble(arr[1]);
//            lst2[ct] = pv;
//            ct ++;
//        }
//
//        double[] pop = new double[838215];
//        for (int i = 0; i < 838215; i++){
//            pop[i] = lst1[i]*lst2[i];
//        }
//
//        BufferedOutputStream popout = new BufferedOutputStream(new FileOutputStream("/Users/research/wikibrain/dat/wikitovec/fullPop1.txt"));
//        ct = 0;
//        BufferedReader name = new BufferedReader(new FileReader("/Users/research/wikibrain/dat/wikitovec/fullpagerank.txt"));
//        for (String line = name.readLine(); line != null; line = name.readLine()){
//            String[] arr = line.split("\t");
//            String n = arr[0];
//            popout.write((n + "\t").getBytes());
//            popout.write((Double.toString(pop[ct])+"\n").getBytes());
//        }
//
//        popout.flush();
    }
}
