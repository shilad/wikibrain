package org.wikibrain.cookbook.wikiwalker;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.core.model.LocalPage;

/**
 * @author Shilad Sen
 */
public class Example {
    public static void main(String args[]) throws ConfigurationException {
        WikiBrainWrapper wrapper =  new WikiBrainWrapper(Utils.PATH_DB);
        LocalPage obama = wrapper.getLocalPageByTitle(Utils.LANG_SIMPLE, "Barack Obama");
        System.out.println("Obama is " + obama);
        Node parent = new Node(wrapper, obama);
        for (Node child : parent.getChildren()) {
            System.out.println("\tchild: " + child);
        }
    }
}
