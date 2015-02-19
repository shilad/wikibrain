package org.wikibrain.cookbook.wikidata;

import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.Title;
import org.wikibrain.sr.Explanation;
import org.wikibrain.sr.disambig.Disambiguator;
import org.wikibrain.sr.wikidata.WikidataMetric;
import org.wikibrain.wikidata.WikidataDao;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by Josh on 2/4/15.
 * This class is designed to provide explanations using Wikidata
 * It does provide a simple SR metric which will be 0.0 when there
 * are no explanations and 1.0 where there are explanations.
 */

public class WikidataExplanationsTest {
    static private LocalPageDao lpDao;
    static private WikidataDao wdDao;
    public static void main(String args[]) throws ConfigurationException, DaoException, ParserConfigurationException, SAXException, IOException {
        Env env = new EnvBuilder().build();
        Configurator conf = env.getConfigurator();
        lpDao = conf.get(LocalPageDao.class);
        wdDao = conf.get(WikidataDao.class);
        Language simple = Language.getByLangCode("simple");
        
        HashMap parameters = new HashMap();
        parameters.put("language", "simple");
        Disambiguator dis = conf.get(Disambiguator.class, "similarity", parameters);

        metric = new WikidataMetric("wikidata", Language.SIMPLE, lpDao, dis, wdDao);

        addExplanations("Minnesota", "Canada");
        addExplanations("Mexico", "United States");
        addExplanations("Minnesota", "United States");

        addExplanations("University of Minnesota", "Minnesota");

        addExplanations("Benjamin Franklin", "Massachusetts");
        addExplanations("Michael Jackson", "California");
        addExplanations("Michael Jackson", "Indiana");

        printExplanations();
    }

    static private List<Explanation> explanations = new ArrayList<Explanation>();
    static private WikidataMetric metric;

    private static void addExplanations(String itemOne, String itemTwo) throws DaoException {
        try {
            int page1 = lpDao.getIdByTitle(new Title(itemOne, Language.SIMPLE));
            int page2 = lpDao.getIdByTitle(new Title(itemTwo, Language.SIMPLE));
            explanations.addAll(metric.similarity(page1, page2, true).getExplanations());
        } catch (Exception e) {
            explanations.addAll(metric.similarity(itemOne, itemTwo, true).getExplanations());
        }
    }

    private static void printExplanations() {
        for (Explanation e : explanations) {
            System.out.println(String.format(e.getFormat(), e.getInformation().toArray()));
        }
    }
}

