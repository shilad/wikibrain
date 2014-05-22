package org.wikibrain.phrases;

import com.typesafe.config.Config;
import gnu.trove.map.TLongFloatMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongFloatHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.RawPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.StringNormalizer;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.core.nlp.NGramCreator;
import org.wikibrain.core.nlp.StringTokenizer;
import org.wikibrain.core.nlp.Token;
import org.wikibrain.utils.*;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 * Calculates the probability of a link
 */
public class LinkProbabilityDao {
    private static final Logger LOG = Logger.getLogger(LinkProbabilityDao.class.getName());

    private final File path;
    private final RawPageDao pageDao;
    private final PhraseAnalyzerDao phraseDao;
    private final LanguageSet langs;
    private final StringNormalizer normalizer;

    private ObjectDb<Double> db;
    private TLongFloatMap cache = null;

    /**
     * Range of ngrams to consider.
     */
    private int minGram = 1;
    private int maxGram = 3;


    public LinkProbabilityDao(File path, LanguageSet langs, RawPageDao pageDao, PhraseAnalyzerDao phraseDao) throws DaoException {
        this.path = path;
        this.langs = langs;
        this.pageDao = pageDao;
        this.phraseDao = phraseDao;
        this.normalizer = phraseDao.getStringNormalizer();

        if (path.exists()) {
            try {
                db = new ObjectDb<Double>(path, false);
            } catch (IOException e) {
                throw new DaoException(e);
            }
        } else {
            LOG.warning("path " + path + " does not exist... wll not function until build() is called.");
        }
    }

    public double getLinkProbability(Language language, String mention) throws DaoException {
        if (db == null) {
            throw new IllegalStateException("Dao has not yet been built. Call build()");
        }
        String normalizedMention = normalizer.normalize(language, mention);
        String key = language.getLangCode() + ":" + normalizedMention;

        Double d = null;
        try {
            d = db.get(key);
        } catch (IOException e) {
            throw new DaoException(e);
        } catch (ClassNotFoundException e) {
            throw new DaoException(e);
        }
        if (d == null) {
            return 0.0;
        } else {
            return d;
        }
    }

    public synchronized void useCache(boolean useCache) {
        if (useCache) {
            LOG.info("building cache...");
            TLongFloatMap cache = new TLongFloatHashMap();
            Iterator<Pair<String, Double>> iter = db.iterator();
            while (iter.hasNext()) {
                Pair<String, Double> entry = iter.next();
                String tokens[] = entry.getKey().split(":", 2);
                Language lang = Language.getByLangCode(tokens[0]);
                long hash = hashCode(lang, normalizer.normalize(lang, entry.getKey()));
                cache.put(hash, entry.getRight().floatValue());
            }
            this.cache = cache;
            LOG.info("created cache with " + cache.size() + " entries");
        } else {
            cache = null;
        }
    }

    public void build() throws DaoException {
        if (db != null) {
            db.close();
        }
        if (path.exists()) {
            FileUtils.deleteQuietly(path);
        }
        path.mkdirs();

        try {
            this.db = new ObjectDb<Double>(path, true);
        } catch (IOException e) {
            throw new DaoException(e);
        }
        for (Language lang : langs) {
            this.build(lang);
        }
    }

    private void build(Language lang) throws DaoException {
        final TLongIntMap counts = new TLongIntHashMap();
        Iterator<String> iter = phraseDao.getAllPhrases(lang);
        while (iter.hasNext()) {
            String phrase = iter.next();
            long hash = hashCode(lang, phrase);
            counts.put(hash, 0);
        }
        LOG.info("found " + counts.size() + " unique anchortexts");

        DaoFilter filter = new DaoFilter()
                .setRedirect(false)
                .setDisambig(false)
                .setNameSpaces(NameSpace.ARTICLE);

        LOG.info("building link probabilities for language " + lang);
        ParallelForEach.iterate(
                pageDao.get(filter).iterator(),
                WpThreadUtils.getMaxThreads(),
                100,
                new Procedure<RawPage>() {
                    @Override
                    public void call(RawPage page) throws Exception {
                        processPage(counts, page);
                    }
                },
                10000);

        Iterator<Pair<String, PrunedCounts<Integer>>> phraseIter = phraseDao.getAllPhraseCounts(lang);
        int count = 0;
        double sum = 0;
        while (phraseIter.hasNext()) {
            Pair<String, PrunedCounts<Integer>> pair = phraseIter.next();
            long hash = hashCode(lang, pair.getLeft());
            try {
                double p = 1.0 * pair.getRight().getTotal() / counts.get(hash);
                count++;
                sum += p;
//                System.out.println(String.format("inserting values into db: %s, %f", pair.getLeft, p));
                String key = lang.getLangCode() + ":" + pair.getLeft();
                db.put(key, p);
            } catch (IOException e) {
                throw new DaoException(e);
            }
        }

        if (count != 0) {
            LOG.info(String.format(
                    "Inserted link probabilities for %d anchors with mean probability %.4f",
                    count, sum/count));
        }
    }

    private void processPage(TLongIntMap counts, RawPage page) {
        Language lang = page.getLanguage();
        NGramCreator nGramCreator = new NGramCreator();
        StringTokenizer tokenizer = new StringTokenizer();
        for (Token sentence : tokenizer.getSentenceTokens(lang, page.getPlainText())) {
            List<Token> words = tokenizer.getWordTokens(lang, sentence);
            for (Token ngram : nGramCreator.getNGramTokens(words, minGram, maxGram)) {
                String phrase = normalizer.normalize(lang, ngram.getToken());
                long hash = hashCode(lang, phrase);
                synchronized (counts) {
                    if (counts.containsKey(hash)) {
                        counts.adjustValue(hash, 1);
                    }
                }
            }
        }
    }

    private long hashCode(Language lang, String string) {
        return WpStringUtils.longHashCode(lang.getLangCode() + ":" + string);
    }

    public static class Provider extends org.wikibrain.conf.Provider<LinkProbabilityDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class<LinkProbabilityDao> getType() {
            return LinkProbabilityDao.class;
        }

        @Override
        public String getPath() {
            return "phrases.linkProbability";
        }

        @Override
        public LinkProbabilityDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            LanguageSet ls = getConfigurator().get(LanguageSet.class);
            File path = new File(config.getString("path"));
            String pageName = config.hasPath("rawPageDao") ? config.getString("rawPageDao") : null;
            String phraseName = config.hasPath("phraseAnalyzer") ? config.getString("phraseAnalyzer") : null;
            RawPageDao rpd = getConfigurator().get(RawPageDao.class, pageName);
            PhraseAnalyzer pa = getConfigurator().get(PhraseAnalyzer.class, phraseName);
            if (!(pa instanceof AnchorTextPhraseAnalyzer)) {
                throw new ConfigurationException("LinkProbabilityDao's phraseAnalyzer must be an AnchorTextPhraseAnalyzer");
            }
            PhraseAnalyzerDao pad = ((AnchorTextPhraseAnalyzer)pa).getDao();
            try {
                return new LinkProbabilityDao(path, ls, rpd, pad);
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
