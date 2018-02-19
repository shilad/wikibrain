package org.wikibrain.sr.wikify;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalLinkDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.RawPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.lang.StringNormalizer;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.nlp.StringTokenizer;
import org.wikibrain.phrases.*;
import org.wikibrain.sr.SRMetric;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Shilad Sen
 */
public class Tester {
    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
        Env env = EnvBuilder.envFromArgs(args);
        Configurator c = env.getConfigurator();
//        RawPageDao rpd = c.get(RawPageDao.class);
//        LocalLinkDao linkDao = c.get(LocalLinkDao.class);
        LocalPageDao pageDao = c.get(LocalPageDao.class);
//        AnchorTextPhraseAnalyzer phraseAnalyzer = (AnchorTextPhraseAnalyzer) c.get(PhraseAnalyzer.class, "anchortext");
//        LinkProbabilityDao linkProbabilityDao = env.getComponent(LinkProbabilityDao.class, Language.SIMPLE);
//        if (!linkProbabilityDao.isBuilt()) {
//            linkProbabilityDao.build();
//        }
//        linkProbabilityDao.useCache(true);

//        System.out.println(linkProbabilityDao.getLinkProbability(Language.SIMPLE, "United States"));
//        System.out.println(linkProbabilityDao.getLinkProbability(Language.SIMPLE, "United_States"));
//        System.exit(0);

//        System.out.println("text is " + rpd.getById(Language.SIMPLE, 116466).getPlainText());
//        LinkProbabilityDao lpd = c.get(LinkProbabilityDao.class);
//        lpd.build();

        Language lang = env.getDefaultLanguage();
        Wikifier identity = env.getComponent(Wikifier.class, "identity", lang);
        WebSailWikifier websail = (WebSailWikifier) env.getComponent(Wikifier.class, "websail", lang);

//        WikiTextCorpusCreator creator = new WikiTextCorpusCreator(Language.SIMPLE, websail, rpd, pageDao, linkProbabilityDao);
//        creator.write(new File("foo"));
        LocalPage obama = pageDao.getByTitle(Language.SIMPLE, NameSpace.ARTICLE, "Barack Obama");
        Set<LocalLink> identityLinks = new HashSet<LocalLink>(identity.wikify(obama.getLocalId()));
        Set<LocalLink> websailLinks= new HashSet<LocalLink>(websail.wikify(obama.getLocalId()));

        Set<Integer> linked = new HashSet<Integer>();
        for (LocalLink ll : identityLinks) linked.add(ll.getLocalId());
        for (LocalLink ll :websailLinks) {
            if (linked.contains(ll.getLocalId())) continue;
            System.out.println(identityLinks.contains(ll) + ": " + ll.getAnchorText() + ": " + pageDao.getById(Language.SIMPLE, ll.getLocalId()));
        }
    }
}
