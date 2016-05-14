package org.wikibrain.sr.wikify;

import com.typesafe.config.Config;
import gnu.trove.TCollections;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.core.nlp.NGramCreator;
import org.wikibrain.core.nlp.StringTokenizer;
import org.wikibrain.core.nlp.Token;
import org.wikibrain.phrases.*;
import org.wikibrain.sr.SRMetric;

import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Shilad Sen
 */
public class MilneWittenWikifier implements Wikifier {
    private static final Logger LOG = LoggerFactory.getLogger(MilneWittenWikifier.class);

    private final LocalPageDao lpd;
    private final LocalLinkDao lld;
    private final RawPageDao rpd;
    private final SRMetric metric;
    private final PhraseAnalyzerDao phraseDao;
    private final LinkProbabilityDao linkProbDao;

    private final Language language;
    private int numTestingDocs = 100;
    private double minLinkProbability = 0.03;

    private int maxNGram = 3;

    private StringTokenizer tokenizer = new StringTokenizer();
    private NGramCreator nGramCreator = new NGramCreator();

    public MilneWittenWikifier(SRMetric metric, AnchorTextPhraseAnalyzer pa, LocalPageDao lpd, RawPageDao rpd, LocalLinkDao lld, LinkProbabilityDao linkProbDao) {
        this.lpd = lpd;
        this.linkProbDao = linkProbDao;
        this.phraseDao = pa.getDao();
        this.metric = metric;
        this.rpd = rpd;
        this.lld = lld;
        this.language = metric.getLanguage();
    }

    public void testWikify() throws DaoException {
        int barackId = lpd.getIdByTitle("Barack Obama", language, NameSpace.ARTICLE);
        RawPage rp = rpd.getById(language, barackId);
        for (int i = 0; i < 1; i++) {
            List<LocalLink> detected = wikify(rp.getLocalId());
            System.out.println("Links detected for " + rp.getTitle() + " (" + i + ")");
            for (LocalLink ll : detected) {
                System.out.println("\t" + ll + " page " + lpd.getById(language, ll.getDestId()).getTitle());
            }
        }

    }

    private List<Token> getNGramTokens(String text) {
        List<Token> ngrams = new ArrayList<Token>();
        for (Token sentence : tokenizer.getSentenceTokens(language, text)) {
            List<Token> words = tokenizer.getWordTokens(language, sentence);
            ngrams.addAll(nGramCreator.getNGramTokens(words, 1, maxNGram));
        }
        return ngrams;
    }

    private double getLinkProbability(String phrase) throws DaoException {
        return linkProbDao.getLinkProbability(phrase);
    }


    @Override
    public List<LocalLink> wikify(int wpId, String text) throws DaoException {
        List<LinkInfo> candidates = getCandidates(text);
        identifyKnownCandidates(wpId, candidates);
        List<LinkInfo> detected = detectLinks(candidates);
        List<LocalLink> results = new ArrayList<LocalLink>();
        for (LinkInfo li : detected) {
            results.add(new LocalLink(language, li.getAnchortext(), wpId, li.getDest(), true, li.getStartChar(), true, null));
        }
        return results;
    }

    @Override
    public List<LocalLink> wikify(int wpId) throws DaoException {
        RawPage rp = rpd.getById(language, wpId);
        if (rp == null) {
            return new ArrayList<LocalLink>();
        }
        return wikify(wpId, rp.getPlainText(false));
    }

    @Override
    public List<LocalLink> wikify(String text) throws DaoException {
        List<LinkInfo> candidates = getCandidates(text);
        List<LinkInfo> detected = detectLinks(candidates);
        List<LocalLink> results = new ArrayList<LocalLink>();
        for (LinkInfo li : detected) {
            results.add(new LocalLink(language, li.getAnchortext(), -1, li.getDest(), true, li.getStartChar(), true, null));
        }
        // Sort by position
        Collections.sort(results, new Comparator<LocalLink>() {
            @Override
            public int compare(LocalLink l1, LocalLink l2) {
                return l1.getLocation() - l2.getLocation();
            }
        });
        return results;
    }

    private List<LinkInfo> detectLinks(List<LinkInfo> candidates) throws DaoException {
        Map<String, LinkInfo> scoreCache = new HashMap<String, LinkInfo>();
        TIntDoubleMap relatedness = getRelatedness(candidates);
        for (LinkInfo li : candidates) {
            scoreLinkInfo(li, scoreCache, relatedness);
        }
        TIntSet used = new TIntHashSet();   // used characters
        Collections.sort(candidates);

        List<LinkInfo> detected = new ArrayList<LinkInfo>();
        for (LinkInfo li : candidates) {
            if (li.getScore() < 0.01) {
                break;
            }
            if(!li.intersects(used)) {
                detected.add(li);
                li.markAsUsed(used);
            }
//            if (li.getDest() >= 0) {
//                System.out.println("link " + li.getAnchortext() + " to " + lpd.getById(language, li.getDest()) + " has score " + li.getScore());
//            }
        }

        return detected;
    }

    private TIntDoubleMap getRelatedness(List<LinkInfo> candidates) throws DaoException {
        TIntSet knownSet = new TIntHashSet();
        TIntSet candidateSet = new TIntHashSet();
        for (LinkInfo li : candidates) {
            if (li.getKnownDest() != null) {
                knownSet.add(li.getKnownDest());
            } else if (li.hasOnePossibility()) {
                knownSet.add(li.getTopPriorDestination());
            } else {
                for (int wpId : li.getPrior().keySet()) {
                    candidateSet.add(wpId);
                }
            }
        }

        int [] knownIds = knownSet.toArray();
        int [] candidateIds = candidateSet.toArray();
        double cosimilarity[][] = metric.cosimilarity(candidateIds, knownIds);

        TIntDoubleMap similarities = new TIntDoubleHashMap();
        for (int i = 0; i < candidateIds.length; i++) {
            double sum = 0.0;
            for (double sim : cosimilarity[i]) {
                sum += sim;
            }
            similarities.put(candidateIds[i], sum / knownIds.length);
        }

        return similarities;
    }

    private void scoreLinkInfo(LinkInfo link, Map<String, LinkInfo> cache, TIntDoubleMap allRelatedness) throws DaoException {
        if (link.getKnownDest() != null) {
            link.setDest(link.getKnownDest());
            link.setScore(1000000.0);
            return;
        }
        if (cache.containsKey(link.getAnchortext())) {
            LinkInfo existing = cache.get(link.getAnchortext());
            link.setDest(existing.getDest());
            link.setScore(existing.getScore());
            return;
        }
        for (int wpId : link.getPrior().keySet()) {
            double prior = link.getPrior().get(wpId);
            double relatedness = allRelatedness.get(wpId);
            double score = prior * relatedness * link.getLinkProbability() * getGenerality(wpId);
            link.addScore(wpId, score);
        }
        if (link.getScores().size() == 0) {
            return;
        }

        link.setDest(link.getScores().getElement(0));
        link.setScore(link.getScores().getScore(0));

        if (link.getScores().size() == 1) {
            link.setScore(link.getScore() * 3);
        } else {
            double score2 = link.getScores().getScore(1);
            link.setScore(link.getScore() * Math.min(3.0, link.getScore() / score2));
        }
        cache.put(link.getAnchortext(), link);
    }

    private final TIntDoubleMap generality = TCollections.synchronizedMap(new TIntDoubleHashMap());
    private final int MAX_INLINKS = 1000;
    private double getGenerality(int wpId) throws DaoException {
        if (generality.containsKey(wpId)) {
            return generality.get(wpId);
        }
        int numInLinks = lld.getCount(new DaoFilter().setLanguages(language).setDestIds(wpId));
        double g = 0.5 + Math.log(1 + Math.min(MAX_INLINKS, numInLinks)) / Math.log(1 + MAX_INLINKS);
        generality.put(numInLinks, numInLinks);
        return numInLinks;
    }

    private void identifyKnownCandidates(int wpId, List<LinkInfo> candidates) throws DaoException {
        Set<String> usedAnchors = new HashSet<String>();
        /**
         * Hack: Mark the FIRST POSSIBLE of each candidate link as verified.
         */
        for (LocalLink ll : lld.getLinks(language, wpId, true)) {

            if (ll.getDestId() < 0 || ll.getAnchorText() == null || usedAnchors.contains(ll.getAnchorText())) {
                continue;
            }
            for (LinkInfo li : candidates) {
                if (ll.getAnchorText().equals(li.getAnchortext())) {
                    if (li.getKnownDest() != null) {
                        LOG.info("conflict for link info " + li.getAnchortext() + " between " + li.getKnownDest() + " and " + ll.getDestId());
                    } else {
                        li.setKnownDest(ll.getDestId());
                        break;
                    }
                }
            }
            usedAnchors.add(ll.getAnchorText());
        }
    }

    public List<LinkInfo> getTextContext(String text) throws DaoException {
        return getCandidates(text);
    }

    private List<LinkInfo> getCandidates(String text) throws DaoException {
        Map<String, LinkInfo> cache = new HashMap<String, LinkInfo>();
        List<LinkInfo> candidates = new ArrayList<LinkInfo>();
        for (Token ngram : getNGramTokens(text)) {

            LinkInfo li = makeLinkInfo(ngram, cache);
            if (li != null) {
                candidates.add(li);
            }
        }
        return candidates;
    }

    private LinkInfo makeLinkInfo(Token token, Map<String, LinkInfo> cache) throws DaoException {
        double linkProbability = getLinkProbability(token.getToken());
        if (linkProbability < minLinkProbability) {
            return null;
        }

        if (cache.containsKey(token.getToken())) {
            LinkInfo old = cache.get(token.getToken());
            LinkInfo li = new LinkInfo();
            li.setLinkProbability(linkProbability);
            li.setAnchortext(token.getToken());
            li.setStartChar(token.getBegin());
            li.setEndChar(token.getEnd());
            li.setPrior(old.getPrior());
            return li;
        }

        PrunedCounts<Integer> counts = phraseDao.getPhraseCounts(language, token.getToken(), 30);
        if (counts != null && !counts.isEmpty()) {
            LinkInfo li = new LinkInfo();
            li.setLinkProbability(linkProbability);
            li.setAnchortext(token.getToken());
            li.setStartChar(token.getBegin());
            li.setEndChar(token.getEnd());
            li.setPrior(counts);
            cache.put(token.getToken(), li);
            return li;
        } else {
            return null;
        }
    }
    public static class Provider extends org.wikibrain.conf.Provider<Wikifier> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class<Wikifier> getType() {
            return Wikifier.class;
        }

        @Override
        public String getPath() {
            return "sr.wikifier";
        }

        @Override
        public Wikifier get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (runtimeParams == null || !runtimeParams.containsKey("language")){
                throw new IllegalArgumentException("Wikifier requires 'language' runtime parameter.");
            }
            if (!config.getString("type").equals("milnewitten")) {
                return  null;
            }

            Language language = Language.getByLangCode(runtimeParams.get("language"));
            Configurator c = getConfigurator();
            String srName = config.getString("sr");
            String phraseName = config.getString("phraseAnalyzer");
            String linkName = config.getString("localLinkDao");
            LinkProbabilityDao lpd = Env.getComponent(c, LinkProbabilityDao.class, language);
            if (config.getBoolean("useLinkProbabilityCache")) {
                lpd.useCache(true);
            }

            Wikifier dab = new MilneWittenWikifier(
                    c.get(SRMetric.class, srName, "language", language.getLangCode()),
                    (AnchorTextPhraseAnalyzer)c.get(PhraseAnalyzer.class, phraseName),
                    c.get(LocalPageDao.class),
                    c.get(RawPageDao.class),
                    c.get(LocalLinkDao.class, linkName),
                    lpd
            );
            return dab;
        }
    }


    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
        Env env = EnvBuilder.envFromArgs(args);
        Configurator c = env.getConfigurator();
        MilneWittenWikifier w = c.get(MilneWittenWikifier.class, "default", "language", "simple");
        w.testWikify();
    }
}
