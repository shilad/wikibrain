package org.wikibrain.atlasify;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.JsonParser;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.LocalLinkDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.Title;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.phrases.PhraseAnalyzer;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;
import sun.net.www.content.text.plain;
import java.io.IOException;
import java.net.MalformedURLException;
import org.wikibrain.lucene.LuceneSearcher;
import org.wikibrain.phrases.LucenePhraseAnalyzer;

import java.awt.Color;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.*;

import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.commons.codec.binary.Base64;

// The Java class will be hosted at the URI path "/helloworld"
@Path("/wikibrain")
public class AtlasifyResource {

    private static class AtlasifyQuery{
        private String keyword;
        private String[] featureIdList;
        private String[] featureNameList;

        public AtlasifyQuery(){

        }

        public AtlasifyQuery(String keyword, String[] featureIdList, String[] featureNameList){
            this.keyword = keyword;
            this.featureIdList = featureIdList;
            this.featureNameList = featureNameList;
        }

        public AtlasifyQuery(String keyword, List<String> featureIdList, List<String> featureNameList){
            this.keyword = keyword;
            this.featureIdList = featureIdList.toArray(new String[featureIdList.size()]);
            this.featureNameList = featureNameList.toArray(new String[featureNameList.size()]);
        }

        public String getKeyword(){
            return keyword;
        }

        public String[] getFeatureIdList(){
            return featureIdList;
        }

        public String[] getFeatureNameList(){
            return featureNameList;
        }

    }

    private static SRMetric sr = null;
    private static PhraseAnalyzer pa = null;
    private static LocalPageDao lpDao = null;
    private static Language lang = Language.getByLangCode("en");
    private static LocalPageAutocompleteSqlDao lpaDao = null;
    private static LocalLinkDao llDao = null;
    private static AtlasifyLogger atlasifyLogger;

    private static void wikibrainSRinit(){

        try {
            System.out.println("START LOADING WIKIBRAIN");
            Env env = new EnvBuilder().build();
            Configurator conf = env.getConfigurator();
            lpDao = conf.get(LocalPageDao.class);
            lpaDao = conf.get(LocalPageAutocompleteSqlDao.class);
			llDao = conf.get(LocalLinkDao.class);
            atlasifyLogger = new AtlasifyLogger("./log/AtlasifyLogin.csv", "./log/AtlasifyQuery.csv");

            pa = conf.get(PhraseAnalyzer.class, "anchortext");
            System.out.println("FINISHED LOADING WIKIBRAIN");


            //sr = conf.get(
            //        SRMetric.class, "ensemble",
            //        "language", "simple");


        } catch (Exception e) {
            System.out.println("Exception when initializing WikiBrain: "+e.getMessage());

        }

    }

    private static LocalId wikibrainPhaseResolution(String title) throws Exception {
        Language language = lang;
        LinkedHashMap<LocalId, Float> resolution = pa.resolve(language, title, 1);
        for (LocalId p : resolution.keySet()) {
            return p;
        }
        throw new Exception("failed to resolve");
    }

    private static Map<LocalId, Double> accessNorthwesternAPI(LocalId id) throws Exception {
        Language language = lang;
        String url = "http://downey-n2.cs.northwestern.edu:8080/wikisr/sr/sID/" + id.getId() + "/langID/" + language.getId();
        InputStream inputStream = new URL(url).openStream();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
        StringBuilder stringBuilder = new StringBuilder();
        int currentChar;
        while ((currentChar = bufferedReader.read()) != -1) {
            stringBuilder.append((char) currentChar);
        }

        JSONObject jsonObject = new JSONObject(stringBuilder.toString());
        JSONArray jsonArray = jsonObject.getJSONArray("result");
        Map<LocalId, Double> result = new HashMap<LocalId, Double>();
        int length = jsonArray.length();

        for (int i = 0; i < length; i++) {
            JSONObject pageSRPair = jsonArray.getJSONObject(i);
            LocalId page = new LocalId(language, pageSRPair.getInt("wikiPageId"));
            Double sr = new Double(pageSRPair.getDouble("srMeasure"));
            result.put(page, sr);
        }

        return result;
    }
    @GET
    @Path("/helloworld")
    @Produces("text/plain")
    public Response helloWorld() throws Exception{
	return Response.ok("hello world").header("Access-Control-Allow-Origin", "*").build();
    }

    // The Java method will process HTTP GET requests
    @GET
    // The Java method will produce content identified by the MIME Media
    // type "text/plain"
    @Path("/SR/keyword={keyword}&feature=[{input}]")
    @Consumes("text/plain")
    @Produces("text/plain")
    public Response getClichedMessage(@PathParam("keyword") String keyword, @PathParam("input") String data) throws  DaoException{
        if(pa == null){
            wikibrainSRinit();
        }
        String[] features = data.split(",");
        Map<String, String> srMap = new HashMap<String, String>();
        for(int i = 0; i < features.length; i++){
            srMap.put(features[i].toString(), getColorStringFromSR(sr.similarity(keyword, features[i].toString(), false).getScore()));
        }
        return Response.ok(new JSONObject(srMap).toString()).header("Access-Control-Allow-Origin", "*").build();
    }
/*
    @POST
    @Path("/send")
    @Produces("text/plain")
    public Response nullResponse () {
        return Response.ok("success").build();
    }
*/

    static private boolean useNorthWesternAPI = true;

    @POST
    @Path("/send")
    @Consumes("application/json")
    @Produces("text/plain")

    public Response consumeJSON (AtlasifyQuery query) {
        if(pa == null){
            wikibrainSRinit();
        }
        String[] featureIdList = query.getFeatureIdList();
        String[] featureNameList = query.getFeatureNameList();
        Map<String, String> srMap = new HashMap<String, String>();
        System.out.println("Receive featureId size of " + featureIdList.length + " and featureName size of " + featureNameList.length);

        if (useNorthWesternAPI) {
            LocalId queryID = new LocalId(lang, 0);
            try{
                queryID = wikibrainPhaseResolution(query.getKeyword());
            }
            catch (Exception e){
                System.out.println("Failed to resolve keyword " + query.getKeyword());
                return Response.ok(new JSONObject(srMap).toString()).build();
            }
            // LocalId queryID = new LocalId(Language.EN, 19908980);
            try {
                Map<LocalId, Double> srValues = accessNorthwesternAPI(queryID);

                for (int i = 0; i < featureIdList.length; i++) {
                    LocalId featureID = new LocalId(lang, 0);

                    try{
                        featureID = new LocalId(lang, Integer.parseInt(featureIdList[i]));
                    }
                    catch (Exception e){
                        System.out.println("Failed to resolve " + featureNameList[i]);
                        continue;
                        //do nothing
                    }

                    try{
                        String color = getColorStringFromSR(srValues.get(featureID));
                        srMap.put(featureNameList[i].toString(), color);
                        System.out.println("SR Between " + lpDao.getById(queryID).getTitle().getCanonicalTitle() + " and " + lpDao.getById(featureID).getTitle().getCanonicalTitle() + " is " + srValues.get(featureID));
                    }
                    catch (Exception e){
                        //put white for anything not present in the SR map
                        System.out.println("NO SR Between " + lpDao.getById(queryID).getTitle().getCanonicalTitle() + " and " + lpDao.getById(featureID).getTitle().getCanonicalTitle());
                        srMap.put(featureNameList[i].toString(), "#ffffff");
                        continue;
                        //do nothing
                    }
                }
            }
            catch (Exception e) {
                System.out.println("Error when connecting to Northwestern Server");
                // do nothing

            }
        } else {

            for (int i = 0; i < featureNameList.length; i++) {
                String color = "#ffffff";
                try {

                    color = getColorStringFromSR(sr.similarity(query.getKeyword(), featureNameList[i].toString(), false).getScore());
                } catch (Exception e) {
                    //do nothing
                }

                srMap.put(featureNameList[i].toString(), color);
            }
        }

        return Response.ok(new JSONObject(srMap).toString()).build();
    }

    private String getColorStringFromSR(double SR){
        if(SR < 0.2873)
            return "#ffffff";
        if(SR < 0.3651)
            return "#f7fcf5";
        if(SR < 0.4095)
            return "#e5f5e0";
        if(SR < 0.4654)
            return "#c7e9c0";
        if(SR < 0.5072)
            return "#a1d99b";
        if(SR < 0.5670)
            return "#74c476";
        if(SR < 0.6137)
            return "#41ab5d";
        if(SR < 0.6809)
            return "#238b45";
        if(SR < 0.7345)
            return "#006d2c";
        if(SR < 0.7942)
            return "#00441b";
        return "#002000";
    }

    @POST
    @Path("logLogin")
    @Consumes("application/json")
    public Response processLogLogin(AtlasifyLogger.logLogin query) throws Exception{

        atlasifyLogger.LoginLogger(query, "");
        System.out.println("LOGIN LOGGED " + query.toString());
        return Response.ok("received").build();

    }

    @POST
    @Path("logQuery")
    @Consumes("application/json")
    public Response processLogQuery(AtlasifyLogger.logQuery query) throws Exception{

        atlasifyLogger.QueryLogger(query, "");
        System.out.println("QUERY LOGGED " + query.toString());
        return Response.ok("received").build();
    }

    @POST
    @Path("/autocomplete")
    @Consumes("application/json")
    @Produces("text/plain")

    public Response autocompleteSearch(AtlasifyQuery query) throws Exception {
        if (pa == null) {
            wikibrainSRinit();
        }

        Language language = Language.EN;
        System.out.println("Received Auto Complete Query " + query.getKeyword());
        Map<String, String> autocompleteMap = new HashMap<String, String>();
        try {
            int i = 0;
            /* Phrase Analyzer
            LinkedHashMap<LocalId, Float> resolution = pa.resolve(language, query.getKeyword(), 100);
            for (LocalId p : resolution.keySet()) {
                org.wikibrain.core.model.LocalPage page = lpDao.getById(p);
                autocompleteMap.put(i + "", page.getTitle().getCanonicalTitle());
                i++;
            } */

            /* Page Titles that being/contain search term
            Title title = new Title(query.getKeyword(), language);
            List<LocalPage> similarPages = lpaDao.getBySimilarTitle(title, NameSpace.ARTICLE, llDao);

            for (LocalPage p : similarPages) {
                autocompleteMap.put(i + "", p.getTitle().getCanonicalTitle());
                i++;
            } */

            /* Bing */
            String bingAccountKey = "Y+KqEsFSCzEzNB85dTXJXnWc7U4cSUduZsUJ3pKrQfs";
            byte[] bingAccountKeyBytes = Base64.encodeBase64((bingAccountKey + ":" + bingAccountKey).getBytes());
            String bingAccountKeyEncoded = new String(bingAccountKeyBytes);

            String bingQuery = query.getKeyword();
            URL bingQueryurl = new URL("https://api.datamarket.azure.com/Bing/SearchWeb/v1/Web?Query=%27"+java.net.URLEncoder.encode(bingQuery, "UTF-8")+"%20site%3Aen.wikipedia.org%27&$top=50&$format=json");

            HttpURLConnection connection = (HttpURLConnection)bingQueryurl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Basic " + bingAccountKeyEncoded);
            connection.setRequestProperty("Accept", "application/json");
            BufferedReader br = new BufferedReader(new InputStreamReader((connection.getInputStream())));

            String output;
            StringBuilder sb = new StringBuilder();
            while ((output = br.readLine()) != null) {
                sb.append(output);
            }

            JSONObject bingResponse = new JSONObject(sb.toString());
            bingResponse = bingResponse.getJSONObject("d");
            JSONArray bingResponses = bingResponse.getJSONArray("results");
            JSONObject response;
            for (int j = 0; j < bingResponses.length() && i < 10; j++) {
                response = bingResponses.getJSONObject(j);
                URL url = new URL(response.getString("Url"));
                String path = url.getPath();
                String title = path.substring(path.lastIndexOf('/') + 1).replace('_', ' ');
                LocalPage page = new LocalPage(language, 0, "");
                for (LocalId p : pa.resolve(language, title, 1).keySet()) {
                    page = lpDao.getById(p);
                }
                if (page != null && !autocompleteMap.values().contains(page.getTitle().getCanonicalTitle())){
                    autocompleteMap.put(i + "", page.getTitle().getCanonicalTitle());
                    i++;
                }
            }

        } catch (Exception e) {
            autocompleteMap = new HashMap<String, String>();
        }
	System.out.println("Get Auto Complete Result" + new JSONObject(autocompleteMap).toString());
        return Response.ok(new JSONObject(autocompleteMap).toString()).build();
    }

    @GET
    // The Java method will produce content identified by the MIME Media
    // type "text/plain"
    @Path("/SR/Explanation/keyword={keyword}&feature={feature}")
    @Consumes("text/plain")
    @Produces("text/plain")
    public Response handleExplanation(@PathParam("keyword") String keyword, @PathParam("feature") String feature) throws  DaoException, MalformedURLException, IOException, Exception{
        System.out.println("Received query for explanation between " + keyword + " and " + feature);
        String keywordTitle;
        String featureTitle;
        try{
            keywordTitle = lpDao.getById(wikibrainPhaseResolution(keyword)).getTitle().getCanonicalTitle().replace(" ", "_");
            featureTitle = lpDao.getById(wikibrainPhaseResolution(feature)).getTitle().getCanonicalTitle().replace(" ", "_");
        }
        catch (Exception e){
            System.out.println("Failed to resolve " + keyword + " and " + feature);
            return Response.ok("").header("Access-Control-Allow-Origin", "*").build();
        }

        String url = "http://downey-n1.cs.northwestern.edu:3030/api?concept1=" + keywordTitle + "&concept2=" + featureTitle;

        InputStream inputStream = new URL(url).openStream();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
        StringBuilder stringBuilder = new StringBuilder();
        int currentChar;
        while ((currentChar = bufferedReader.read()) != -1) {
            stringBuilder.append((char) currentChar);
        }
        System.out.println("GOT REPLY\n" + stringBuilder.toString());
		/*
        JSONObject jsonObject = new JSONObject(stringBuilder.toString());
        JSONArray result = jsonObject.getJSONArray("result");
        System.out.println("GOT RESULT\n" + result.toString());
        String returnVal = "";
        for(int i = 0; i < result.length(); i ++){
            returnVal = returnVal.concat(result.getJSONObject(i).getString("title") + "\n\n");
            System.out.println("GOT TITLE\n" + result.getJSONObject(i).getString("title") );
            JSONArray resultForEach = result.getJSONObject(i).getJSONArray("explanations");
            for(int j = 0; j < resultForEach.length(); j ++){

                returnVal = returnVal.concat(resultForEach.getJSONObject(j).getString("content") + "\n");
                System.out.println("GOT CONTENT\n" + result.getJSONObject(i).getString("title") );

            }
            returnVal = returnVal.concat("\n");
        }
        System.out.println("REQUESTED explanation between " + keywordTitle + " and " + featureTitle + "\n\n" + returnVal);
        */
        return Response.ok(stringBuilder.toString()).build();



    }

}
