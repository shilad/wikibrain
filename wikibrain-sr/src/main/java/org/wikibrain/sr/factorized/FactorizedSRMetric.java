package org.wikibrain.sr.factorized;

import com.typesafe.config.Config;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.matrix.SparseMatrix;
import org.wikibrain.matrix.SparseMatrixRow;
import org.wikibrain.matrix.SparseMatrixWriter;
import org.wikibrain.matrix.ValueConf;
import org.wikibrain.sr.BaseSRMetric;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.dataset.Dataset;
import org.wikibrain.sr.disambig.Disambiguator;
import org.wikibrain.sr.utils.Leaderboard;
import org.wikibrain.utils.ParallelForEach;
import org.wikibrain.utils.Procedure;
import org.wikibrain.utils.WbMathUtils;
import org.wikibrain.utils.WpIOUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class FactorizedSRMetric extends BaseSRMetric {
    private static final Logger LOG = Logger.getLogger(FactorizedSRMetric.class.getName());

    private int rank = 10;
    private TIntObjectMap<float[]> vectors;
    private Factorizer factorizer;
    private SRMetric baseMetric;
    private TIntSet conceptIds;

    public FactorizedSRMetric(String name, Language language, LocalPageDao dao, Disambiguator disambig, SRMetric baseMetric, Factorizer factorizer) {
        super(name, language, dao, disambig);
        this.baseMetric = baseMetric;
        this.factorizer = factorizer;
    }

    @Override
    public SRConfig getConfig() {
        return new SRConfig();
    }

    @Override
    public SRResult similarity(int pageId1, int pageId2, boolean explanations) throws DaoException {
        float v1[] = vectors.get(pageId1);
        float v2[] = vectors.get(pageId2);

        if (v1 == null || v2 == null) {
            return new SRResult(Double.NaN);
        } else {
            return new SRResult(simFn(v1, v2));   // cosine sim or dot?
        }
    }

    @Override
    public SRResultList mostSimilar(final int pageId, int maxResults, final TIntSet validIds) throws DaoException {
        final float [] v1 = vectors.get(pageId);
        if (v1 == null) {
            return null;
        }
        final Leaderboard top = new Leaderboard(maxResults);
        vectors.forEachEntry(new TIntObjectProcedure<float[]>() {
            @Override
            public boolean execute(int pageId2, float[] v2) {
                if (validIds == null || validIds.contains(pageId2)) {
                    top.tallyScore(pageId, simFn(v1, v2));
                }
                return true;
            }
        });
        return top.getTop();
    }

    @Override
    public void write() throws IOException {
        super.write();
        WpIOUtils.writeObjectToFile(new File(getDataDir(), "vectors.bin"), vectors);
    }


    @Override
    public void read() throws IOException{
        super.read();
        File f = new File(getDataDir(), "vectors.bin");
        if (f.isFile()) {
            vectors = (TIntObjectMap<float[]>) WpIOUtils.readObjectFromFile(f);
        }
    }
    @Override
    public synchronized void trainSimilarity(Dataset dataset) throws DaoException {
        TIntIntMap mapping = new TIntIntHashMap();
        SparseMatrix matrix = createMostSimilarCache(mapping);
        float [][] packedVectors = factorizer.factorize(matrix, rank);
        vectors = new TIntObjectHashMap<float[]>();
        for (int id : mapping.keys()) {
            vectors.put(id, packedVectors[mapping.get(id)]);
        }
        super.trainSimilarity(dataset);
    }

    private SparseMatrix createMostSimilarCache(final TIntIntMap mapping) {
        try {
            // Write out adjacency matrix
            File mostSimilarCache = File.createTempFile("wikibrain-cosims", "matrix");
            mostSimilarCache.deleteOnExit();
            mostSimilarCache.delete();
            final ValueConf vconf = new ValueConf();
            final SparseMatrixWriter writer = new SparseMatrixWriter(mostSimilarCache, vconf);
            Iterable<LocalId> ids = getLocalPageDao().getIds(DaoFilter.normalPageFilter(getLanguage()));
            ParallelForEach.iterate(ids.iterator(), new Procedure<LocalId>() {
                @Override
                public void call(LocalId sparseId) throws Exception {
                    SRResultList rl = baseMetric.mostSimilar(sparseId.getId(), 1000, null);
                    if (rl == null) {
                        return;
                    }
                    TIntFloatMap packedRow = new TIntFloatHashMap();
                    int denseId;
                    synchronized (mapping) {
                        denseId = packId(mapping, sparseId.getId());
                        for (SRResult r : rl) {
                            int id2= packId(mapping, r.getId());
                            packedRow.put(id2, (float) r.getScore());
                        }
                    }
                    SparseMatrixRow row = new SparseMatrixRow(vconf, denseId, rl.asTroveMap());
                    writer.writeRow(row);
                }
            });
            writer.finish();
            return new SparseMatrix(mostSimilarCache);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (DaoException e) {
            throw new RuntimeException(e);
        }
    }

    private int packId(TIntIntMap mapping, int sparseId) {
        if (mapping.containsKey(sparseId)) {
            return mapping.get(sparseId);
        }
        int denseId = mapping.size();
        mapping.put(sparseId, denseId);
        return denseId;
    }

    private double simFn(float v1[], float v2[]) {
        return WbMathUtils.dot(v1, v2);
    }

    public void setConcepts(File file) throws IOException {
        conceptIds = new TIntHashSet();
        if (!file.isFile()) {
            LOG.warning("concept path " + file + " not a file; defaulting to all concepts");
            return;
        }
        for (String wpId : FileUtils.readLines(file)) {
            conceptIds.add(Integer.valueOf(wpId));
        }
        LOG.warning("installed " + conceptIds.size() + " concepts for " + getLanguage());
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    @Override
    public boolean similarityIsTrained() {
        return super.similarityIsTrained() && vectors != null && vectors.size() > 0;
    }

    @Override
    public boolean mostSimilarIsTrained() {
        return super.mostSimilarIsTrained() && vectors != null && vectors.size() > 0;
    }

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
            if (!config.getString("type").equals("factorized")) {
                return null;
            }
            if (runtimeParams == null || !runtimeParams.containsKey("language")) {
                throw new IllegalArgumentException("Monolingual SR Metric requires 'language' runtime parameter");
            }
            Language language = Language.getByLangCode(runtimeParams.get("language"));
            SRMetric baseMetric = getConfigurator().get(
                    SRMetric.class, config.getString("baseMetric"),
                    "language", language.getLangCode());
            Disambiguator dab = getConfigurator().get(Disambiguator.class, config.getString("disambiguator"), "language", language.getLangCode());
            Factorizer factorizer = new FunkFactorizer();
            FactorizedSRMetric fm = new FactorizedSRMetric(
                    name,
                    language,
                    getConfigurator().get(LocalPageDao.class),
                    dab,
                    baseMetric,
                    factorizer
            );
            try {
                fm.setConcepts(FileUtils.getFile(
                        config.getString("concepts"),
                        language.getLangCode() + ".txt"));
            } catch (IOException e) {
                throw new ConfigurationException(e);
            }
            if (config.hasPath("rank")) {
                fm.setRank(config.getInt("rank"));
            }
            configureBase(getConfigurator(), fm, config);
            return fm;
        }
    }
}
