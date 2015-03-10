package org.wikibrain.sr.vector;

import com.typesafe.config.Config;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.set.TIntSet;
import org.apache.commons.lang3.ArrayUtils;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.disambig.Disambiguator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Shilad Sen
 */
public class FancyPhraseVectorBasedSRMetric extends SparseVectorSRMetric {
    private static enum PhraseMode {
        GENERATOR,  // try to get phrase vectors from the generator directly
        CREATOR,    // try to get phrase vectors form the phrase vector creator
        BOTH,       // first try the generator, then the creator
        NONE        // don't resolve phrases at all.
    }

    private final PhraseVectorCreator phraseVectorCreator;

    private PhraseMode phraseMode = PhraseMode.BOTH;

    public FancyPhraseVectorBasedSRMetric(String name, Language language, LocalPageDao dao, Disambiguator disambig, SparseVectorGenerator generator, VectorSimilarity similarity, PhraseVectorCreator creator) {
        super(name, language, dao, disambig, generator, similarity);
        this.phraseVectorCreator = creator;
        creator.setMetric(this);
    }


    @Override
    public SRResult similarity(String phrase1, String phrase2, boolean explanations) throws DaoException {
        if (phraseMode == PhraseMode.NONE) {
            return super.similarity(phrase1, phrase2, explanations);
        }
        TIntFloatMap vector1 = null;
        TIntFloatMap vector2 = null;
        // try using phrases directly
        if (phraseMode == PhraseMode.BOTH || phraseMode == PhraseMode.GENERATOR) {
            try {
                vector1 = generator.getVector(phrase1);
                vector2 = generator.getVector(phrase2);
            } catch (UnsupportedOperationException e) {
                // try using other methods
            }
        }
        if ((vector1 == null || vector2 == null)
                &&  (phraseMode == PhraseMode.BOTH || phraseMode == PhraseMode.CREATOR)) {
            if (phraseVectorCreator == null) {
                throw new IllegalStateException("phraseMode is " + phraseMode + " but phraseVectorCreator is null");
            }
            TIntFloatMap vectors[] = phraseVectorCreator.getPhraseVectors(phrase1, phrase2);
            if (vectors != null) {
                vector1 = vectors[0];
                vector2 = vectors[1];
            }
        }
        if (vector1 == null || vector2 == null) {
            // fallback on parent's phrase resolution algorithm
            return super.similarity(phrase1, phrase2, explanations);
        } else {
            SRResult result= new SRResult(similarity.similarity(vector1, vector2));
            if(explanations) {
                result.setExplanations(generator.getExplanations(phrase1, phrase2, vector1, vector2, result));
            }
            return normalize(result);
        }
    }

    @Override
    public SRResultList mostSimilar(String phrase, int maxResults, TIntSet validIds) throws DaoException {
        if (phraseMode == PhraseMode.NONE) {
            return super.mostSimilar(phrase, maxResults, validIds);
        }
        TIntFloatMap vector = null;
        // try using phrases directly
        if (phraseMode == PhraseMode.BOTH || phraseMode == PhraseMode.GENERATOR) {
            try {
                vector = generator.getVector(phrase);
            } catch (UnsupportedOperationException e) {
                // try using other methods
            }
        }
        if (vector == null &&  (phraseMode == PhraseMode.BOTH || phraseMode == PhraseMode.CREATOR)) {
            if (phraseVectorCreator == null) {
                throw new IllegalStateException("phraseMode is " + phraseMode + " but phraseVectorCreator is null");
            }
            vector = phraseVectorCreator.getPhraseVector(phrase);
        }
        if (vector == null) {
            // fall back on parent's phrase resolution algorithm
            return super.mostSimilar(phrase, maxResults, validIds);
        } else {
            try {
                return similarity.mostSimilar(vector, maxResults, validIds);
            } catch (IOException e) {
                throw new DaoException(e);
            }
        }
    }

    /**
     * Calculates the cosimilarity matrix between phrases.
     * First tries to use generator to get phrase vectors directly, but some generators will not support this.
     * Falls back on disambiguating phrase vectors to page ids.
     *
     * @param rowPhrases
     * @param colPhrases
     * @return
     * @throws DaoException
     */
    @Override
    public double[][] cosimilarity(String rowPhrases[], String colPhrases[]) throws DaoException {
        if (rowPhrases.length == 0 || colPhrases.length == 0) {
            return new double[rowPhrases.length][colPhrases.length];
        }
        List<TIntFloatMap> rowVectors = new ArrayList<TIntFloatMap>();
        List<TIntFloatMap> colVectors = new ArrayList<TIntFloatMap>();
        try {
            // Try to use strings directly, but generator may not support them, so fall back on disambiguation
            Map<String, TIntFloatMap> vectors = new HashMap<String, TIntFloatMap>();
            for (String s : ArrayUtils.addAll(rowPhrases, colPhrases)) {
                if (!vectors.containsKey(s)) {
                    vectors.put(s, generator.getVector(s));
                }
            }
            for (String s : rowPhrases) {
                rowVectors.add(vectors.get(s));
            }
            for (String s : colPhrases) {
                colVectors.add(vectors.get(s));
            }
        } catch (UnsupportedOperationException e) {
        }

        // If direct phrase vectors failed, try to disambiguate
        if (rowVectors.isEmpty() || colVectors.isEmpty()) {
            List<String> unique = new ArrayList<String>();
            for (String s : ArrayUtils.addAll(rowPhrases, colPhrases)) {
                if (!unique.contains(s)) {
                    unique.add(s);
                }
            }
            TIntFloatMap[] vectors = phraseVectorCreator.getPhraseVectors(unique.toArray(new String[0]));
            for (String s : rowPhrases) {
                int i = unique.indexOf(s);
                if (i < 0) throw new IllegalStateException();
                rowVectors.add(vectors[i]);
            }
            for (String s : colPhrases) {
                int i = unique.indexOf(s);
                if (i < 0) throw new IllegalStateException();
                colVectors.add(vectors[i]);
            }
        }
        return cosimilarity(rowVectors, colVectors);
    }

    public void setPhraseMode(PhraseMode mode) {
        this.phraseMode = mode;
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
            if (!config.getString("type").equals("fancyphrasevector")) {
                return null;
            }

            if (runtimeParams == null || !runtimeParams.containsKey("language")){
                throw new IllegalArgumentException("Monolingual requires 'language' runtime parameter.");
            }
            Language language = Language.getByLangCode(runtimeParams.get("language"));
            Map<String, String> params = new HashMap<String, String>();
            params.put("language", language.getLangCode());
            SparseVectorGenerator generator = getConfigurator().construct(
                    SparseVectorGenerator.class, null, config.getConfig("generator"), params);
            VectorSimilarity similarity = getConfigurator().construct(
                    VectorSimilarity.class,  null, config.getConfig("similarity"), params);
            FancyPhraseVectorBasedSRMetric sr = new FancyPhraseVectorBasedSRMetric(
                    name,
                    language,
                    getConfigurator().get(LocalPageDao.class,config.getString("pageDao")),
                    getConfigurator().get(Disambiguator.class,config.getString("disambiguator"),"language", language.getLangCode()),
                    generator,
                    similarity,
                    getConfigurator().construct(
                            PhraseVectorCreator.class, null, config.getConfig("phrases"), null)
            );
            if (config.hasPath("phraseMode")) {
                sr.setPhraseMode(PhraseMode.valueOf(config.getString("phraseMode").toUpperCase()));
            }
            configureBase(getConfigurator(), sr, config);
            return sr;
        }

    }
}
