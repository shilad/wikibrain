package org.wikibrain.restapi;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.sr.*;
import org.wikibrain.sr.utils.ExplanationFormatter;

import java.util.*;


/**
 * Root resource (exposed at "myresource" path)
 */
@Path("myresource")
public class MyResource {

    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Path("pingtest")
    @Produces(MediaType.TEXT_PLAIN)
    public String pingTest() {
        return "Ping!";
    }

    @GET
    @Path("similarityBasic")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSimilarityBasic() {
        try{
            // Initialize the WikiBrain environment and get the local page dao
            StringBuilder responseStr = new StringBuilder();
            Env env = new EnvBuilder().build();
            Configurator conf = env.getConfigurator();
            LocalPageDao lpDao = conf.get(LocalPageDao.class);
            Language simple = Language.getByLangCode("simple");

            // Retrieve the "ensemble" sr metric for simple english
            SRMetric sr = conf.get(
                    SRMetric.class, "ensemble",
                    "language", simple.getLangCode());

            //Similarity between strings
            String pairs[][] = new String[][] {
                    { "cat", "kitty" },
                    { "obama", "president" },
                    { "tires", "car" },
                    { "java", "computer" },
                    { "dog", "computer" },
            };

            ExplanationFormatter formatter= new ExplanationFormatter(lpDao);
            for (String pair[] : pairs) {
                SRResult s = sr.similarity(pair[0], pair[1], true);
                responseStr.append(s.getScore() + ": '" + pair[0] + "', '" + pair[1] + "'");
                for (Explanation e:s.getExplanations()) {
                    responseStr.append(formatter.formatExplanation(e));
                }
            }
            return responseStr.toString();
        } catch (ConfigurationException ex){
            return "Operation Failed";

        } catch (DaoException ex){
            return "Operation Failed";
        }
    }

    @GET
    @Path("similarityBasicJSON")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<Map<String, String>> getSimilarityBasicJSON() {
        Set<Map<String, String>> results = new HashSet<Map<String, String>>(); //This set will be automatically converted to JSON format
        try{
            // Initialize the WikiBrain environment and get the local page dao
            Env env = new EnvBuilder().build();
            Configurator conf = env.getConfigurator();
            LocalPageDao lpDao = conf.get(LocalPageDao.class);
            Language simple = Language.getByLangCode("simple");

            // Retrieve the "ensemble" sr metric for simple english
            SRMetric sr = conf.get(
                    SRMetric.class, "ensemble",
                    "language", simple.getLangCode());

            //Similarity between strings
            String pairs[][] = new String[][] {
                    { "cat", "kitty" },
                    { "obama", "president" },
                    { "tires", "car" },
                    { "java", "computer" },
                    { "dog", "computer" },
            };

            ExplanationFormatter formatter= new ExplanationFormatter(lpDao);
            for (String pair[] : pairs) {
                SRResult s = sr.similarity(pair[0], pair[1], true);
                Map<String, String> scores = new HashMap<String, String>();
                scores.put("score", s.getScore()+"");
                scores.put("pairs", pair[0] + "', '" + pair[1] + "'");
                results.add(scores);
            }
            Map<String, String> successMsg = new HashMap<String, String>();
            successMsg.put("success", "true");
            results.add(successMsg);
            return results;
        } catch (Exception ex) {
            Map<String, String> errMsg = new HashMap<String, String>();
            errMsg.put("success", "false");
            results.add(errMsg);
            return results;
        }
    }

    @GET
    @Path("similarityScore")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> getSimilarityScore(@QueryParam("phrase1") String phrase1, @QueryParam("phrase2") String phrase2) {
        Map<String, String> results = new HashMap<String, String>();
        results.put("phrase1", phrase1);
        results.put("phrase2", phrase2);
        try {
            // Initialize the WikiBrain environment and get the local page dao
            Env env = new EnvBuilder().build();
            Configurator conf = env.getConfigurator();
            LocalPageDao lpDao = conf.get(LocalPageDao.class);
            Language simple = Language.getByLangCode("simple");

            // Retrieve the "ensemble" sr metric for simple english
            SRMetric sr = conf.get(
                    SRMetric.class, "ensemble",
                    "language", simple.getLangCode());

            //Similarity between strings
            SRResult s = sr.similarity(phrase1, phrase2, true);
            results.put("score", s.getScore()+"");
            results.put("success", "true");
        } catch (Exception ex) {
            results.put("success", "false");
        }
        return results;
    }
}
