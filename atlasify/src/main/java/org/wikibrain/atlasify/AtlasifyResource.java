package org.wikibrain.atlasify;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import com.vividsolutions.jts.geom.Geometry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.LocalLinkDao;
import org.wikibrain.core.dao.UniversalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.Title;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.phrases.PhraseAnalyzer;

import org.wikibrain.sr.Explanation;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.disambig.Disambiguator;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.spatial.dao.SpatialDataDao;

import java.io.*;
import java.net.MalformedURLException;

import org.wikibrain.sr.wikidata.WikidataMetric;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import java.net.URL;
import java.nio.charset.Charset;

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
    private static WikidataMetric wdMetric = null;
    private static WikidataDao wdDao = null;
    private static SpatialDataDao sdDao = null;
    private static UniversalPageDao upDao = null;
    private static Map<Integer, Geometry> geometryMap = null;
    private static AtlasifyLogger atlasifyLogger;

    private static void wikibrainSRinit(){

        try {
            System.out.println("START LOADING WIKIBRAIN");
            Env env = new EnvBuilder().build();
            Configurator conf = env.getConfigurator();
            lpDao = conf.get(LocalPageDao.class);
            System.out.println("FINISHED LOADING LOCALPAGE DAO");
            lpaDao = conf.get(LocalPageAutocompleteSqlDao.class);
            llDao = conf.get(LocalLinkDao.class);
            System.out.println("FINISHED LOADING LOCALLINK DAO");

            sr = conf.get(SRMetric.class, "ensemble", "language", lang.getLangCode());
            System.out.println("FINISHED LOADING SR");

            wdDao = conf.get(WikidataDao.class);
            System.out.println("FINISHED LOADING WIKIDATA DAO");
            HashMap parameters = new HashMap();
            parameters.put("language", lang.getLangCode());
            Disambiguator dis = conf.get(Disambiguator.class, "similarity", parameters);
            wdMetric = new WikidataMetric("wikidata", lang, lpDao, dis, wdDao);
            System.out.println("FINISHED LOADING WIKIDATA METRIC");

            atlasifyLogger = new AtlasifyLogger("./log/AtlasifyLogin.csv", "./log/AtlasifyQuery.csv");
            System.out.println("FINISHED LOADING LOGGER");
            pa = conf.get(PhraseAnalyzer.class, "anchortext");
            System.out.println("FINISHED LOADING PHRASE ANALYZER");

            sdDao = conf.get(SpatialDataDao.class);
            System.out.println("FINISHED LOADING SPATIALDATA DAO");
            upDao = conf.get(UniversalPageDao.class);
            System.out.println("FINISHED LOADING UNIVERSALPAGE DAO");
            geometryMap = sdDao.getAllGeometriesInLayer("wikidata");
            System.out.println("FINISHED LOADING GEOMETRYMAP");
            System.out.println("FINISHED LOADING WIKIBRAIN");


            //sr = conf.get(
            //        SRMetric.class, "ensemble",
            //        "language", "simple");


        } catch (Exception e) {
            System.out.println("Exception when initializing WikiBrain: "+e.getMessage());
        }

    }

    private static LocalId wikibrainPhaseResolution(String title) throws Exception {
        /*Language language = lang;
        LinkedHashMap<LocalId, Float> resolution = pa.resolve(language, title, 1);
        for (LocalId p : resolution.keySet()) {
            return p;
        }

        throw new Exception("failed to resolve"); */

        return new LocalId(lang, lpDao.getByTitle(lang, title).getLocalId());
    }

    private static Map<LocalId, Double> accessNorthwesternAPI(LocalId id, Integer topN) throws Exception {
        Language language = lang;
        String url = "";
        if(topN == -1){
            url = "http://downey-n2.cs.northwestern.edu:8080/wikisr/sr/sID/" + id.getId() + "/langID/" + language.getId();
        }
        else{
            url = "http://downey-n2.cs.northwestern.edu:8080/wikisr/sr/sID/" + id.getId() + "/langID/" + language.getId()+ "/top/" + topN.toString();
        }
        System.out.println("NU QUERY " + url);
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
            try{
                JSONObject pageSRPair = jsonArray.getJSONObject(i);
                LocalId page = new LocalId(language, pageSRPair.getInt("wikiPageId"));
                Double sr = new Double(pageSRPair.getDouble("srMeasure"));
                result.put(page, sr);
            }
            catch (Exception e){
                continue;
            }
        }

        return result;
    }
    @GET
    @Path("/helloworld")
    @Produces("text/plain")
    public Response helloWorld() throws Exception{
        return Response.ok("hello world").build();
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

    static private boolean useNorthWesternAPI = false;


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
                Map<LocalId, Double> srValues = accessNorthwesternAPI(queryID, -1);

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
                        try{
                            System.out.println("NO SR Between " + lpDao.getById(queryID).getTitle().getCanonicalTitle() + " and " + lpDao.getById(featureID).getTitle().getCanonicalTitle());
                        }
                        catch (Exception e1){
                            System.out.println("Failed to get SR");
                        }
                        srMap.put(featureNameList[i].toString(), "#ffffff");
                        continue;
                        //do nothing
                    }
                }
            }
            catch (Exception e) {
                System.out.println("Error when connecting to Northwestern Server ");
                e.printStackTrace();
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
        return Response.ok("received").header("Access-Control-Allow-Origin", "*").build();

    }

    @POST
    @Path("logQuery")
    @Consumes("application/json")
    public Response processLogQuery(AtlasifyLogger.logQuery query) throws Exception{

        atlasifyLogger.QueryLogger(query, "");
        System.out.println("QUERY LOGGED " + query.toString());
        return Response.ok("received").header("Access-Control-Allow-Origin", "*").build();
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

                /* Page Titles that being/contain search term */
            Title title = new Title(query.getKeyword(), language);
            List<LocalPage> similarPages = lpaDao.getBySimilarTitle(title, NameSpace.ARTICLE, llDao);

            for (LocalPage p : similarPages) {
                autocompleteMap.put(i + "", p.getTitle().getCanonicalTitle());
                i++;
            }

                /* Bing */
                /* String bingAccountKey = "Y+KqEsFSCzEzNB85dTXJXnWc7U4cSUduZsUJ3pKrQfs";
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
                }*/

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
        if (lpDao == null) {
            wikibrainSRinit();
        }

        JSONArray explanations = new JSONArray();

        System.out.println("Received query for explanation between " + keyword + " and " + feature);
        String keywordTitle;
        String featureTitle;
        try{
            keywordTitle = lpDao.getById(wikibrainPhaseResolution(keyword)).getTitle().getCanonicalTitle().replace(" ", "_");
            featureTitle = lpDao.getById(wikibrainPhaseResolution(feature)).getTitle().getCanonicalTitle().replace(" ", "_");

            String url = "http://downey-n1.cs.northwestern.edu:3030/api?concept1=" + keywordTitle + "&concept2=" + featureTitle;

            InputStream inputStream = new URL(url).openStream();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
            StringBuilder stringBuilder = new StringBuilder();
            int currentChar;
            while ((currentChar = bufferedReader.read()) != -1) {
                stringBuilder.append((char) currentChar);
            }
            System.out.println("GOT REPLY\n" + stringBuilder.toString());

            // Process the northwestern json
            JSONArray northwesternJSONArray = new JSONArray(stringBuilder.toString());
            for (int i = 0; i < northwesternJSONArray.length(); i++) {
                JSONObject northwesternJSON = northwesternJSONArray.getJSONObject(i);
                JSONArray northwesternExplanations = northwesternJSON.getJSONArray("explanations");
                double srval = northwesternJSON.getDouble("srval");
                String title = northwesternJSON.getString("title");

                for (int j = 0; j < northwesternExplanations.length(); j++) {
                    JSONObject northwesternExplanation = (JSONObject) northwesternExplanations.get(j);

                    String explanationString = northwesternExplanation.getString("content");
                    // Load the complete content if content is unavailable
                    if (explanationString.equals("")) {
                        explanationString = northwesternExplanation.getString("completeContent");
                    }
                    // Make sure the string is still valid
                    if (explanationString.equals("") || explanationString.contains("Category:") || containsExplanation(explanations, explanationString)) {
                        continue;
                    }

                    JSONArray keywordArray = new JSONArray();
                    JSONArray featureArray = new JSONArray();
                    try {
                        keywordArray = northwesternExplanation.getJSONArray(keywordTitle.replace("_", " "));
                    } catch (Exception e) {
                        try {
                            keywordArray = northwesternExplanation.getJSONArray(keywordTitle);
                        } catch (Exception err) {

                        }
                    }
                    try {
                        featureArray = northwesternExplanation.getJSONArray(featureTitle.replace("_", " "));
                    } catch (Exception e) {
                        try {
                            featureArray = northwesternExplanation.getJSONArray(featureTitle);
                        } catch (Exception err) {

                        }
                    }

                    JSONObject jsonExplanation = new JSONObject();
                    jsonExplanation.put("explanation", explanationString);

                    JSONObject data = new JSONObject();
                    data.put("algorithm", "northwestern");
                    data.put("keyword", keyword);
                    data.put("keyword-data", keywordArray);
                    data.put("feature-data", featureArray);
                    data.put("feature", feature);
                    data.put("srval", srval);
                    data.put("title", title);
                    jsonExplanation.put("data", data);

                    explanations.put(explanations.length(), jsonExplanation);
                }
            }
        }
        catch (Exception e){
            System.out.println("Failed to resolve " + keyword + " and " + feature);
            // return Response.ok("").header("Access-Control-Allow-Origin", "*").build();
        }

        // Get Wikidata Explanations using the disambiguator
        for (Explanation exp : wdMetric.similarity(keyword, feature, true).getExplanations()) {
            String explanationString = String.format(exp.getFormat(), exp.getInformation().toArray());
            if (containsExplanation(explanations, explanationString)) {
                continue;
            }

            JSONObject jsonExplanation = new JSONObject();
            jsonExplanation.put("explanation", explanationString);

            JSONObject data = new JSONObject();
            data.put("algorithm", "wikidata");
            data.put("page-finder", "disambiguator");
            data.put("keyword", keyword);
            data.put("feature", feature);
            jsonExplanation.put("data", data);
            System.out.println("GOT WIKIDATA EXPLANATION " + jsonExplanation.toString() + "\n\n");

            explanations.put(explanations.length(), jsonExplanation);
        }

        // Get Wikidata Explanations using the LocalPageDao
        int keywordID = lpDao.getIdByTitle(new Title(keyword, Language.SIMPLE));
        int featureID = lpDao.getIdByTitle(new Title(feature, Language.SIMPLE));
        for (Explanation exp : wdMetric.similarity(keywordID, featureID, true).getExplanations()) {
            String explanationString = String.format(exp.getFormat(), exp.getInformation().toArray());
            if (containsExplanation(explanations, explanationString)) {
                continue;
            }

            JSONObject jsonExplanation = new JSONObject();
            jsonExplanation.put("explanation", explanationString);

            JSONObject data = new JSONObject();
            data.put("algorithm", "wikidata");
            data.put("page-finder", "local-page-dao");
            data.put("keyword", keyword);
            data.put("feature", feature);
            jsonExplanation.put("data", data);

            explanations.put(explanations.length(), jsonExplanation);
        }

        shuffleJSONArray(explanations);
        JSONObject result = new JSONObject();
        result.put("explanations", explanations);

        System.out.println("REQUESTED explanation between " + keyword + " and " + feature + "\n\n" + explanations.toString());

        return Response.ok(result.toString()).header("Access-Control-Allow-Origin", "*").build();
    }

    private Random randomSeedGenerator = new Random();
    private void shuffleJSONArray(JSONArray array) {
        int remainingItems = array.length() - 1;
        Random rand = new Random(randomSeedGenerator.nextInt());
        while (remainingItems > 0) {
            int index = rand.nextInt(remainingItems);

            // Swap index and the last item
            Object object = array.getJSONObject(index);
            array.put(index, array.get(remainingItems));
            array.put(remainingItems, object);

            remainingItems--;
        }
    }

    private boolean containsExplanation(JSONArray array, String explanation) {
        for (int i = 0; i < array.length(); i++) {
            if (array.getJSONObject(i).get("explanation").equals(explanation)) {
                return true;
            }
        }

        return false;
    }

    // This method is used to progress the explanations information from Atlasify
    @POST
    @Path("/explanationsData")
    @Consumes("application/json")

    public void processesExplanations(String json) throws DaoException {
        JSONObject explanationsData = new JSONObject(json);
        int id = explanationsData.getInt("id");

        JSONArray dataArray = explanationsData.getJSONArray("data");
        JSONObject data = new JSONObject();
        data.put("data", dataArray);
        data.put("time", new Date().getTime());
        data.put("id", id);

        // See if log file exists
        String file = "explanation-logs/" + id + ".json";
        File f = new File(file);
        if (f.isFile()) {
            // Write to the file
            try {
                String fileContents = new String(Files.readAllBytes(Paths.get(file)), Charset.defaultCharset());
                JSONArray fileArray = new JSONArray(fileContents);
                fileArray.put(fileArray.length(), data);

                Writer writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(file), "utf-8"));
                writer.write(fileArray.toString());
                writer.close();
            } catch (IOException e) {

            }
        } else {
            // Create it
            try {
                JSONArray fileArray = new JSONArray();
                fileArray.put(fileArray.length(), data);
                Writer writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(file), "utf-8"));
                writer.write(fileArray.toString());
                writer.close();
            } catch (IOException e) {

            }
        }
    }


    //return the list of all spatial objects in the top 100 most realted articles
    @GET
    @Path("/getpoi/id={keyword}")
    @Consumes("text/plain")
    @Produces("text/plain")

    public Response getPOIs (@PathParam("keyword") String keyword){
        if(pa==null){
            wikibrainSRinit();
        }
        System.out.println("REQUESTED POI "+keyword);


        Map<String, String>srMap=new HashMap<String, String>();
        LocalId queryID=new LocalId(lang,0);
        try{
            queryID=wikibrainPhaseResolution(keyword);
        }
        catch(Exception e){
            System.out.println("Failed to resolve keyword "+keyword);
            return Response.ok(new JSONObject(srMap).toString()).build();
        }
        // LocalId queryID = new LocalId(Language.EN, 19908980);
        Map<String, Geometry>resultMap=new HashMap<String, Geometry>();
        try{
            Map<LocalId, Double>srValues=accessNorthwesternAPI(queryID,100);
            for(Map.Entry<LocalId, Double>e:srValues.entrySet()){
                try{
                    LocalPage localPage=lpDao.getById(e.getKey());
                    int univId=upDao.getByLocalPage(localPage).getUnivId();
                    if(geometryMap.containsKey(univId)){
                        resultMap.put(localPage.getTitle().getCanonicalTitle(),geometryMap.get(univId));
                    }
                }
                catch(Exception e1){
                    continue;
                }


            }

        }
        catch(Exception e){
            System.out.println("Error when connecting to Northwestern Server ");
            e.printStackTrace();
            // do nothing

        }
        System.out.println("GOT POI "+(resultMap.toString()));
        JSONObject jsonMap = new JSONObject(resultMap);
        System.out.println("GOT JSON POI " + jsonMap);
        System.out.println("GOT POI "+(jsonMap.toString()));
        return Response.ok(jsonMap.toString()).header("Access-Control-Allow-Origin", "*").build();
    }
    // A logging method called by the god mode of Atlasify to check the status of the system
    @POST
    @Path("/status")
    @Produces("application/json")

    public Response getLog () throws DaoException{
        ByteArrayOutputStream output = AtlasifyServer.logger;
        String s = output.toString();

        /* In order to support multiple god modes running the console
         * output cannot be cleared. This functionality could change
         * in the future if there are performance problems.
         */
        // output.reset();

        Map<String, String> result = new HashMap<String, String>();
        result.put("log", s);

        return Response.ok(new JSONObject(result).toString()).header("Access-Control-Allow-Origin", "*").build();
    }

}
