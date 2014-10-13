package org.wikibrain.restapi;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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
        // Initialize the WikiBrain environment and get the local page dao
        try{
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

}
