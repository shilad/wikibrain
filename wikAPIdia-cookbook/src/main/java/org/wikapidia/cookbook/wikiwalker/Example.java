package org.wikapidia.cookbook.wikiwalker;

import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.core.model.LocalPage;

/**
 * @author Shilad Sen
 */
public class Example {
    public static void main(String args[]) throws ConfigurationException {
        WikAPIdiaWrapper wrapper =  new WikAPIdiaWrapper(Utils.PATH_DB);
        LocalPage obama = wrapper.getLocalPageByTitle(Utils.LANG_SIMPLE, "Barack Obama");
        System.out.println("Obama is " + obama);
        Node parent = new Node(wrapper, obama);
        for (Node child : parent.getChildren()) {
            System.out.println("\tchild: " + child);
        }
    }
}
