package org.wikibrain.phrases;

import com.typesafe.config.Config;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.lucene.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Using Lucene in a phrase analyzer.
 * @author Yulun Li
 */
public class OldLucenePhraseAnalyzer implements PhraseAnalyzer {
    private static final Logger LOG = LoggerFactory.getLogger(PhraseAnalyzer.class);
    private final LuceneSearcher searcher;

    protected LocalPageDao localPageDao;

    public OldLucenePhraseAnalyzer(LocalPageDao localPageDao, LuceneSearcher searcher) {
        this.localPageDao = localPageDao;
        this.searcher = searcher;
    }

    @Override
    public LinkedHashMap<LocalId, Float> resolve(Language language, String phrase, int maxPages) throws DaoException {
        LinkedHashMap<LocalId, Float> result = new LinkedHashMap<LocalId, Float>();
        WikiBrainScoreDoc[] wikibrainScoreDocs = searcher.getQueryBuilderByLanguage(language)
                .setPhraseQuery(phrase)
                .setNumHits(10)
                .search();
        if (wikibrainScoreDocs.length == 0 && phrase.indexOf(" ") < 0) {
            String phraseMultiVersion = "";
            for (int i = 1; i < phrase.length(); i++) {
                phraseMultiVersion += (i > 2 ? phrase.substring(0, i) + " " : "");
                phraseMultiVersion += (phrase.length() - i > 2 ? phrase.substring(i, phrase.length()) + " " : "");
            }
            wikibrainScoreDocs = searcher.getQueryBuilderByLanguage(language)
                    .setPhraseQuery(phraseMultiVersion)
                    .setNumHits(10)
                    .search();
        }

        float totalScore = 0;
        for (WikiBrainScoreDoc wikibrainScoreDoc : wikibrainScoreDocs) {
            totalScore += wikibrainScoreDoc.score;
        }
        for (WikiBrainScoreDoc wikibrainScoreDoc : wikibrainScoreDocs) {
            int localPageId = searcher.getLocalIdFromDocId(wikibrainScoreDoc.luceneId, language);
            LocalId localId = new LocalId(language, localPageId);
            result.put(localId, wikibrainScoreDoc.score / totalScore);
        }
        return result;
    }

    public static class Provider extends org.wikibrain.conf.Provider<PhraseAnalyzer> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class<PhraseAnalyzer> getType() {
            return PhraseAnalyzer.class;
        }

        @Override
        public String getPath() {
            return "phrases.analyzer";
        }
        @Override
        public PhraseAnalyzer get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("olucene")) {
                return null;
            }
            LocalPageDao localPageDao = getConfigurator().get(LocalPageDao.class, config.getString("localPageDao"));
            LuceneSearcher searcher = new LuceneSearcher(
                    new LanguageSet("simple"),
                    getConfigurator().get(LuceneOptions.class));

            return new LucenePhraseAnalyzer(localPageDao, searcher);
        }

    }

    @Override
    public LinkedHashMap<String, Float> describe(Language language, LocalPage page, int maxPhrases) throws DaoException {
        return null;
    }

    @Override
    public int loadCorpus(LanguageSet langs) throws DaoException, IOException {
        return -1;
    }
}
