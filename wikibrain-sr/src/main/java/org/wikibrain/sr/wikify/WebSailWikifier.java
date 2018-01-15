package org.wikibrain.sr.wikify;

import com.typesafe.config.Config;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.collections.IteratorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.core.nlp.StringTokenizer;
import org.wikibrain.core.nlp.Token;
import org.wikibrain.phrases.*;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.Scoreboard;

import java.util.*;

/**
 * Wikifier based on Doug Downey's approach described in
 *
 * http://web-ngram.research.microsoft.com/erd2014/Docs/submissions/erd14_submission_24.pdf
 * @author Shilad Sen
 */
public class WebSailWikifier implements Wikifier {
    private static final Logger LOG = LoggerFactory.getLogger(WebSailWikifier.class);

    /**
     * TODO: Make this configurable
     */
    private int numTrainingPages = 500;

    private final Wikifier identityWikifier;
    private final SRMetric metric;
    private final LinkProbabilityDao linkProbDao;
    private final Language language;
    private final PhraseTokenizer phraseTokenizer;
    private final LocalLinkDao linkDao;
    private final PhraseAnalyzerDao phraseDao;
    private final RawPageDao rawPageDao;

    private double desiredWikifiedFraction = 0.25;
    private double minLinkProbability = 0.01;
    private double minFinalScore = 0.001;

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

    public void setDesiredWikifiedFraction(double frac) throws DaoException {
        desiredWikifiedFraction = frac;
        learnMinLinkProbability();
    }

    private void learnMinLinkProbability() throws DaoException {
        if (!linkProbDao.isBuilt()) {
            linkProbDao.build();
        }
        LOG.info("Learning minimum link probability");

        // Choose a sample of numTrainingPages * 4 and train on the longest quarter of them.
        DaoFilter filter = DaoFilter.normalPageFilter(language).setLimit(numTrainingPages * 4);
        List<RawPage> pages = IteratorUtils.toList(rawPageDao.get(filter).iterator());
        Collections.sort(pages, new Comparator<RawPage>() {
            @Override
            public int compare(RawPage p1, RawPage p2) {
                return p2.getBody().length() - p1.getBody().length();
            }
        });
        if (pages.size() > numTrainingPages) {
            pages = pages.subList(0, numTrainingPages);
        }

        minLinkProbability = 0.001;  // set the link probability ridiculously low
        minFinalScore = 0;

        final List<List<LinkInfo>> results = new ArrayList<List<LinkInfo>>();
        ParallelForEach.loop(pages, new Procedure<RawPage>() {
            @Override
            public void call(RawPage page) throws Exception {
                String text = page.getPlainText(false);
                List<LinkInfo> candidates = scoreMentions(page.getLocalId(), text);
                Collections.sort(candidates);
                List<String> words = new StringTokenizer().getWords(language, text);
                int target = (int) (words.size() * desiredWikifiedFraction);
                if (candidates.size() > target) candidates = candidates.subList(0, target);
                synchronized (results) { results.add(candidates); }
            }
        });

        List<Double> minScores = new ArrayList<Double>();
        List<Double> minProbabilities = new ArrayList<Double>();
        for (List<LinkInfo> pageResults: results) {
            if (!pageResults.isEmpty()) {
                double s = 100000000000000f;    // min score
                double p = 10000000000000f;     // min probability
                for (LinkInfo li : pageResults) {
                    if (li.hasScore()) {
                        p = Math.min(p, li.getLinkProbability());
                        s = Math.min(s, li.getScore());
                    }
                }
                minScores.add(s);
                minProbabilities.add(p);
            }
        }

        // For link probabilities, throw out the bottom 5% of outliers
        if (minProbabilities.isEmpty()) {
            minLinkProbability = 0.0001;
        } else {
            Collections.sort(minProbabilities);
            minLinkProbability = minProbabilities.get((int) (minProbabilities.size() * 0.05));
        }

        // For score, take the median of mins
        if (minScores.isEmpty()) {
            minFinalScore = 0.0001;
        } else {
            Collections.sort(minScores);
            minFinalScore = minProbabilities.get((int) (minProbabilities.size() * 0.50));
        }

        LOG.info("Set minimum link probability to " + minLinkProbability + " to achieve " + desiredWikifiedFraction + "% wikification");
        LOG.info("Set minimum final score to " + minFinalScore + " to achieve " + desiredWikifiedFraction + "% wikification");
    }

    private List<LinkInfo> getCandidates(int wpId, String text) throws DaoException {
        return getCandidates(text); // We should do something smarter with the text.
    }

    private List<LinkInfo> getCandidates(String text) throws DaoException {
        List<LinkInfo> candidates = new ArrayList<LinkInfo>();
        StringTokenizer tokenizer = new StringTokenizer();
        for (Token sentence : tokenizer.getSentenceTokens(language, text)) {
            for (Token phrase : phraseTokenizer.makePhraseTokens(language, sentence)) {
                double p = linkProbDao.getLinkProbability(phrase.getToken());
                if (p > minLinkProbability) {
                    LinkInfo li = new LinkInfo(phrase);
                    li.setLinkProbability(p);
                    candidates.add(li);
                }
            }
        }
        return candidates;
    }

    private List<LinkInfo> scoreMentions(int wpId, String text) throws DaoException {
        // Find all mentions that are linked with some likelihood
        List<LinkInfo> mentions = getCandidates(wpId, text);

        // Find disambiguation candidates for each possible mention
        for (LinkInfo li : mentions) {
            li.setPrior(phraseDao.getPhraseCounts(language, li.getAnchortext(), 5));
        }

        // Calculate the relatedness of each mention to known links in the article
        TIntSet existingIds = getActualLinks(wpId);
        TIntDoubleMap sr = calculateConceptRelatedness(existingIds, mentions);

        // Score every possible mention
        for (LinkInfo li : mentions) {
            scoreInfo(existingIds, li, sr);
        }

        return mentions;
    }

    @Override
    public List<LocalLink> wikify(int wpId, String text) throws DaoException {
        return link(wpId, text, scoreMentions(wpId, text));
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
        List<LinkInfo> mentions = getCandidates(text);

        // Temporarily score eveything based on link probability and prior
        for (LinkInfo li : mentions) {
            PrunedCounts<Integer> prior = phraseDao.getPhraseCounts(language, li.getAnchortext(), 5);
            li.setPrior(prior);
            if (prior == null || prior.isEmpty()) continue;
            double p = 1.0 * prior.values().iterator().next() / (prior.getTotal() + 1);
            li.setScore(Math.sqrt(li.getLinkProbability()) * p);
        }

        // Take the top scoring items as existing ids
        Collections.sort(mentions);
        TIntSet existingIds = new TIntHashSet();
        for (int i = 0; i < mentions.size(); i++) {
            LinkInfo li = mentions.get(i);
            if (li.getPrior() == null || li.getPrior().isEmpty()) continue;
            double p = 1.0 * li.getPrior().values().iterator().next() / (li.getPrior().getTotal() + 1);
//            String name = phraseDao.getPageCounts(language, li.getTopPriorDestination(), 1).keySet().iterator().next();
            if ((li.getScore() > 0.01 && i < 3 && p >= 0.5) || (li.getScore() > 0.25 && p >= 0.5)) {
                existingIds.add(li.getTopPriorDestination());
            }
        }

        TIntDoubleMap sr = calculateConceptRelatedness(existingIds, mentions);

        // Score every possible mention
        for (LinkInfo li : mentions) {
            scoreInfo(existingIds, li, sr);
        }

        return link(-1, text, mentions);
    }

    private void scoreInfo(TIntSet existingIds, LinkInfo li, TIntDoubleMap sr) {
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

    private TIntDoubleMap calculateConceptRelatedness(TIntSet existingIds, List<LinkInfo> infos) throws DaoException {
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

    public void setMinFinalScore(double minFinalScore) {
        this.minFinalScore = minFinalScore;
    }

    private List<LocalLink> link(int wpId, String text, List<LinkInfo> infos) throws DaoException {
        BitSet used = new BitSet(text.length());
        List<LocalLink> results = identityWikifier.wikify(wpId, text);
        for (LocalLink li : results) {
            used.set(li.getLocation(), li.getLocation() + li.getAnchorText().length());
        }

        Collections.sort(infos);
        for (LinkInfo li : infos) {
            if (li.getDest() != null && li.getScore() > minFinalScore && used.get(li.getStartChar(), li.getEndChar()).isEmpty()) {
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
            LinkProbabilityDao lpd = Env.getComponent(c, LinkProbabilityDao.class, language);
            if (config.getBoolean("useLinkProbabilityCache")) {
                lpd.useCache(true);
            }

            try {
                WebSailWikifier wikifier = new WebSailWikifier(
                            c.get(Wikifier.class, identityName, "language", language.getLangCode()),
                            c.get(RawPageDao.class),
                            c.get(LocalLinkDao.class, linkName),
                            lpd,
                            ((AnchorTextPhraseAnalyzer)c.get(PhraseAnalyzer.class, phraseName)).getDao(),
                            c.get(SRMetric.class, srName, "language", language.getLangCode())
                        );

                if (config.hasPath("desiredWikifiedFraction")) {
                    wikifier.setDesiredWikifiedFraction(config.getDouble("desiredWikifiedFraction"));
                }
                return wikifier;
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
