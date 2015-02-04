package org.wikibrain.cookbook.wikidata;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.sr.Explanation;
import org.wikibrain.sr.disambig.Disambiguator;
import org.wikibrain.wikidata.WikidataDao;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by Josh on 2/4/15.
 */

public class WikidataExplanationsTest {
    public static void main(String args[]) throws ConfigurationException, DaoException, ParserConfigurationException, SAXException, IOException {
        Env env = new EnvBuilder().build();
        Configurator conf = env.getConfigurator();
        LocalPageDao lpDao = conf.get(LocalPageDao.class);
        WikidataDao wdDao = conf.get(WikidataDao.class);
        Language simple = Language.getByLangCode("simple");
        
        HashMap parameters = new HashMap();
        parameters.put("language", "simple");
        Disambiguator dis = conf.get(Disambiguator.class, "similarity", parameters);

        metric = new WikidataMetric("wikidata", Language.SIMPLE, lpDao, dis, wdDao);

        addExplanations("Chocolate", "Food");
        addExplanations("Minnesota", "United States");
        addExplanations("Water", "Chemical compound");
        addExplanations("Water", "Oxygen");
        addExplanations("Water", "Hydrogen");
        addExplanations("TV", "United States");
        addExplanations("Mexico", "United States");

        printExplanations();
    }

    static private List<Explanation> explanations = new ArrayList<Explanation>();
    static private WikidataMetric metric;

    private static void addExplanations(String itemOne, String itemTwo) throws DaoException {
        explanations.addAll(metric.similarity(itemOne, itemTwo, true).getExplanations());
    }

    private static void printExplanations() {
        for (Explanation e : explanations) {
            System.out.println(String.format(e.getFormat(), e.getInformation().toArray()));
        }
    }
}

