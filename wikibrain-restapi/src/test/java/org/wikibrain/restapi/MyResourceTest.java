package org.wikibrain.restapi;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.glassfish.grizzly.http.server.HttpServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class MyResourceTest {

    private HttpServer server;
    private WebTarget target;

    @Before
    public void setUp() throws Exception {
        // start the server
        server = Main.startServer();
        // create the client
        Client c = ClientBuilder.newClient();

        // uncomment the following line if you want to enable
        // support for JSON in the client (you also have to uncomment
        // dependency on jersey-media-json module in pom.xml and Main.startServer())
        // --
        // c.configuration().enable(new org.glassfish.jersey.media.json.JsonJaxbFeature());

        target = c.target(Main.BASE_URI);
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    /**
     * Test to see that the message "Got it!" is sent in the response.
     */
    @Test
    public void testPingTest() {
        String responseMsg = target.path("myresource/pingtest").request().get(String.class);
        assertEquals("Ping!", responseMsg);
    }

    /**
     * Test getSimilarity function
     */
    @Test
    public void testGetSimilarityBasic() {
        String responseMsg = target.path("myresource/similarityBasic").request().get(String.class);
        assertEquals("0.7949277818968377: 'cat', 'kitty'0.819256786419553: 'obama', 'president'0.7888198946695613: 'tires', 'car'0.7152675353640431: 'java', 'computer'0.4714520068290455: 'dog', 'computer'", responseMsg);
    }

    /**
     * Test getSimilarityJSON function with JSON
     */
    @Test
    public void testGetSimilarityBasicJSON() {
        String responseMsg = target.path("myresource/similarityBasicJSON").request().get(String.class);
        assertEquals("[{\"pairs\":\"tires', 'car'\",\"score\":\"0.7888198946695613\"},{\"pairs\":\"obama', 'president'\",\"score\":\"0.819256786419553\"},{\"success\":\"true\"},{\"pairs\":\"cat', 'kitty'\",\"score\":\"0.7949277818968377\"},{\"pairs\":\"dog', 'computer'\",\"score\":\"0.4714520068290455\"},{\"pairs\":\"java', 'computer'\",\"score\":\"0.7152675353640431\"}]", responseMsg);
    }

    /**
     * Test getSimilarityScore function with JSON
     */
    @Test
    public void testGetSimilarityScore() {
        WebTarget webTarget = target.path("myresource/similarityScore")
                .queryParam("phrase1", "tires")
                .queryParam("phrase2", "car");
        String responseMsg = webTarget.request(MediaType.APPLICATION_JSON).get(String.class);
        assertEquals(responseMsg, "{\"score\":\"0.7888198946695613\",\"phrase1\":\"tires\",\"success\":\"true\",\"phrase2\":\"car\"}");
    }

}
