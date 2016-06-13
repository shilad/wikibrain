package org.wikibrain.cookbook.kmeans;

import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.dao.LocalLinkDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.model.Title;

import java.util.ArrayList;
import java.util.Collections;

import java.io.*;

/**
 * Created by research on 6/6/16.
 */
public class getTopTitles{

        public static void main (String[] args) throws Exception{
        //set up environment
        Env env = EnvBuilder.envFromArgs(args);
        Configurator conf = env.getConfigurator();
        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        LocalLinkDao llDao = conf.get(LocalLinkDao.class);


        //set up readers and writers
        FileReader reader1 = new FileReader("/Users/research/wikibrain/wikibrain-cookbook/results/kmeans-results/popularity_filtered_labels.txt");
        BufferedReader popularityReader = new BufferedReader(reader1);

            ArrayList<String> popularArticles = new ArrayList<String>();

            String l = popularityReader.readLine();
            while(l != null){
                popularArticles.add(l);
                l = popularityReader.readLine();
            }


        FileReader reader2 = new FileReader("/Users/research/wikibrain/wikibrain-cookbook/results/kmeans-results/ordered_article_titles.txt");
        BufferedReader orderedReader = new BufferedReader(reader2);

            BufferedWriter writer = new BufferedWriter(new FileWriter(new File("wikibrain-cookbook/results/kmeans-results/most_popular_articles_ordered.txt")));

            String l2 = orderedReader.readLine();
            while(l2 != null){
                if(popularArticles.contains(l2)){
                    writer.append(l2 + "\n");
                }
                else{
                    writer.append(" \n");
                }
                l2 = orderedReader.readLine();
            }

        orderedReader.close();
        popularityReader.close();
        writer.close();

        }
}
