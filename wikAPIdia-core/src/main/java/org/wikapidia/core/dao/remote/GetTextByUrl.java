package org.wikapidia.core.dao.remote;

import java.net.*;
import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: Toby "Jiajun" Li
 * Date: 10/26/13
 * Time: 7:50 PM
 * To change this template use File | Settings | File Templates.
 */






public class GetTextByUrl {

    /**
     *
     * Get the text from a url
     * @param url the text's location
     * @return the text
     * @throws Exception if there was an error retrieving the text
     *
     *
     */


    public static String getText(String url) throws Exception {
        URL website = new URL(url);
        URLConnection connection = website.openConnection();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        connection.getInputStream()));

        StringBuilder response = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null)
            response.append(inputLine);

        in.close();

        return response.toString();
    }


}
