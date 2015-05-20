package org.wikibrain.sr.phrasesim;

import com.typesafe.config.Config;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TLongFloatMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.procedure.TIntFloatProcedure;
import gnu.trove.procedure.TLongFloatProcedure;
import gnu.trove.set.TIntSet;
import org.apache.commons.io.FileUtils;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.StringNormalizer;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.dataset.Dataset;
import org.wikibrain.sr.normalize.IdentityNormalizer;
import org.wikibrain.sr.normalize.Normalizer;
import org.wikibrain.sr.normalize.PercentileNormalizer;
import org.wikibrain.sr.utils.Leaderboard;
import org.wikibrain.sr.vector.SparseVectorSRMetric;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WpIOUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This metric is intended to support very fast SR for a known (expandable) set of phrases.
 * This metric ONLY operates on phrases, not on pages (TODO: fix this).
 *
 * The SR metric must be made aware of phrases using the addPhrase() method.
 * All phrases have an application-generated integer ID associated with them.
 * "Unkonwn" phrases (e.g. those not added) will not be returned by mostSimilar() and
 * will fail for, e.g. similarity() and cosimilarity().
 *
 * SR methods (e.g. similarity, cosimilarity, mostSimilar) that typically take or return
 * local article ids will instead take or return phrase ids. For example, mostSimilar()
 * returns the scores and ids associated with known phrases.
 *
 * Phrase are represented as vectors, and cached in two methods. First, a full cosimilarity
 * matrix is maintained, ensuring that any all SR methods on existing phrases are fast.
 * Second, an inverted index for the vector representations is maintained so that
 * all cosimilarities for a new phrase can be calculated very quickly.
 *
 * The universe of known phrases and associated data structures is serialized dynamically
 * to files in the specific data directory. However, the full cosimilarity matrix is only
 * written out when the write() method (or flushCosimilarity method) is called.
 *
 * The normalizer should be retrained for internal phrases (using trainNormalizer())
 * periodically. It initially defaults to the "identity" normalizer.
 *
 * This means that space and time complexities are O(N^2) for a new phrase - to be specific,
 * 4 bytes are needed for each element in the cosimilarity matrix. In exchange for this,
 * similarity is O(1), cosimilarity is O(m*n) for m phrases by n phrases, and mostSimilar
 * is O(n). The complexity of addPhrase scales with the sparsity of the feature vector matrix.
 * To be specific, the complexity of addPhrase(phrase) is linear in the number of non-zero
 * cells in the full (all-phrase) feature matrix for each of the phrase's features.
 *
 * All elements of this metric are thread-safe.
 *
 * @author Shilad Sen
 */
public class KnownPhraseSim implements SRMetric {
    private static final Logger LOGGER = LoggerFactory.getLogger(KnownPhraseSim.class);

    private final StringNormalizer stringNormalizer;
    private final HTreeMap<Object, Object> db;
    private final PhraseCreator creator;
    private final Language language;
    private final File dir;
    private final String name;

    private Normalizer scoreNormalizer = new IdentityNormalizer();

    // Regular index
    private ConcurrentHashMap<String, KnownPhrase> byPhrase;
    private ConcurrentHashMap<Integer, KnownPhrase> byId;

    // Keeps an inverted index for fast mostSimilar performance
    private ConcurrentHashMap<Long, TIntFloatMap> invertedIndex = new ConcurrentHashMap<Long, TIntFloatMap>();

    // Caches the full cosimilarity matrix.
    private CosimilarityMatrix cosim = new CosimilarityMatrix();

    private DB phraseDb;

    public KnownPhraseSim(Language language, PhraseCreator creator, File dir, StringNormalizer stringNormalizer) throws IOException {
        this("known-phrase-sim", language, creator, dir, stringNormalizer);
    }

    public KnownPhraseSim(String name, Language language, PhraseCreator creator, File dir, StringNormalizer stringNormalizer) throws IOException {
        this.name = name;
        this.language = language;
        this.creator = creator;
        this.stringNormalizer = stringNormalizer;
        this.dir = dir;
        this.dir.mkdirs();
        this.phraseDb = DBMaker
                .newFileDB(new File(dir, "phrases.mapdb"))
                .mmapFileEnable()
                .transactionDisable()
                .asyncWriteEnable()
                .asyncWriteFlushDelay(100)
                .make();
        this.db = phraseDb.getHashMap("phrases");
        this.readPhrases();
        this.readCosimilarity();

        File f = new File(dir, "scoreNormalizer.bin");
        if (f.isFile()) {
            scoreNormalizer = (Normalizer) WpIOUtils.readObjectFromFile(f);
        }
    }

    @Override
    public void read() {
        throw new UnsupportedOperationException("Metric cannot be re-read after creation");
    }

    /**
     * Write simply flushes the cache. All other writes happen asynchronously.
     * @throws IOException
     */
    @Override
    public void write() throws IOException {
        flushCosimilarity();
    }

    private void readCosimilarity() throws IOException {
        File f = new File(dir, "cosimilarity.bin");
        try {
            this.cosim = (CosimilarityMatrix) WpIOUtils.readObjectFromFile(f);
        } catch (Exception e) {
            LOGGER.info("Reading cosim file " + f + " failed... rebuilding it from scratch");
            cosim = new CosimilarityMatrix();
        }
        final TIntSet built = cosim.getCompleted();
        ParallelForEach.loop(byId.values(), new Procedure<KnownPhrase>() {
            @Override
            public void call(KnownPhrase p) throws Exception {
                if (!built.contains(p.getId())) {
                    SRResultList neighbors = indexedMostSimilar(p.getVector(), byId.size(), null);
                    cosim.update(p.getId(), neighbors);
                }
            }
        });
    }

    public void flushCosimilarity() throws IOException {
        WpIOUtils.writeObjectToFile(new File(dir, "cosimilarity.bin"), cosim);
        db.getEngine().commit();
    }

    private void readPhrases() {
        byId = new ConcurrentHashMap<Integer, KnownPhrase>();
        byPhrase = new ConcurrentHashMap<String, KnownPhrase>();
        for (Map.Entry entry : db.entrySet()) {
            String key = (String) entry.getKey();
            KnownPhrase val = (KnownPhrase) entry.getValue();
            if (!key.equals(val.getNormalizedPhrase())) {
                throw new IllegalStateException();
            }
            byId.put(val.getId(), val);
            byPhrase.put(val.getNormalizedPhrase(), val);
            for (String version : val.getVersions()) {
                byPhrase.put(version, val);
            }
            PhraseVector v = val.getVector();
            for (int i = 0; i < v.ids.length; i++) {
                long featureId = v.ids[i];
                float featureVal = v.vals[i];
                invertedIndex.putIfAbsent(featureId, new TIntFloatHashMap());
                TIntFloatMap index = invertedIndex.get(featureId);
                synchronized (index) {
                    invertedIndex.get(featureId).put(val.getId(), featureVal);
                }
            }
        }
    }

    /**
     * Adds a particular phrase to the internal SR model.
     * Multiple calls to add() for the same phrase are safe
     * (the phrase's frequency will be incremented).
     *
     * @param phrase
     * @param id
     */
    public void addPhrase(String phrase, final int id) {
        KnownPhrase ifAbsent = new KnownPhrase(id, phrase, normalize(phrase));
        KnownPhrase old = byPhrase.putIfAbsent(ifAbsent.getNormalizedPhrase(), ifAbsent);
        if (old == null) {
            TLongFloatMap vector = creator.getVector(phrase);
            if (vector == null) {
                return;
            }
            ifAbsent.setVector(new PhraseVector(vector));
            byId.put(id, ifAbsent);
            db.put(ifAbsent.getNormalizedPhrase(), new KnownPhrase(ifAbsent));
            vector.forEachEntry(new TLongFloatProcedure() {
                @Override
                public boolean execute(long k, float v) {
                    invertedIndex.putIfAbsent(k, new TIntFloatHashMap());
                    TIntFloatMap index = invertedIndex.get(k);
                    synchronized (index) {
                        invertedIndex.get(k).put(id, v);
                    }
                    return true;
                }
            });
            if (cosim != null) {
                SRResultList neighbors = indexedMostSimilar(ifAbsent.getVector(), byId.size(), null);
                cosim.update(id, neighbors);
            }
        } else {
            old.increment(phrase);
            db.put(ifAbsent.getNormalizedPhrase(), new KnownPhrase(old));
        }
    }

    public void rebuild() {
        throw new UnsupportedOperationException();
    }

    /**
     * Trains the normalizer on the existing phrases.
     * The normalizer is (right now) ALWAYS a
     * percentile normalizer to the power of 10.
     *
     * @throws IOException
     */
    public void trainNormalizer() throws IOException {
        Normalizer restored = this.scoreNormalizer;
        this.scoreNormalizer = new IdentityNormalizer();
        try {
            List<Integer> ids = new ArrayList<Integer>(byId.keySet());
            Random random = new Random();
            PercentileNormalizer newNormalizer = new PercentileNormalizer();
            newNormalizer.setPower(10);
            newNormalizer.setSampleSize(100000);
            for (int i = 0; i < 1000; i++) {
                int id = ids.get(random.nextInt(ids.size()));
                for (SRResult r : mostSimilar(id, ids.size())) {
                    newNormalizer.observe(r.getScore());
                }
            }
            newNormalizer.observationsFinished();
            File f = new File(dir, "scoreNormalizer.bin");
            WpIOUtils.writeObjectToFile(f, newNormalizer);
            restored = newNormalizer;
        } finally {
            this.scoreNormalizer = restored;
        }
    }

    /**
     * Returns the phrase associated with a particular id (or null).
     * @param id
     * @return
     */
    public String getPhrase(int id) {
        if (byId.containsKey(id)) {
            return byId.get(id).getCanonicalVersion();
        }
        return null;
    }

    /**
     *
     * @param phrase
     * @return
     */
    public Integer getId(String phrase) {
        KnownPhrase kp = byPhrase.get(normalize(phrase));
        if (kp == null) {
            return null;
        } else {
            return kp.getId();
        }
    }

    /**
     * Return the normalized (i.e. canonical) string associated with a phrase.
     * @param phrase
     * @return
     */
    public String normalize(String phrase) {
        return stringNormalizer.normalize(language, phrase);
    }

    @Override
    public SRResult similarity(int id1, int id2, boolean explanations) {
        return new SRResult(cosim.similarity(id1, id2));
    }

    @Override
    public SRResult similarity(String phrase1, String phrase2, boolean explanations) throws DaoException {
        Integer id1 = getId(phrase1);
        Integer id2 = getId(phrase2);
        if (id1 == null || id2 == null) {
            return new SRResult(Double.NaN);
        }
        return similarity(id1, id2, explanations);
    }

    @Override
    public SRResultList mostSimilar(String phrase, int maxResults, TIntSet candidateIds) {
        Integer id = getId(phrase);
        if (id == null) {
            return null;
        } else {
            return mostSimilar(id, maxResults, candidateIds);
        }
    }

    @Override
    public SRResultList mostSimilar(String phrase, int maxResults) {
        return mostSimilar(phrase, maxResults, null);
    }

    @Override
    public SRResultList mostSimilar(int id, int maxResults, TIntSet candidateIds) {
        KnownPhrase p = byId.get(id);
        if (p == null) {
            return null;
        }
        SRResultList results;
        if (cosim == null) {
            PhraseVector v1 = p.getVector();
            if (candidateIds != null && candidateIds.size() < 10) {
                return mostSimilar(v1, maxResults, candidateIds);
            } else {
                return indexedMostSimilar(v1, maxResults, candidateIds);
            }
        } else {
            results = cosim.mostSimilar(id, maxResults, candidateIds);
        }
        return scoreNormalizer.normalize(results);
    }

    private SRResultList indexedMostSimilar(PhraseVector v1, int maxResults, TIntSet candidateIds) {
        final TIntDoubleHashMap dots = new TIntDoubleHashMap(maxResults * 5);

        for (int i = 0; i < v1.ids.length; i++) {
            long featureId = v1.ids[i];
            final float featureVal = v1.vals[i];
            TIntFloatMap index = invertedIndex.get(featureId);
            if (index == null) continue;
            synchronized (index) {
                index.forEachEntry(new TIntFloatProcedure() {
                    @Override
                    public boolean execute(int id, float val) {
                        dots.adjustOrPutValue(id, val * featureVal, val * featureVal);
                        return true;
                    }
                });
            }
        }

        final Leaderboard leaderboard = new Leaderboard(maxResults);
        double l1 = v1.norm2();
        int keys[] = dots.keys();
        for (int i = 0; i < keys.length; i++) {
            int id = keys[i];
            double l2 = byId.get(id).getVector().norm2();
            double dot = dots.get(id);
            double sim = dot / (l1 * l2);
            leaderboard.tallyScore(id, sim);
        }

        return leaderboard.getTop();
    }

    private SRResultList mostSimilar(PhraseVector v1, int maxResults, TIntSet candidateIds) {
        final Leaderboard top = new Leaderboard(maxResults);
        if (candidateIds != null) {
            for (int id2 : candidateIds.toArray()) {
                KnownPhrase p2 = byId.get(id2);
                if (p2 != null) {
                    top.tallyScore(id2, v1.cosineSim(p2.getVector()));
                }
            }
        } else {
            for (KnownPhrase p2 : byId.values()) {
                top.tallyScore(p2.getId(), v1.cosineSim(p2.getVector()));
            }
        }
        return top.getTop();
    }

    @Override
    public SRResultList mostSimilar(int id, int maxResults) {
        return mostSimilar(id, maxResults, null);
    }

    @Override
    public double[][] cosimilarity(String rows[], String columns[]) {
        int rowIds[] = new int[rows.length];
        for (int i = 0; i < rowIds.length; i++) {
            rowIds[i] = getId(rows[i]);
        }
        int colIds[] = new int[columns.length];
        for (int i = 0; i < colIds.length; i++) {
            colIds[i] = getId(columns[i]);
        }
        return cosimilarity(rowIds, colIds);
    }

    @Override
    public double[][] cosimilarity(int[] ids) throws DaoException {
        return new double[0][];
    }

    @Override
    public double[][] cosimilarity(String[] phrases) throws DaoException {
        return new double[0][];
    }

    public float[] getPhraseVector(String phrase) {
        Integer id = getId(phrase);
        if (id == null) {
            return null;
        } else {
            return getPhraseVector(id);
        }
    }

    public float[] getPhraseVector(int id) {
        return cosim.getVector(id);
    }

    @Override
    public double[][] cosimilarity(int rows[], int columns[]) {
        double cosims[][] = new double[rows.length][columns.length];
        if (cosim != null) {
            return cosim.cosimilarity(rows, columns);
        }
        List<PhraseVector> colVectors = new ArrayList<PhraseVector>(columns.length);
        for (int i = 0; i < columns.length; i++) {
            KnownPhrase kp = byId.get(columns[i]);
            colVectors.add(kp == null ? null : kp.getVector());
        }
        for (int i = 0; i < rows.length; i++) {
            KnownPhrase kp = byId.get(columns[i]);
            if (kp == null) {
                continue;   // leave sims as their default value of 0.0
            }
            PhraseVector v1 = kp.getVector();
            for (int j = 0; j < columns.length; j++) {
                PhraseVector v2 = colVectors.get(j);
                if (v2 != null) {
                    cosims[i][j] = scoreNormalizer.normalize(v1.cosineSim(v2));
                }
            }
        }
        return cosims;
    }

    @Override public String getName() { return name; }
    @Override public Language getLanguage() { return language; }

    @Override public File getDataDir() { return dir; }
    @Override public void setDataDir(File dir) { throw new UnsupportedOperationException(); }

    public Normalizer getScoreNormalizer() {
        return scoreNormalizer;
    }
    @Override public Normalizer getMostSimilarNormalizer() { return scoreNormalizer; }
    @Override public void setMostSimilarNormalizer(Normalizer n) { throw new UnsupportedOperationException(); }
    @Override public Normalizer getSimilarityNormalizer() { return scoreNormalizer; }
    @Override public void setSimilarityNormalizer(Normalizer n) { throw new UnsupportedOperationException(); }

    @Override public void trainSimilarity(Dataset dataset) {}
    @Override public void trainMostSimilar(Dataset dataset, int numResults, TIntSet validIds) {}
    @Override public boolean similarityIsTrained() { return false; }
    @Override public boolean mostSimilarIsTrained() { return false; }


    public static class Provider extends org.wikibrain.conf.Provider<SRMetric> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return SRMetric.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.local";
        }

        @Override
        public SRMetric get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("knownphrase")) {
                return null;
            }
            if (runtimeParams == null || !runtimeParams.containsKey("language")) {
                throw new IllegalArgumentException("Monolingual SR Metric requires 'language' runtime parameter");
            }
            Language language = Language.getByLangCode(runtimeParams.get("language"));

            List<String> names = config.getStringList("metrics");
            SparseVectorSRMetric[] metrics = new SparseVectorSRMetric[names.size()];
            for (int i = 0; i < names.size(); i++) {
                metrics[i] = (SparseVectorSRMetric) getConfigurator().get(
                        SRMetric.class, names.get(i),
                        "language", language.getLangCode());
            }
            PhraseCreator creator = new EnsemblePhraseCreator(
                    metrics,
                    toPrimitive(config.getDoubleList("coefficients")));

            String stringNormalizerName = null;
            if (config.hasPath("stringnormalizer")) {
                stringNormalizerName = config.getString("stringnormalizer");
            }
            StringNormalizer normalizer = getConfigurator().get(StringNormalizer.class, stringNormalizerName);

            File dir = FileUtils.getFile(
                    getConfig().getString("sr.metric.path"),
                    name,
                    language.getLangCode());

            try {
                return new KnownPhraseSim(name, language, creator, dir, normalizer);
            } catch (IOException e) {
                throw new ConfigurationException(e);
            }
        }
    }

    private static double[] toPrimitive(List<Double> l) {
        double [] result = new double[l.size()];
        for (int i = 0; i < l.size(); i++) {
            result[i] = l.get(i);
        }
        return result;
    }
}
