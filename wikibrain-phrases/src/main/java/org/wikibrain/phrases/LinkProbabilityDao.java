package org.wikibrain.phrases;

import com.typesafe.config.Config;
import gnu.trove.map.TLongFloatMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongFloatHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.wikibrain.core.nlp.StringTokenizer;
import org.wikibrain.core.nlp.Token;
import org.wikibrain.utils.*;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Shilad Sen
 *
 * Calculates the probability that a section of text is hyperlinked. Useful for
 * detecting entities.
 */
public class LinkProbabilityDao {
    private static final Logger LOG = LoggerFactory.getLogger(LinkProbabilityDao.class);

    private final File path;
    private final Language lang;
    private final RawPageDao pageDao;
    private final PhraseAnalyzerDao phraseDao;
    private final StringNormalizer normalizer;

    private ObjectDb<Double> db;
    private TLongFloatMap cache = null;
    private TLongSet subGrams = null;


    public LinkProbabilityDao(File path, Language lang, RawPageDao pageDao, PhraseAnalyzerDao phraseDao) throws DaoException {
        this.path = path;
        this.lang = lang;
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
            LOG.warn("path " + path + " does not exist... LinkProbabilityDao will not work until build() is called.");
        }
    }

    /**
     * If true, create a memory cache that stores a 64-bit hashcode for each word.
     * If the cache doesn't exist, it will be built.
     * @param useCache
     */
    public void useCache(boolean useCache) {
        if (!useCache) {
            this.cache = null;
            return;
        } else if (db == null) {
            this.cache = new TLongFloatHashMap();   // build cache later
            return;
        }

        File fp = new File(path + "-phrase-cache.bin");
        File fsg = new File(path + "-subgram-cache.bin");
        long tstamp = 0;
        try {
            Double doubleTstamp = db.get("tstamp");
            if (doubleTstamp == null) {
                tstamp = System.currentTimeMillis();
                db.put("tstamp", 1.0 * tstamp);
                db.flush();
            } else {
                tstamp = db.get("tstamp").longValue();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        if (fp.isFile() && fp.lastModified() > tstamp
                && fsg.isFile() && fsg.lastModified() > tstamp) {
            try {
                cache = (TLongFloatMap) WpIOUtils.readObjectFromFile(fp);
                subGrams = (TLongSet) WpIOUtils.readObjectFromFile(fsg);
                LOG.info("Using up-to-date link probability cache files {} and {}", fp, fsg);
                return;
            } catch (IOException e) {
                LOG.warn("Using link probability dao cache failed: ", e);
            }
        }
        LOG.info("building cache...");
        TLongFloatMap cache = new TLongFloatHashMap();
        Iterator<Pair<String, Double>> iter = db.iterator();
        TLongSet subgrams = new TLongHashSet();
        while (iter.hasNext()) {
            Pair<String, Double> entry = iter.next();
            if (entry.getKey().equalsIgnoreCase("tstamp")) {
                // do nothing...
            } else if (entry.getKey().startsWith(":s:")) {
                long hash = Long.valueOf(entry.getKey().substring(3));
                subgrams.add(hash);
            } else {
                String tokens[] = entry.getKey().split(":", 2);
                Language lang = Language.getByLangCode(tokens[0]);
                long hash = hashCode(tokens[1]);
                cache.put(hash, entry.getRight().floatValue());
            }
        }
        this.cache = cache;
        this.subGrams = subgrams;
        LOG.info("created cache with " + cache.size() + " entries and " + subgrams.size() + " subgrams");
        try {
            WpIOUtils.writeObjectToFile(fp, cache);
            WpIOUtils.writeObjectToFile(fsg, subgrams);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Build the cache if it is not already built.
     * @throws DaoException
     */
    public void buildIfNecessary() throws DaoException {
        if (!isBuilt()) build();
    }

    /**
     * @return The language associated with this dao.
     */
    public Language getLang() {
        return lang;
    }


    /**
     * Retrieves the probability a link is linked in Wikipedia.
     * If normalize is true, text normalization is first performed.
     * @param mention
     * @return
     * @throws DaoException
     */
    public double getLinkProbability(String mention) throws DaoException {
        return getLinkProbability(mention, true);
    }

    /**
     * Retrieves the probability a link is linked in Wikipedia.
     * If normalize is true, text normalization is first performed.
     * @param mention
     * @param normalize If true, the text is normalized.
     * @return
     * @throws DaoException
     */
    public double getLinkProbability(String mention, boolean normalize) throws DaoException {
        if (db == null) {
            throw new IllegalStateException("Dao has not yet been built. Call build()");
        }
        String normalizedMention = cleanString(mention, normalize);
        if (cache != null && cache.size() > 0) {
            long hash = hashCode(normalizedMention);
            return cache.containsKey(hash) ? cache.get(hash) : 0.0;
        }
        String key = lang.getLangCode() + ":" + normalizedMention;
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

    /**
     * Rebuilds the link probability dao. Deletes the dao if it currently exists.
     * @throws DaoException
     */
    public synchronized void build() throws DaoException {
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

        subGrams = new TLongHashSet();


        LOG.info("building link probabilities for language " + lang);

        final TLongIntMap counts = new TLongIntHashMap();
        Iterator<String> iter = phraseDao.getAllPhrases(lang);
        StringTokenizer tokenizer = new StringTokenizer();

        while (iter.hasNext()) {
            String phrase = iter.next();
            List<String> words = tokenizer.getWords(lang, phrase);
            StringBuilder buffer = new StringBuilder("");
            long hash = -1;
            for (int i = 0; i < words.size(); i++) {
                if (i > 0) buffer.append(' ');
                buffer.append(words.get(i));
                hash = hashCode(buffer.toString());
                subGrams.add(hash);
            }
            counts.put(hash, 0);
        }
        LOG.info("found " + counts.size() + " unique anchortexts and " + subGrams.size() + " subgrams");

        DaoFilter filter = new DaoFilter()
                .setRedirect(false)
                .setLanguages(lang)
                .setDisambig(false)
                .setNameSpaces(NameSpace.ARTICLE);

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

        int count = 0;
        int misses = 0;
        double sum = 0.0;

        TLongSet completed = new TLongHashSet();
        TLongIntMap linkCounts = getPhraseLinkCounts();

        Iterator<Pair<String, PrunedCounts<Integer>>> phraseIter = phraseDao.getAllPhraseCounts(lang);

        while (phraseIter.hasNext()) {
            Pair<String, PrunedCounts<Integer>> pair = phraseIter.next();
            String phrase = cleanString(pair.getLeft());
            long hash = hashCode(phrase);
            if (completed.contains(hash)) {
                continue;
            }
            completed.add(hash);
            try {
                int numLinks = linkCounts.get(hash);
                int numText = counts.get(hash);
                if (numText == 0) {
                    misses++;
                }
                count++;
                double p = 1.0 * numLinks / (numText + 3.0);  // 3.0 for smoothing
                sum += p;
//                System.out.println(String.format("inserting values into db: %s, %f", pair.getLeft, p));
                db.put(lang.getLangCode() + ":" + phrase, p);
                if (cache != null) {
                    cache.put(hash, (float) p);
                }
            } catch (IOException e) {
                throw new DaoException(e);
            }
        }

        for (long h : subGrams.toArray()) {
            try {
                db.put(":s:" + h, -1.0);
            } catch (IOException e) {
                throw new DaoException(e);
            }
        }

        try {
            db.put("tstamp", 1.0 * System.currentTimeMillis());
        } catch (IOException e) {
            throw new DaoException(e);
        }

        if (count != 0) {
            LOG.info(String.format(
                    "Inserted link probabilities for %d anchors with mean probability %.4f and %d mises",
                    count, sum / count, misses));
        }
        db.flush();
    }


    private void processPage(TLongIntMap counts, RawPage page) {
        Language lang = page.getLanguage();
        StringTokenizer tokenizer = new StringTokenizer();
        StringBuilder buffer = new StringBuilder();
        for (Token sentence : tokenizer.getSentenceTokens(lang, page.getPlainText())) {
            List<Token> words = tokenizer.getWordTokens(lang, sentence);
            for (int i = 0; i < words.size(); i++) {
                buffer.setLength(0);
                for (int j = i; j < words.size(); j++) {
                    if (j > i) {
                        buffer.append(' ');
                    }
                    buffer.append(words.get(j).getToken());
                    String phrase = cleanString(buffer.toString(), true);
                    long hash = hashCode(phrase);
                    if (subGrams.contains(hash)) {
                        synchronized (counts) {
                            if (counts.containsKey(hash)) {
//                                System.out.println("here 1: " + phrase);
                                counts.adjustValue(hash, 1);
                            } else {
//                                System.out.println("here 2: " + phrase);
                            }
                        }
                    } else {
//                        System.out.println("here 3: " + phrase);
                        break;  // no point in going any further...
                    }
                }
            }
        }
    }


    private TLongIntMap getPhraseLinkCounts() {
        Iterator<Pair<String, PrunedCounts<Integer>>> phraseIter = phraseDao.getAllPhraseCounts(lang);
        TLongIntMap counts = new TLongIntHashMap();
        while (phraseIter.hasNext()) {
            Pair<String, PrunedCounts<Integer>> pair = phraseIter.next();
            String phrase = cleanString(pair.getLeft());
            long hash = hashCode(phrase);
            int n = pair.getRight().getTotal();
            counts.adjustOrPutValue(hash, n, n);
        }
        return counts;
    }

    public boolean isBuilt() {
        return (db != null && !db.isEmpty());
    }

    public boolean isSubgram(String phrase, boolean normalize) {
        if (cache == null || subGrams == null) {
            throw new IllegalArgumentException("Subgrams require a cache!");
        }
        String cleaned = cleanString(phrase, normalize);
        long h = hashCode(cleaned);
        return cache.containsKey(h) || subGrams.contains(h);
    }

    private String cleanString(String s) {
        return cleanString(s, false);
    }

    private String cleanString(String s, boolean normalize) {
        if (normalize) s = normalizer.normalize(lang, s);
        StringTokenizer t = new StringTokenizer();
        return StringUtils.join(t.getWords(lang, s), " ");
    }

    static long hashCode(String string) {
        return WpStringUtils.longHashCode2(string);
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
            if (runtimeParams == null || !runtimeParams.containsKey("language")){
                throw new IllegalArgumentException("LinkProbabilityDao requires 'language' runtime parameter.");
            }
            Language language = Language.getByLangCode(runtimeParams.get("language"));
            File path = new File(config.getString("path"), language.getLangCode());
            String pageName = config.hasPath("rawPageDao") ? config.getString("rawPageDao") : null;
            String phraseName = config.hasPath("phraseAnalyzer") ? config.getString("phraseAnalyzer") : null;
            RawPageDao rpd = getConfigurator().get(RawPageDao.class, pageName);
            PhraseAnalyzer pa = getConfigurator().get(PhraseAnalyzer.class, phraseName);
            if (!(pa instanceof AnchorTextPhraseAnalyzer)) {
                throw new ConfigurationException("LinkProbabilityDao's phraseAnalyzer must be an AnchorTextPhraseAnalyzer");
            }
            PhraseAnalyzerDao pad = ((AnchorTextPhraseAnalyzer)pa).getDao();
            try {
                return new LinkProbabilityDao(path, language, rpd, pad);
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }

}
