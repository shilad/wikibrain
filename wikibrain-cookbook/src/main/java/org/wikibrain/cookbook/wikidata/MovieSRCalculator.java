package org.wikibrain.cookbook.wikidata;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.jooq.tables.UniversalPage;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.sr.SRMetric;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by toby on 3/4/15.
 */
public class MovieSRCalculator {
    public static void main(String args[]) throws FileNotFoundException, IOException, ConfigurationException, DaoException {
        Env env = new EnvBuilder().build();
        Configurator conf = env.getConfigurator();
        UniversalPageDao upDao = conf.get(UniversalPageDao.class);
        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        CSVReader csvReader = new CSVReader(new FileReader(new File("top5000movies.csv")), ',');
        CSVWriter csvWriter = new CSVWriter(new FileWriter(new File("movieSR.csv")), ',');
        SRMetric sr = conf.get(
                SRMetric.class, "ensemble",
                "language", "en");

        List<String[]> movieRowList = csvReader.readAll();
        List<Integer> movieLocalIdList = new ArrayList<Integer>();
        Map<Integer, String> localIdTitleMap = new HashMap<Integer, String>();
        Map<Integer, String> localIdImdbMap = new HashMap<Integer, String>();
        int[] array = new int[5000];

        int i = 0;
        int count = 0;
        for(String[] entry: movieRowList){
            if(count++ > 20)
                break;
            Integer uId = Integer.parseInt(entry[0]);
            Integer localId = upDao.getLocalId(Language.EN, uId);
            localIdTitleMap.put(localId, entry[1]);
            localIdImdbMap.put(localId, entry[2]);
            array[i++] = localId;
        }
        String[] row = new String[5];
        System.out.println("CALCULATING SR");
        double[][] srResult = sr.cosimilarity(array);
        System.out.println("FINISHED CALCULATING SR");

        for(int x = 0; x < i; x ++){
            for(int y = 0; y < i; y ++){
                if(x % 10 == 0){
                    System.out.println("FINISHED " + x + " OUT OF " + i);
                }
                row[0] = localIdImdbMap.get(array[x]);
                row[1] = localIdTitleMap.get(array[x]);
                row[2] = localIdImdbMap.get(array[y]);
                row[3] = localIdTitleMap.get(array[y]);
                row[4] = String.valueOf(srResult[x][y]);
                csvWriter.writeNext(row);
                csvWriter.flush();
            }
        }
        csvReader.close();
        csvWriter.close();
    }
}
