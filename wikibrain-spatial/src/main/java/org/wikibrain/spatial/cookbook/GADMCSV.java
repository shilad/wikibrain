package org.wikibrain.spatial.cookbook;

import au.com.bytecode.opencsv.CSVWriter;

import com.google.gson.JsonParser;
import org.apache.lucene.analysis.ja.util.CSVUtil;
import org.geotools.data.shapefile.dbf.DbaseFileReader;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.Title;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.wikidata.JsonUtils;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataFilter;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

/**
 * Created by aaron on 6/12/14.
 */
public class GADMCSV {

    public static void main(String[] args) throws Exception {
        Env env = new EnvBuilder().build();
        Configurator c = env.getConfigurator();
        LocalPageDao lpDao = c.get(LocalPageDao.class);
        UniversalPageDao upDao = c.get(UniversalPageDao.class);
        CSVWriter writer = new CSVWriter(new FileWriter("gadm.csv"), ',');
        try {
            writeToFile(0, lpDao, upDao, writer);
            writeToFile(1, lpDao, upDao, writer);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            writer.close();
        }


    }


    private static void writeToFile(int level, LocalPageDao lpDao, UniversalPageDao upDao, CSVWriter writer) throws Exception {

        FileChannel in = new FileInputStream(String.format("spatial_data/earth/gadm%d.dbf", level)).getChannel();
        DbaseFileReader reader = new DbaseFileReader(in, true, Charset.forName(StandardCharsets.ISO_8859_1.name()));

        while (reader.hasNext()) {
            String name = (String) (reader.readEntry()[0]);
            if (!name.equals("")) {
                //int entityId = getQNumber(name);
                LocalPage entityPage = lpDao.getByTitle(new Title(name, Language.SIMPLE), NameSpace.ARTICLE);
                UniversalPage uPage = upDao.getByLocalPage(entityPage, 0);
                int univId = uPage.getUnivId();

                String[] entry = String.format("%d#%s#%d", univId, name, level).split("#");
                writer.writeNext(entry);
            }
        }
        writer.flush();
        reader.close();
        in.close();

    }

    private static int getQNumber(String name) throws Exception {
        String urlString = String.format("http://www.wikidata.org/w/api.php?action=wbgetentities&titles=%s&sites=enwiki&props=&format=json", URLEncoder.encode(name, StandardCharsets.UTF_8.name()));
        BufferedReader in = new BufferedReader(new InputStreamReader(new URL(urlString).openStream()));
        JSONObject json = (JSONObject) JSONValue.parse(in.readLine());
        String entityId = (String) ((JSONObject) json.get("entities")).keySet().toArray()[0];
        return entityId.equals("-1") ? -1 : Integer.parseInt(entityId.substring(1));
    }


}
