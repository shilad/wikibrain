package org.wikapidia.sr.utils;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: research
 * Date: 7/17/13
 * Time: 1:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestKnownSim {

    @Test
    public void test() throws IOException {



        String path = "/Users/research/IdeaProjects/wikapidia/dat/gold/gold.titles.similarity.txt";
        List<KnownSim> knownSimList = KnownSim.read(new File(path));

        for (KnownSim ks:knownSimList) {
            System.out.println(ks.toString());
        }
    }


}
