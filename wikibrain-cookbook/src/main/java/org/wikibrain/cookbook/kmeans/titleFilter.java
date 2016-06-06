package org.wikibrain.cookbook.kmeans;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


/**
 * Created by Anja Beth Swoap on 6/6/16.
 */
public class titleFilter {
    public static void main(String[] args) throws IOException {
        FileReader titleRdr = new FileReader("/Users/research/wikibrain/wikibrain-cookbook/results/kmeans-results/ordered_article_titles.txt");
        BufferedReader bufferedReader = new BufferedReader(titleRdr);

        BufferedWriter writer = new BufferedWriter( new FileWriter(new File("wikibrain-cookbook/results/kmeans-results/filtered_labels.txt")));
        //get rid of title line
       bufferedReader.readLine();

        for(int i = 0; i < 8000; i++){
            String line = bufferedReader.readLine();
            if(i % 25 == 0){
                writer.append(line + "\n");
            }
            else{
                writer.append(" \n");
            }

        }

        bufferedReader.close();
        writer.close();
    }

}
