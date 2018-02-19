package org.wikibrain.sr.wikify;

import com.typesafe.config.Config;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.core.nlp.StringTokenizer;
import org.wikibrain.core.nlp.Token;
import org.wikibrain.phrases.*;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.vector.DenseVectorSRMetric;
import org.wikibrain.utils.*;

import java.io.BufferedWriter;
import java.io.IOException;
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
    private int numTrainingPages = 100;
    private static final int SMOOTH_PRIOR = 2;

    private final Wikifier identityWikifier;
    private final SRMetric metric;
    private final LinkProbabilityDao linkProbDao;
    private final Language language;
    private final PhraseTokenizer phraseTokenizer;
    private final LocalLinkDao linkDao;
    private final PhraseAnalyzerDao phraseDao;
    private final RawPageDao rawPageDao;
    private boolean training = false;

    private double desiredWikifiedFraction = 0.05;
    private double desiredPrecision = 0.99;
    private double desiredRecall = 1.0;
    private double minLinkProbability = -1;
    private double minFinalScore = 0.001;


    // Model is: correct ~ smoothed_prior + sr + gap + existing + only + log(linkprob)
    private static final int COEF_OFFSET_INDEX = 0;
    private static final int COEF_PRIOR_INDEX = 1;
    private static final int COEF_SR_INDEX = 2;
    private static final int COEF_GAP_INDEX = 3;
    private static final int COEF_EXISTS_INDEX = 4;
    private static final int COEF_ONLY_INDEX = 5;
    private static final int COEF_LINKPROB_INDEX = 6;

    private double [] coefficients = new double[7];
    {
        Arrays.fill(coefficients, 1.0 / 7);
    }

    public WebSailWikifier(Wikifier identityWikifier, RawPageDao rawPageDao, LocalLinkDao linkDao, LinkProbabilityDao linkProbDao, PhraseAnalyzerDao phraseDao, SRMetric metric) throws DaoException {
        this.identityWikifier = identityWikifier;
        this.metric = metric;
        this.language = metric.getLanguage();
        this.linkDao = linkDao;
        this.linkProbDao = linkProbDao;
        this.rawPageDao = rawPageDao;
        this.phraseDao = phraseDao;
        this.phraseTokenizer = new PhraseTokenizer(linkProbDao);
    }

    public void setDesiredWikifiedFraction(double frac) throws DaoException {
        desiredWikifiedFraction = frac;
    }

    public void train() throws DaoException {
        // Set initial values
        learnMinLinkProbability();
        trainCoefficients();
    }

    private void learnMinLinkProbability() throws DaoException {
        if (!linkProbDao.isBuilt()) {
            linkProbDao.build();
        }
        LOG.info("Learning minimum link probability");

        // Choose a sample of numTrainingPages * 2 and train on the longest quarter of them.
        DaoFilter filter = DaoFilter.normalPageFilter(language).setLimit(numTrainingPages * 2);
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

    private synchronized void trainCoefficients() throws DaoException {
        LOG.info("Learning coefficients");
        training = true;

        // Choose a sample of numTrainingPages .
        DaoFilter filter = DaoFilter.normalPageFilter(language).setLimit(1000);
        final Map<LocalLink, LinkInfo> train = new HashMap<LocalLink, LinkInfo>();

        final List<List<LinkInfo>> results = new ArrayList<List<LinkInfo>>();
        ParallelForEach.iterate(rawPageDao.get(filter).iterator(), new Procedure<RawPage>() {
            @Override
            public void call(RawPage page) throws Exception {
                String text = page.getPlainText(false);
                Map<Integer, LocalLink> actual = new HashMap<Integer, LocalLink>();
                for (LocalLink ll : identityWikifier.wikify(page.getLocalId(), text)) {
                    actual.put(ll.getLocation(), ll);
                }
                Map<Integer, LinkInfo> candidates = new HashMap<Integer, LinkInfo>();
                for (LinkInfo li : scoreMentions(page.getLocalId(), text)) {
                    int loc = li.getStartChar();
                    if (actual.containsKey(loc) && actual.get(loc).getAnchorText().equals(li.getAnchortext())) {
                        candidates.put(loc, li);
                    }
                }

                synchronized (train) {
                    for (int loc : candidates.keySet()) {
                        LocalLink ll = actual.get(loc);
                        LinkInfo li = candidates.get(loc);
                        // Mark the correct existing link
                        for (int id : li.getCandidates()) {
                            li.getFeature(id).correct = (id == ll.getDestId());
                        }
                        train.put(actual.get(loc), candidates.get(loc));
                    }
                }
            }
        });

        trainModel(train);
//        writeTrainingDataset(train);

        training = false;
    }

    private void trainModel(Map<LocalLink, LinkInfo> train) {
        Random rand = new Random();

        // Build up the training dataset.
        // Randomize the existing link settings...
        // Holding out the link itself out penalizes things too much.
        // In practice, we expect *most* existing links to be correct, but not all of them.
        List<LinkInfo> candidates = new ArrayList<LinkInfo>();
        for (LinkInfo li : train.values()) {

            // Are we missing data?
            if (li == null || li.getDest() == null) continue;

            // Does this link info represent the only candidate?
            if (li.hasOnePossibility() && li.getPrior().getTotal() == 1) continue;

            for (int id : li.getCandidates()) {
                LinkInfo.Feature f = li.getFeature(id);
                if (f.correct) {
                    f.existingLink = f.existingLink || rand.nextDouble() < 0.2;
                } else {
                    f.existingLink = f.existingLink || rand.nextDouble() < 0.005;
                }
            }

            candidates.add(li);
        }

        // Within location stage: Train the model to score candidatres for a particular location\
        // model is: is_correct ~ smoothed_prior + sr + has_existing

        // Build up the within training dataset
        int n = 0;
        for (LinkInfo li : candidates) n += li.getCandidates().size();
        double[][] X1 = new double[n][3];
        double [] Y1 = new double[n];
        int i = 0;
        for (LinkInfo li : candidates) {
            List<Integer> cands = new ArrayList<Integer>(li.getCandidates());
            Collections.sort(cands);
            for (int id : cands) {
                LinkInfo.Feature f = li.getFeature(id);

                X1[i][0] = f.sr;
                X1[i][1] = 1.0 * f.priorCount / (SMOOTH_PRIOR + f.totalCount);
                X1[i][2] = f.existingLink ? 1 : 0;
                Y1[i] = f.correct ? 1 : 0;
                i++;
            }
        }
        assert(i == n);

        // Fit model and create scores.
        OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
        regression.newSampleData(Y1, X1);
        LOG.info("R^2 for stage 1 of NER regression is " + regression.calculateAdjustedRSquared());
        double[] coefficients = regression.estimateRegressionParameters();
        double[] scores = new double[n];
        for (i = 0; i < n; i++) {
            scores[i] = coefficients[0];
            for (int j = 0; j < X1[i].length; j++) {
                scores[i] += coefficients[j + 1] * X1[i][j];
            }
        }

        // Between location stage: Find the top-scoring within-link candidates.
        // Train a model for across-link top candidates
        // Model is: correct ~ smoothed_prior + sr + gap + existing + only + log(linkprob)
        double [][] X2 = new double[candidates.size()][6];
        double [] Y2 = new double[candidates.size()];

        int offset = 0;
        for (i = 0; i < candidates.size(); i++) {
            LinkInfo li = candidates.get(i);
            List<Integer> cands = new ArrayList<Integer>(li.getCandidates());
            Collections.sort(cands);

            double[] candScores = Arrays.copyOfRange(scores, offset, offset+cands.size());
            int maxIndex = 0;
            for (int j = 1; j < candScores.length; j++) {
                if (candScores[j] > candScores[maxIndex]) {
                    maxIndex = j;
                }
            }
            Arrays.sort(candScores);
            ArrayUtils.reverse(candScores);

            LinkInfo.Feature f = li.getFeature(cands.get(maxIndex));
            X2[i][COEF_PRIOR_INDEX - 1] = 1.0 * f.priorCount / (SMOOTH_PRIOR + f.totalCount);
            X2[i][COEF_SR_INDEX - 1] = f.sr;
            X2[i][COEF_GAP_INDEX - 1] = (candScores.length == 1) ? 0.0 : (candScores[0] - candScores[1]);
            X2[i][COEF_EXISTS_INDEX - 1] = f.existingLink ? 1 : 0;
            X2[i][COEF_ONLY_INDEX - 1] = (candScores.length == 1) ? 1.0 : 0.0;
            X2[i][COEF_LINKPROB_INDEX - 1] = Math.log(li.getLinkProbability());
            Y2[i] = f.correct ? 1.0 : 0.0;

            offset += cands.size();
        }

        // Fit model and report scores.
        regression = new OLSMultipleLinearRegression();
        regression.newSampleData(Y2, X2);
        double[] estimates = regression.estimateRegressionParameters();

        LOG.info("R^2 for stage 2 of NER regression is " + regression.calculateAdjustedRSquared());
        LOG.info("Coefficients for stage 2 are: " + ArrayUtils.toString(estimates));

        this.coefficients = ArrayUtils.clone(estimates);

        List<double[]> finalResults = new ArrayList<double[]>(); // array of (score, label)
        for (i = 0; i < X2.length; i++) {
            double s = this.coefficients[0];
            for (int j = 0; j < X2[i].length; j++) {
                s += this.coefficients[j + 1] * X2[i][j];
            }
            finalResults.add(new double[] { s, Y2[i]});
        }
        Collections.sort(finalResults, new Comparator<double[]>() {
            @Override
            public int compare(double[] o1, double[] o2) {
                return -1 * Double.compare(o1[0], o2[0]);
            }
        });
        int totalPositive = 0;
        for (double y : Y2) {
            if (y > 0.5) totalPositive++;
        }

        int k;
        int hits = 0;
        for (k = 0; k < finalResults.size(); k++) {
            if (finalResults.get(k)[1] > 0.5) hits++;

            // Go through at least 20% of the results...
            if (k > finalResults.size() / 5) {
                if (1.0 * hits / totalPositive > desiredRecall) break;
                if (1.0 * hits / k < desiredPrecision) break;
            }
        }
        this.minFinalScore = finalResults.get(k)[0];
        LOG.info("Set score cutoff at " + this.minFinalScore +
                " yielding precision  " + (1.0 * hits / k ) +
                " and recall " + 1.0 * hits / totalPositive);

    }

    private void writeTrainingDataset(Map<LocalLink, LinkInfo> train) throws DaoException {
        try {
            int total = 0;
            int hits = 0;
            BufferedWriter writer = WpIOUtils.openWriter("analyses/data/ner.tsv");
            writer.write("id\trank\tcorrect\texisting\tsr\tprior\ttotal\tlinkprob\tscore\n");
            for (LocalLink actual : train.keySet()) {
                LinkInfo li = train.get(actual);
                // Are we missing data?
                if (li == null || li.getDest() == null || actual == null) continue;

                // Does this link info represent the only candidate?
                if (li.hasOnePossibility() && li.getPrior().getTotal() == 1) continue;

                total++;
                int rank = 1;
                for (int id : li.getCandidates()) {
                    LinkInfo.Feature f = li.getFeature(id);
                    writer.write("" +
                            total + "\t" +
                            rank + "\t" +
                            (f.correct ? 1 : 0) + "\t" +
                            (f.existingLink ? 1 : 0) + "\t" +
                            f.sr + "\t" +
                            f.priorCount + "\t" +
                            f.totalCount + "\t" +
                            li.getLinkProbability() + "\t" +
                            f.score + "\n"
                    );
                    rank++;
                }
                if (li.getDest() == actual.getDestId()) {
                    hits++;
                }
            }
            writer.close();
        } catch (IOException e) {
            throw new DaoException(e);
        }


    }

    private List<LinkInfo> getCandidates(int wpId, String text) throws DaoException {
        return getCandidates(text); // We should do something smarter with the text.
    }

    private List<LinkInfo> getCandidates(String text) throws DaoException {
        if (minLinkProbability < 0) {
            synchronized (this) {
                if (minLinkProbability < 0)  train();
            }
        }
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
        if (training) {
            TIntIntMap counts = getActualLinksAndCounts(wpId);
            for (LinkInfo li : mentions) {
                for (int id : li.getCandidates()) {
                    LinkInfo.Feature f = li.getFeature(id);
                    f.sr = sr.get(id);

                    // In the calculations below, we don't include the training link itself
                    f.priorCount= li.getPrior().get(id) - 1;
                    f.totalCount = li.getPrior().getTotal() - 1;
                    f.existingLink = counts.get(id) > 1;
                    f.score = li.getScore();

                    // if this is the only link adjust sr by removing the 1.0
                    if (counts.containsKey(id) && counts.get(id) == 1) {
                        if (existingIds.size() == 1) {
                            f.sr = Double.NaN;
                        } else {
                            f.sr = (f.sr * existingIds.size() - 1.0) / (existingIds.size() - 1);
                        }
                    }
                }
            }
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
            double score = coefficients[COEF_OFFSET_INDEX] +
                           coefficients[COEF_SR_INDEX] + sr.get(id) +
                           coefficients[COEF_PRIOR_INDEX] * li.getPrior().get(id) / (li.getPrior().getTotal() + SMOOTH_PRIOR) +
                           coefficients[COEF_LINKPROB_INDEX] * Math.log(li.getLinkProbability());
            if (existingIds.contains(id)) {
                score += coefficients[COEF_EXISTS_INDEX];

            }
            scores.add(id, score);
        }

        double score = scores.getScore(0);
        if (scores.size() == 1) {
            score += coefficients[COEF_ONLY_INDEX];
        } else {
            score += coefficients[COEF_GAP_INDEX] * (scores.getScore(0) - scores.getScore(1));
        }
        li.setDest(scores.getElement(0));
        li.setScore(score);
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
    private TIntIntMap getActualLinksAndCounts(int wpId) throws DaoException {
        TIntIntMap counts = new TIntIntHashMap();
        for (LocalLink ll : linkDao.getLinks(language, wpId, true)) {
            if (ll.getDestId() >= 0) {
                counts.adjustOrPutValue(ll.getDestId(), 1, 1);
            }
        }
        // hack: add the link itself
        counts.put(wpId, 1);
        return counts;
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

    public void setDesiredPrecision(double desiredPrecision) {
        this.desiredPrecision = desiredPrecision;
    }

    public void setDesiredRecall(double desiredRecall) {
        this.desiredRecall = desiredRecall;
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
                if (config.hasPath("desiredRecall")) {
                    wikifier.desiredRecall = config.getDouble("desiredRecall");
                }
                if (config.hasPath("desiredPrecision")) {
                    wikifier.desiredPrecision = config.getDouble("desiredPrecision");
                }
                return wikifier;
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
