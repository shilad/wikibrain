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
public class chooseLabels {

    public static void main(String[] args) throws Exception {
        //set up environment
        Env env = EnvBuilder.envFromArgs(args);
        Configurator conf = env.getConfigurator();
        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        LocalLinkDao llDao = conf.get(LocalLinkDao.class);
        NameSpace n = NameSpace.ARTICLE;
        Language simple = Language.SIMPLE;

        //set up readers and writers
        FileReader titleRdr = new FileReader("/Users/research/wikibrain/wikibrain-cookbook/results/kmeans-results/ordered_article_titles.txt");
        BufferedReader reader = new BufferedReader(titleRdr);

        BufferedWriter writer = new BufferedWriter( new FileWriter(new File("wikibrain-cookbook/results/kmeans-results/popularity_filtered_labels.txt")));

        //initialize line
        String line = reader.readLine();

        //initialize array list of titleAndPop objects to sort later
        ArrayList<titleAndPop> articleList = new ArrayList<titleAndPop>();

        //read until the end of the file
        while(line != null){
            Title t = new Title(line, simple);
            //look up the title of the article
            LocalPage l = lpDao.getByTitle(t, n);
            //get the article's popularity
            double pop = llDao.getPageRank(simple, l.getLocalId());
            //create titleAndPop object to store it
            titleAndPop pg = new titleAndPop(line, pop);

            //add object to array list
            articleList.add(pg);
            //update line
            line = reader.readLine();
        }

        //sort articles by popularity
        Collections.sort(articleList);
        Collections.reverse(articleList);

       for(int i = 0; i < 500; i++){
            writer.append(articleList.get(i).getTitle() + "\n");
        }


        reader.close();
        writer.close();
    }
}
