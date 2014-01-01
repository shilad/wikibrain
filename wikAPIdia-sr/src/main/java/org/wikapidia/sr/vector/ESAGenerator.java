package org.wikapidia.sr.vector;

import com.typesafe.config.Config;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.lucene.LuceneSearcher;
import org.wikapidia.lucene.QueryBuilder;
import org.wikapidia.lucene.WikapidiaScoreDoc;
import org.wikapidia.lucene.WpIdFilter;
import org.wikapidia.matrix.SparseMatrix;
import org.wikapidia.sr.Explanation;
import org.wikapidia.sr.SRResult;
import org.wikapidia.sr.SRResultList;
import org.wikapidia.sr.utils.Leaderboard;
import org.wikapidia.sr.utils.SimUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Shilad Sen
 */
public class ESAGenerator implements VectorGenerator {

    private static final Logger LOG = Logger.getLogger(ESAGenerator.class.getName());

    private final LuceneSearcher searcher;
    private final Language language;
    private final LocalPageDao pageDao;

    private Map<Language, WpIdFilter> conceptFilter = new HashMap<Language, WpIdFilter>();

    public ESAGenerator(Language language, LocalPageDao pageDao, LuceneSearcher searcher) {
        this.language = language;
        this.pageDao = pageDao;
        this.searcher = searcher;
    }


    @Override
    public TIntFloatMap getVector(int pageId) throws DaoException {
        int luceneId = searcher.getDocIdFromLocalId(pageId, language);
        if (luceneId < 0) {
            LOG.warning("Unindexed document " + pageId + " in " + language.getEnLangName());
            return new TIntFloatHashMap();
        }
        WikapidiaScoreDoc[] wikapidiaScoreDocs =  getQueryBuilder()
                .setMoreLikeThisQuery(luceneId)
                .search();
        wikapidiaScoreDocs = pruneSimilar(wikapidiaScoreDocs);
        return SimUtils.normalizeVector(expandScores(wikapidiaScoreDocs));

    }

    @Override
    public TIntFloatMap getVector(String phrase) {
        QueryBuilder builder = getQueryBuilder().setPhraseQuery(phrase);
        if (builder.hasQuery()) {
            WikapidiaScoreDoc[] scoreDocs = builder.search();
            scoreDocs = SimUtils.pruneSimilar(scoreDocs);
            return SimUtils.normalizeVector(expandScores(scoreDocs));
        } else {
            LOG.log(Level.WARNING, "Phrase cannot be parsed to get a query. "+phrase);
            return null;
        }
    }

    public void setConcepts(File dir) throws IOException {
        conceptFilter.clear();
        if (!dir.isDirectory()) {
            LOG.warning("concept path " + dir + " not a directory; defaulting to all concepts");
            return;
        }
        for (String file : dir.list()) {
            String langCode = FilenameUtils.getBaseName(file);
            TIntSet ids = new TIntHashSet();
            for (String wpId : FileUtils.readLines(new File(dir, file))) {
                ids.add(Integer.valueOf(wpId));
            }
            conceptFilter.put(Language.getByLangCode(langCode), new WpIdFilter(ids.toArray()));
            LOG.warning("installed " + ids.size() + " concepts for " + langCode);
        }
    }

    @Override
    public List<Explanation> getExplanations(LocalPage page1, LocalPage page2, TIntFloatMap vector1, TIntFloatMap vector2, SRResult result) throws DaoException {
        Leaderboard lb = new Leaderboard(5);    // TODO: make 5 configurable
        for (int id : vector1.keys()) {
            if (vector2.containsKey(id)) {
                lb.insert(id, vector1.get(id) * vector2.get(id));
            }
        }
        SRResultList top = lb.getTop();
        if (top.numDocs() == 0) {
            return Arrays.asList(new Explanation("? and ? share no links", page1, page2));
        }
        top.sortDescending();

        List<Explanation> explanations = new ArrayList<Explanation>();
        for (int i = 0; i < top.numDocs(); i++) {
            LocalPage p = pageDao.getById(language, top.getId(i));
            if (p != null) {
                explanations.add(new Explanation("Both ? and ? have similar text to ?", page1, page2, p));
            }
        }
        return explanations;
    }

    private QueryBuilder getQueryBuilder() {
        QueryBuilder builder = searcher.getQueryBuilderByLanguage(language);
        builder.setResolveWikipediaIds(false);
        WpIdFilter filter = conceptFilter.get(language);
        if (filter != null) {
            builder.addFilter(filter);
        }
        return builder;
    }

    /**
     * Put data in a scoreDoc into a TIntDoubleHashMap
     *
     * @param wikapidiaScoreDocs
     * @return
     */
    private TIntFloatMap expandScores(WikapidiaScoreDoc[] wikapidiaScoreDocs) {
        TIntFloatMap expanded = new TIntFloatHashMap();
        for (WikapidiaScoreDoc wikapidiaScoreDoc : wikapidiaScoreDocs) {
            expanded.put(wikapidiaScoreDoc.luceneId, wikapidiaScoreDoc.score);
        }
        return expanded;
    }

    /**
     * Prune a WikapidiaScoreDoc array.
     * @param wikapidiaScoreDocs array of WikapidiaScoreDoc
     */
    private WikapidiaScoreDoc[] pruneSimilar(WikapidiaScoreDoc[] wikapidiaScoreDocs) {
        if (wikapidiaScoreDocs.length == 0) {
            return wikapidiaScoreDocs;
        }
        int cutoff = wikapidiaScoreDocs.length;
        double threshold = 0.005 * wikapidiaScoreDocs[0].score;
        for (int i = 0, j = 100; j < wikapidiaScoreDocs.length; i++, j++) {
            float delta = wikapidiaScoreDocs[i].score - wikapidiaScoreDocs[j].score;
            if (delta < threshold) {
                cutoff = j;
                break;
            }
        }
        if (cutoff < wikapidiaScoreDocs.length) {
            wikapidiaScoreDocs = ArrayUtils.subarray(wikapidiaScoreDocs, 0, cutoff);
        }
        return wikapidiaScoreDocs;
    }

    public static class Provider extends org.wikapidia.conf.Provider<VectorGenerator> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return VectorGenerator.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.generator";
        }

        @Override
        public VectorGenerator get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("esa")) {
                return null;
            }
            if (!runtimeParams.containsKey("language")) {
                throw new IllegalArgumentException("Monolingual SR Metric requires 'language' runtime parameter");
            }
            Language language = Language.getByLangCode(runtimeParams.get("language"));
            ESAGenerator generator = new ESAGenerator(
                    language,
                    getConfigurator().get(LocalPageDao.class),
                    getConfigurator().get(LuceneSearcher.class, config.getString("luceneSearcher"))
            );
            if (config.hasPath("concepts")) {
                try {
                    generator.setConcepts(new File(config.getString("concepts")));
                } catch (IOException e) {
                    throw new ConfigurationException(e);
                }
            }
            return generator;
        }
    }
}
