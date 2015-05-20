package org.wikibrain.sr.vector;

import com.typesafe.config.Config;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.lucene.LuceneSearcher;
import org.wikibrain.lucene.QueryBuilder;
import org.wikibrain.lucene.WikiBrainScoreDoc;
import org.wikibrain.lucene.WpIdFilter;
import org.wikibrain.sr.Explanation;
import org.wikibrain.sr.SRResult;
import org.wikibrain.sr.SRResultList;
import org.wikibrain.sr.utils.Leaderboard;
import org.wikibrain.sr.utils.SimUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Shilad Sen
 */
public class ESAGenerator implements SparseVectorGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(ESAGenerator.class);

    private final LuceneSearcher searcher;
    private final Language language;
    private final LocalPageDao pageDao;

    private WpIdFilter conceptFilter = null;
    private TIntSet blackListSet;
    private final String blackListFilePath;

    public ESAGenerator(Language language, LocalPageDao pageDao, LuceneSearcher searcher, String blackListFilePath) {
        this.language = language;
        this.pageDao = pageDao;
        this.searcher = searcher;
        this.blackListFilePath = blackListFilePath;
        try{
            createBlackListSet();
        } catch (Exception e){
            LOG.info("Could not create Blacklist Set");
        }
    }

    private void createBlackListSet() throws FileNotFoundException {
        blackListSet = new TIntHashSet();
        if(blackListFilePath == null || blackListFilePath.equals("")) {
            LOG.info("Skipping blacklist creation; no blacklist file specified.");
            return;
        }

        File file = new File(blackListFilePath);
        Scanner scanner = new Scanner(file);
        while(scanner.hasNext()){
            blackListSet.add(scanner.nextInt());
        }

        scanner.close();
    }


    @Override
    public TIntFloatMap getVector(int pageId) throws DaoException {
        int luceneId = searcher.getDocIdFromLocalId(pageId, language);
        if (luceneId < 0) {
            LOG.warn("Unindexed document " + pageId + " in " + language.getEnLangName());
            return new TIntFloatHashMap();
        }
        WikiBrainScoreDoc[] wikibrainScoreDocs =  getQueryBuilder()
                .setMoreLikeThisQuery(luceneId)
                .search();
        wikibrainScoreDocs = pruneSimilar(wikibrainScoreDocs);
        return SimUtils.normalizeVector(expandScores(wikibrainScoreDocs));

    }

    @Override
    public TIntFloatMap getVector(String phrase) {
        QueryBuilder builder = getQueryBuilder().setPhraseQuery(phrase);
        if (builder.hasQuery()) {
            WikiBrainScoreDoc[] scoreDocs = builder.search();
            scoreDocs = SimUtils.pruneSimilar(scoreDocs);
            return SimUtils.normalizeVector(expandScores(scoreDocs));
        } else {
            LOG.warn("Phrase cannot be parsed to get a query. "+phrase);
            return null;
        }
    }

    public void setConcepts(File file) throws IOException {
        conceptFilter = null;
        if (!file.isFile()) {
            LOG.warn("concept path " + file + " not a file; defaulting to all concepts");
            return;
        }
        TIntSet ids = new TIntHashSet();
        for (String wpId : FileUtils.readLines(file)) {
            int wpLocalIDNumb= Integer.valueOf(wpId);
            if(!isBlacklisted(wpLocalIDNumb)) {
                ids.add(wpLocalIDNumb);
            }
        }
        conceptFilter = new WpIdFilter(ids.toArray());
        LOG.warn("installed " + ids.size() + " concepts for " + language);
    }

    private boolean isBlacklisted(int wpLocalIDNumb) {
        return blackListSet.contains(wpLocalIDNumb);
    }

    @Override
    public List<Explanation> getExplanations(int pageID1, int pageID2, TIntFloatMap vector1, TIntFloatMap vector2, SRResult result) throws DaoException {
        LocalPage page1=pageDao.getById(language,pageID1);
        LocalPage page2=pageDao.getById(language,pageID2);
        Leaderboard lb = new Leaderboard(5);    // TODO: make 5 configurable
        for (int id : vector1.keys()) {
            if (vector2.containsKey(id)) {
                lb.tallyScore(id, vector1.get(id) * vector2.get(id));
            }
        }
        SRResultList top = lb.getTop();
        if (top.numDocs() == 0) {
            return Arrays.asList(new Explanation("? and ? share no links", page1, page2));
        }

        List<Explanation> explanations = new ArrayList<Explanation>();
        for (int i = 0; i < top.numDocs(); i++) {
            LocalPage p = pageDao.getById(language, top.getId(i));
            if (p != null) {
                explanations.add(new Explanation("Both ? and ? have similar text to ?", page1, page2, p));
            }
        }
        return explanations;
    }

    @Override
    public List<Explanation> getExplanations(String phrase1, String phrase2, TIntFloatMap vector1, TIntFloatMap vector2, SRResult result) throws DaoException {
        Leaderboard lb = new Leaderboard(5);    // TODO: make 5 configurable
        for (int id : vector1.keys()) {
            if (vector2.containsKey(id)) {
                lb.tallyScore(id, vector1.get(id) * vector2.get(id));
            }
        }
        SRResultList top = lb.getTop();
        if (top.numDocs() == 0) {
            return Arrays.asList(new Explanation("? and ? share no tags", phrase1, phrase2));
        }

        List<Explanation> explanations = new ArrayList<Explanation>();
        for (int i = 0; i < top.numDocs(); i++) {
            LocalPage p = pageDao.getById(language, searcher.getLocalIdFromDocId(top.getId(i), language));
            if (p != null) {
                explanations.add(new Explanation("Both ? and ? have similar text to ?", phrase1, phrase2, p));
            }
        }
        return explanations;
    }

    private QueryBuilder getQueryBuilder() {
        QueryBuilder builder = searcher.getQueryBuilderByLanguage(language);
        builder.setResolveWikipediaIds(false);
        if (conceptFilter != null) {
            builder.addFilter(conceptFilter);
        }
        return builder;
    }

    /**
     * Put data in a scoreDoc into a TIntDoubleHashMap
     *
     * @param wikibrainScoreDocs
     * @return
     */
    private TIntFloatMap expandScores(WikiBrainScoreDoc[] wikibrainScoreDocs) {
        TIntFloatMap expanded = new TIntFloatHashMap();
        for (WikiBrainScoreDoc wikibrainScoreDoc : wikibrainScoreDocs) {
            expanded.put(wikibrainScoreDoc.luceneId, wikibrainScoreDoc.score);
        }
        return expanded;
    }

    /**
     * Prune a WikiBrainScoreDoc array.
     * @param wikibrainScoreDocs array of WikiBrainScoreDoc
     */
    private WikiBrainScoreDoc[] pruneSimilar(WikiBrainScoreDoc[] wikibrainScoreDocs) {
        if (wikibrainScoreDocs.length == 0) {
            return wikibrainScoreDocs;
        }
        int cutoff = wikibrainScoreDocs.length;
        double threshold = 0.005 * wikibrainScoreDocs[0].score;
        for (int i = 0, j = 100; j < wikibrainScoreDocs.length; i++, j++) {
            float delta = wikibrainScoreDocs[i].score - wikibrainScoreDocs[j].score;
            if (delta < threshold) {
                cutoff = j;
                break;
            }
        }
        if (cutoff < wikibrainScoreDocs.length) {
            wikibrainScoreDocs = ArrayUtils.subarray(wikibrainScoreDocs, 0, cutoff);
        }
        return wikibrainScoreDocs;
    }

    public static class Provider extends org.wikibrain.conf.Provider<SparseVectorGenerator> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return SparseVectorGenerator.class;
        }

        @Override
        public String getPath() {
            return "sr.metric.sparsegenerator";
        }

        @Override
        public SparseVectorGenerator get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
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
                    getConfigurator().get(LuceneSearcher.class, config.getString("luceneSearcher")),
                    getConfig().get().getString("sr.blacklist.path")
            );
            if (config.hasPath("concepts")) {
                try {
                    generator.setConcepts(FileUtils.getFile(
                            config.getString("concepts"),
                            language.getLangCode() + ".txt"));
                } catch (IOException e) {
                    throw new ConfigurationException(e);
                }
            }
            return generator;
        }
    }
}
