package org.wikibrain.atlasify;

import gnu.trove.set.TIntSet;
import org.jooq.util.derby.sys.Sys;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.sr.*;
import org.wikibrain.sr.disambig.Disambiguator;
import org.wikibrain.sr.ensemble.Ensemble;
import org.jooq.tools.StringUtils;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.Title;
import org.wikibrain.wikidata.LocalWikidataStatement;
import org.wikibrain.wikidata.WikidataDao;
import org.apache.commons.lang.WordUtils;

import java.io.IOException;
import java.util.*;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import sun.rmi.runtime.Log;

import javax.lang.model.element.Element;
import javax.xml.parsers.*;
import org.w3c.dom.*;

/**
 * Created by Josh on 2/2/15.
 */
public class WikidataMetric extends BaseSRMetric {
    private WikidataDao wdDao;
    private String defaultExplanation;
    private Map<Integer, String> explanations;

    public WikidataMetric(String name, Language language, LocalPageDao pageHelper, Disambiguator disambiguator, WikidataDao wikidataDao) throws ConfigurationException, ParserConfigurationException, SAXException, IOException {
        super(name,language, pageHelper, disambiguator);
        wdDao = wikidataDao;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder document = factory.newDocumentBuilder();
        Document dom = document.parse("dat/wikidata/WikidataExplanationPhrases.xml");
        org.w3c.dom.Element xml = dom.getDocumentElement();

        explanations = new HashMap<Integer, String>();

        NodeList list = xml.getElementsByTagName("Phrase");
        for (int i = 0; i < list.getLength(); i++) {
            org.w3c.dom.Element phrase = (org.w3c.dom.Element) list.item(i);

            String id = phrase.getElementsByTagName("ID").item(0).getFirstChild().getNodeValue();
            String text = phrase.getElementsByTagName("Text").item(0).getFirstChild().getNodeValue();
            if (id.equals("default")) {
                defaultExplanation = text;
            } else {
                Integer intID = Integer.parseInt(id);
                explanations.put(intID, text);
            }
        }
    }

    @Override
    public SRResultList mostSimilar(int pageId, int maxResults, TIntSet validIds) throws DaoException {
        SRResultList list = new SRResultList(maxResults);
        int i = 0;
        for (int id : validIds.toArray()) {
            if (i > maxResults) {
                break;
            }
            list.set(i, similarity(pageId, id, true));
            i++;
        }
        return list;
    }

    @Override
    public SRResult similarity(int pageId1, int pageId2, boolean explanations) throws DaoException {
        LocalPage page = getLocalPageDao().getById(getLanguage(), pageId1);
        LocalPage compare = getLocalPageDao().getById(getLanguage(), pageId2);
        Map<String, List<LocalWikidataStatement>> statements = wdDao.getLocalStatements(page);

        SRResult result = new SRResult();
        result.setId(pageId2);
        result.setScore(0.0);

        for (String property : statements.keySet()) {
            for (LocalWikidataStatement lws : statements.get(property)) {
                if (lws.getValue().contains(compare.getTitle().getCanonicalTitle())) {
                    result.setScore(1.0);

                    if (explanations) {
                        Explanation explanation = new Explanation("");

                        ArrayList info = new ArrayList();


                        int id = lws.getStatement().getProperty().getId();
                        String format = getExplanationFormat(id);
                        info.add(WordUtils.capitalize(page.getTitle().getCanonicalTitle()));
                        info.add(WordUtils.capitalize(compare.getTitle().getCanonicalTitle()));
                        info.add(WordUtils.capitalize(lws.getProperty()));


                        explanation.setFormat(format);
                        explanation.setInformation(info);

                        result.addExplanation(explanation);
                    }
                }
            }
        }

        return result;
    }

    private String getExplanationFormat(int id) {
        String format = explanations.get(id);
        if (format == null) {
            format = defaultExplanation;
        }
        return format;
    }

    @Override
    public SRConfig getConfig() { return new SRConfig(); }
}
