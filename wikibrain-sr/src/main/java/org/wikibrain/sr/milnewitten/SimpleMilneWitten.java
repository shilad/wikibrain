package org.wikibrain.sr.milnewitten;

import com.typesafe.config.Config;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalLinkDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalLink;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.phrases.AnchorTextPhraseAnalyzer;
import org.wikibrain.phrases.PhraseAnalyzer;
import org.wikibrain.phrases.PrunedCounts;
import org.wikibrain.sr.SRMetric;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.dataset.Dataset;
import org.wikibrain.sr.normalize.Normalizer;
import org.wikibrain.sr.utils.SimUtils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Shilad Sen
 */
public class SimpleMilneWitten implements SRMetric {
    private final String name;
    private final Language language;
    private final LocalPageDao pageDao;
    private final LocalLinkDao linkDao;
    private final AnchorTextPhraseAnalyzer phraseAnalyzer;
    private final int numArticles;
    private File dataDir;

    public SimpleMilneWitten(String name, Language language, LocalPageDao pageDao, LocalLinkDao linkDao, AnchorTextPhraseAnalyzer phraseAnalyzer) throws DaoException {
        this.name = name;
        this.language = language;
        this.pageDao = pageDao;
        this.linkDao = linkDao;
        this.phraseAnalyzer = phraseAnalyzer;
        this.numArticles = pageDao.getCount(
                new DaoFilter()
                        .setLanguages(language)
                        .setDisambig(false)
                        .setRedirect(false)
                        .setNameSpaces(NameSpace.ARTICLE));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Language getLanguage() {
        return language;
    }

    @Override
    public File getDataDir() {
        return dataDir;
    }

    @Override
    public void setDataDir(File dir) {
        this.dataDir = dir;
    }

    @Override
    public SRResult similarity(int pageId1, int pageId2, boolean explanations) throws DaoException {
        double s1 = googleInlink(pageId1, pageId2);
        double s2 = cosineOutlink(pageId1, pageId2);

        return new SRResult(0.5 * s1 + 0.5 * s2);
    }

    private TIntSet getInlinks(int pageId1) throws DaoException {
        TIntSet inlinks = new TIntHashSet();
        for (LocalLink ll : linkDao.getLinks(language, pageId1, false)) {
            inlinks.add(ll.getSourceId());
        }
        return inlinks;
    }
    private TIntSet getOutlinks(int pageId1) throws DaoException {
        TIntSet outlinks = new TIntHashSet();
        for (LocalLink ll : linkDao.getLinks(language, pageId1, true)) {
            outlinks.add(ll.getDestId());
        }
        return outlinks;
    }

    private double googleInlink(int pageId1, int pageId2) throws DaoException {
        TIntSet inlinks1 = getInlinks(pageId1);
        TIntSet inlinks2 = getInlinks(pageId2);

        if (inlinks1.isEmpty() && inlinks2.isEmpty()) {
            return 0.0;
        }
        int a = inlinks1.size();
        int b = inlinks2.size();
        TIntSet intersection = new TIntHashSet(inlinks1.toArray());
        intersection.retainAll(inlinks2);
        int ab = intersection.size();

        return 1.0 - (
                (Math.log(Math.max(a, b)) - Math.log(ab))
                / (Math.log(numArticles) - Math.log(Math.min(a, b)))
        );
    }

    private double cosineOutlink(int pageId1, int pageId2) throws DaoException {
        TIntSet outlinks1 = getOutlinks(pageId1);
        TIntSet outlinks2 = getOutlinks(pageId2);

        TIntFloatMap v1 = makeOutlinkVector(outlinks1);
        TIntFloatMap v2 = makeOutlinkVector(outlinks2);
        if (v1.isEmpty() || v2.isEmpty()) {
            return 0.0;
        }
        return SimUtils.cosineSimilarity(v1, v2);
    }

    private int getNumLinks(int wpId) throws DaoException {
        return linkDao.getCount(new DaoFilter().setLanguages(language).setSourceIds(wpId));
    }

    private TIntFloatMap makeOutlinkVector(TIntSet links) throws DaoException {
        TIntFloatMap vector = new TIntFloatHashMap();
        for (int wpId : links.toArray()) {
            vector.put(wpId, (float) Math.log(1.0 * numArticles / getNumLinks(wpId)));
        }
        return vector;
    }

    @Override
    public SRResult similarity(String phrase1, String phrase2, boolean explanations) throws DaoException {
        LinkedHashMap<LocalId, Float> candidates1 = phraseAnalyzer.resolve(language, phrase1, 100);
        LinkedHashMap<LocalId, Float> candidates2 = phraseAnalyzer.resolve(language, phrase2, 100);
        if (candidates1 == null || candidates2 == null) {
            return null;
        }

        double highestScore = Double.NEGATIVE_INFINITY;
        for (LocalId lid1 : candidates1.keySet()) {
            for (LocalId lid2 : candidates2.keySet()) {
                double score = similarity(lid1.getId(), lid2.getId(), false).getScore();
                if (score > highestScore) {
                    highestScore = score;
                }
            }
        }

        double result = 0.0;
        double highestPop = Double.NEGATIVE_INFINITY;

        for (LocalId lid1 : candidates1.keySet()) {
            for (LocalId lid2 : candidates2.keySet()) {
                double pop = candidates1.get(lid1) * candidates2.get(lid2);
                double score = similarity(lid1.getId(), lid2.getId(), false).getScore();
                if (score >= 0.4 * highestScore && pop >= highestPop) {
                    highestPop = pop;
                    result = score;
                }
            }
        }

        int n1 = getPhraseCount(phrase1 + " " + phrase2);
        int n2 = getPhraseCount(phrase2 + " " + phrase1);
        if (n1 + n2 > 0) {
            result += Math.log(n1 + n2 + 1) / 10;
        }

        return new SRResult(result);
    }

    private int getPhraseCount(String phrase) throws DaoException {
        PrunedCounts<Integer> pages = phraseAnalyzer.getDao().getPhraseCounts(language, phrase, 1);
        if (pages == null) {
            return 0;
        } else {
            return pages.getTotal();
        }
    }

    @Override
    public SRResultList mostSimilar(int pageId, int maxResults) throws DaoException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SRResultList mostSimilar(int pageId, int maxResults, TIntSet validIds) throws DaoException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SRResultList mostSimilar(String phrase, int maxResults) throws DaoException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SRResultList mostSimilar(String phrase, int maxResults, TIntSet validIds) throws DaoException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write() throws IOException {}

    @Override
    public void read() {}

    @Override
    public void trainSimilarity(Dataset dataset) throws DaoException {
    }

    @Override
    public void trainMostSimilar(Dataset dataset, int numResults, TIntSet validIds) {
    }

    @Override
    public boolean similarityIsTrained() {
        return true;
    }

    @Override
    public boolean mostSimilarIsTrained() {
        return false;
    }

    @Override
    public double[][] cosimilarity(int[] wpRowIds, int[] wpColIds) throws DaoException {
        throw new UnsupportedOperationException();
    }

    @Override
    public double[][] cosimilarity(String[] rowPhrases, String[] colPhrases) throws DaoException {
        throw new UnsupportedOperationException();
    }

    @Override
    public double[][] cosimilarity(int[] ids) throws DaoException {
        throw new UnsupportedOperationException();
    }

    @Override
    public double[][] cosimilarity(String[] phrases) throws DaoException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Normalizer getMostSimilarNormalizer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMostSimilarNormalizer(Normalizer n) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Normalizer getSimilarityNormalizer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSimilarityNormalizer(Normalizer n) {
        throw new UnsupportedOperationException();
    }

    public static class Provider extends org.wikibrain.conf.Provider<SRMetric> {

        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class<SRMetric> getType() {
            return SRMetric.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.local";
        }

        @Override
        public SRMetric get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("simplemilnewitten")) {
                return null;
            }
            if (runtimeParams == null || !runtimeParams.containsKey("language")){
                throw new IllegalArgumentException("SimpleMilneWitten requires 'language' runtime parameter.");
            }
            Language language = Language.getByLangCode(runtimeParams.get("language"));

            try {
                return new SimpleMilneWitten(
                        name,
                        language,
                        getConfigurator().get(LocalPageDao.class),
                        getConfigurator().get(LocalLinkDao.class),
                        (AnchorTextPhraseAnalyzer) getConfigurator().get(PhraseAnalyzer.class, "anchortext")
                );
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }
}
