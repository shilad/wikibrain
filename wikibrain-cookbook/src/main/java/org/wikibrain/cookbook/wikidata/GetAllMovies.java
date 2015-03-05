package org.wikibrain.cookbook.wikidata;


import au.com.bytecode.opencsv.CSVWriter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.jooq.tables.UniversalPage;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataEntity;
import org.wikibrain.wikidata.WikidataStatement;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Created by toby on 3/2/15.
 */
public class GetAllMovies {

    public static void main(String args[]) throws FileNotFoundException, IOException, ConfigurationException, DaoException{
        Env env = new EnvBuilder().build();
        Configurator conf = env.getConfigurator();
        File top500Movies = new File("top5000movies.csv");
        CSVWriter csvWriter = new CSVWriter(new FileWriter(top500Movies), ',');
        WikidataDao wdDao = conf.get(WikidataDao.class);
        UniversalPageDao upDao = conf.get(UniversalPageDao.class);
        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        Map<Integer, String> idNameMap = new HashMap<Integer, String>();
        Map<Integer, String> wikidataIdImdbIdMap = new HashMap<Integer, String>();
        Set<String> imdbidsList = new HashSet<String>();
        File file = new File("imdbids.txt");

        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        while ((line = br.readLine()) != null) {
            imdbidsList.add("tt" + line);
            // process the line.
        }
        br.close();

        File movieList = new File("instance_of_movie.txt");
        BufferedReader brList = new BufferedReader(new FileReader(movieList));
        String movieLine, json = "";
        while ((movieLine = brList.readLine()) != null) {
            json = json + movieLine;
        }
        brList.close();
        JSONObject jsonObject = new JSONObject(json);
        JSONArray jsonArray = jsonObject.getJSONArray("items");

        for(int i = 0; i < jsonArray.length(); i ++){
            if(i % 100 == 0){
                System.out.println("DONE WITH " + i + " LINES OUT OF " + jsonArray.length());
            }
            Integer wikidataItemId = jsonArray.getInt(i);
            WikidataEntity wikidataItem = wdDao.getItem(wikidataItemId);

            List<WikidataStatement> statementList = wikidataItem.getStatements();
            for(WikidataStatement statement : statementList){
                if (statement.getProperty().getId() == 345)
                    if(imdbidsList.contains(statement.getValue().getValue().toString())){
                        try {
                            idNameMap.put(wikidataItemId, upDao.getById(wikidataItemId).getBestEnglishTitle(lpDao, true).getCanonicalTitle());
                            wikidataIdImdbIdMap.put(wikidataItemId, statement.getValue().getValue().toString());
                            break;
                        }
                        catch (Exception e){
                            //do nothing
                            break;
                        }

                    }
            }
        }
        String[] row = new String[3];
        for(Map.Entry<Integer, String> entry : idNameMap.entrySet()){
            row[0] = entry.getKey().toString();
            row[1] = entry.getValue().toString();
            row[2] = wikidataIdImdbIdMap.get(entry.getKey());
            csvWriter.writeNext(row);
            csvWriter.flush();
        }
        csvWriter.close();







        }
}
