package org.wikibrain.sr.wikify;

import com.typesafe.config.Config;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.core.nlp.StringTokenizer;
import org.wikibrain.core.nlp.Token;
import org.wikibrain.phrases.*;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.vector.FeatureFilter;
import org.wikibrain.utils.Scoreboard;

import java.util.*;
import java.util.logging.Logger;

/**
 * Wikifier based on Doug Downey's approach described in
 *
 * http://web-ngram.research.microsoft.com/erd2014/Docs/submissions/erd14_submission_24.pdf
 * @author Shilad Sen
 */
public class WebSailWikifier implements Wikifier {
    private static final Logger LOG = Logger.getLogger(WebSailWikifier.class.getName());

    /**
     * TODO: Make this configurable
     */
    private int numTrainingLinks = 10000;

    private final Wikifier identityWikifier;
    private final SRMetric metric;
    private final LinkProbabilityDao linkProbDao;
    private final Language language;
    private final PhraseTokenizer phraseTokenizer;
    private final LocalLinkDao linkDao;
    private final PhraseAnalyzerDao phraseDao;
    private final RawPageDao rawPageDao;

    private double desiredLinkRecall = 0.98;
    private double minLinkProbability = 0.01;

    public WebSailWikifier(Wikifier identityWikifier, RawPageDao rawPageDao, LocalLinkDao linkDao, LinkProbabilityDao linkProbDao, PhraseAnalyzerDao phraseDao, SRMetric metric) throws DaoException {
        this.identityWikifier = identityWikifier;
        this.metric = metric;
        this.language = metric.getLanguage();
        this.linkDao = linkDao;
        this.linkProbDao = linkProbDao;
        this.rawPageDao = rawPageDao;
        this.phraseDao = phraseDao;
        this.phraseTokenizer = new PhraseTokenizer(linkProbDao);
        learnMinLinkProbability();
    }

    private void learnMinLinkProbability() throws DaoException {
        LOG.info("Learning minimum link probability");
        TDoubleList probs = new TDoubleArrayList();
        for (LocalLink ll : linkDao.get(new DaoFilter().setLanguages(language).setLimit(numTrainingLinks))) {
            if (ll.getDestId() >= 0) {
                double p = linkProbDao.getLinkProbability(language, ll.getAnchorText());
                probs.add(p);
            }
        }
        probs.sort();
        probs.reverse();
//        for (int i = 0; i < 100; i++) {
//            System.out.println("i: " + i + " is " + probs.get(probs.size() * i / 100));
//        }
        minLinkProbability = probs.get((int)(desiredLinkRecall * probs.size()));
        LOG.info("Set minimum link probability to " + minLinkProbability + " to achieve " + desiredLinkRecall + " recall");
    }

    private List<LinkInfo> getCandidates(int wpId, String text) throws DaoException {
        List<LinkInfo> candidates = new ArrayList<LinkInfo>();
        StringTokenizer tokenizer = new StringTokenizer();
        for (Token sentence : tokenizer.getSentenceTokens(language, text)) {
            for (Token phrase : phraseTokenizer.makePhraseTokens(language, sentence)) {
                double p = linkProbDao.getLinkProbability(language, phrase.getToken());
                if (p > minLinkProbability) {
                    LinkInfo li = new LinkInfo(phrase);
                    li.setLinkProbability(p);
                    candidates.add(li);
                }
            }
        }
        return candidates;
    }


    @Override
    public List<LocalLink> wikify(int wpId, String text) throws DaoException {
        List<LinkInfo> mentions = getCandidates(wpId, text);
        disambiguate(mentions, wpId);
        return link(wpId, text, mentions);
    }

    @Override
    public List<LocalLink> wikify(int wpId) throws DaoException {
        RawPage page = rawPageDao.getById(language, wpId);
        if (page == null) {
            return new ArrayList<LocalLink>();
        } else {
            return wikify(wpId, page.getPlainText(false));
        }
    }

    @Override
    public List<LocalLink> wikify(String text) throws DaoException {
        return null;
    }


    private void disambiguate(List<LinkInfo> infos, int wpId) throws DaoException {
        TIntSet existingIds = getActualLinks(wpId);
        for (LinkInfo li : infos) {
            li.setPrior(phraseDao.getPhraseCounts(language, li.getAnchortext(), 5));
        }
        TIntDoubleMap sr = calculateConceptRelatedness(wpId, existingIds, infos);
        for (LinkInfo li : infos) {
            scoreInfo(wpId, existingIds, li, sr);
        }
    }

    private void scoreInfo(int wpId, TIntSet existingIds, LinkInfo li, TIntDoubleMap sr) {
        if (li.getPrior() == null || li.getPrior().isEmpty()) {
            return;
        }

        Scoreboard<Integer> scores = li.getScores();
        for (int id : li.getPrior().keySet()) {
            double score = 0.4 * sr.get(id) + 0.6 * li.getPrior().get(id) / li.getPrior().getTotal();
            score *= li.getLinkProbability();
            if (existingIds.contains(id)) {
                score += 0.2;
            }
            scores.add(id, score);
        }

        li.setDest(scores.getElement(0));
        double multiplier = (scores.size() == 1) ? 0.2 : (scores.getScore(0) - scores.getScore(1));
        li.setScore(scores.getScore(0) * multiplier);
    }

    private TIntSet getActualLinks(int wpId) throws DaoException {
        TIntSet existingIds = new TIntHashSet();
        for (LocalLink ll : linkDao.getLinks(language, wpId, true)) {
            if (ll.getDestId() >= 0) {
                existingIds.add(ll.getDestId());
            }
        }
        // hack: add the link itself
        existingIds.add(wpId);
        return existingIds;
    }

    private TIntDoubleMap calculateConceptRelatedness(int wpId, TIntSet existingIds, List<LinkInfo> infos) throws DaoException {
        TIntSet candidateIds = new TIntHashSet();
        for (LinkInfo li : infos) {
            if (li.getPrior() != null) {
                for (int id : li.getPrior().keySet()) {
                    candidateIds.add(id);
                }
            }
        }

        int existing[] = existingIds.toArray();
        int candidates[] = candidateIds.toArray();

        TIntDoubleMap results = new TIntDoubleHashMap();
        if (existing.length == 0 || candidates.length == 0) {
            return results;
        }

        double [][] cosim = metric.cosimilarity(candidates, existing);
        for (int i = 0; i < candidates.length; i++) {
            double sum = 0.0;
            for (double s : cosim[i]) {
                if (!Double.isInfinite(s) && !Double.isNaN(s)) {
                    sum += s;
                }
            }
            results.put(candidates[i], sum / existing.length);
        }
        return results;
    }

    private List<LocalLink> link(int wpId, String text, List<LinkInfo> infos) throws DaoException {
        BitSet used = new BitSet(text.length());
        List<LocalLink> results = identityWikifier.wikify(wpId, text);
        for (LocalLink li : results) {
            used.set(li.getLocation(), li.getLocation() + li.getAnchorText().length());
        }

        Collections.sort(infos);
        for (LinkInfo li : infos) {
            if (li.getDest() != null && li.getScore() > 0.001 && used.get(li.getStartChar(), li.getEndChar()).isEmpty()) {
                results.add(li.toLocalLink(language, wpId));
                used.set(li.getStartChar(), li.getEndChar());
            }
        }
        Collections.sort(results, new Comparator<LocalLink>() {
            @Override
            public int compare(LocalLink o1, LocalLink o2) {
                return o1.getLocation() - o2.getLocation();
            }
        });
        return results;
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
            if (!config.getString("type").equals("websail")) {
                return  null;
            }

            Language language = Language.getByLangCode(runtimeParams.get("language"));
            Configurator c = getConfigurator();
            String srName = config.getString("sr");
            String phraseName = config.getString("phraseAnalyzer");
            String identityName = config.getString("identityWikifier");
            String linkName = config.getString("localLinkDao");
            LinkProbabilityDao lpd = c.get(LinkProbabilityDao.class);
            if (config.getBoolean("useLinkProbabilityCache")) {
                lpd.useCache(true);
            }

            try {
                return new WebSailWikifier(
                            c.get(Wikifier.class, identityName, "language", language.getLangCode()),
                            c.get(RawPageDao.class),
                            c.get(LocalLinkDao.class, linkName),
                            lpd,
                            ((AnchorTextPhraseAnalyzer)c.get(PhraseAnalyzer.class, phraseName)).getDao(),
                            c.get(SRMetric.class, srName, "language", language.getLangCode())
                        );
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
