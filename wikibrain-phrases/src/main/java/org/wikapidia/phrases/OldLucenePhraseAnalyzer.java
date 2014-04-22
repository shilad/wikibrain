package org.wikapidia.phrases;

import com.typesafe.config.Config;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.lucene.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Using Lucene in a phrase analyzer.
 * @author Yulun Li
 */
public class OldLucenePhraseAnalyzer implements PhraseAnalyzer {
    private static final Logger LOG = Logger.getLogger(PhraseAnalyzer.class.getName());
    private final LuceneSearcher searcher;

    protected LocalPageDao localPageDao;

    public OldLucenePhraseAnalyzer(LocalPageDao localPageDao, LuceneSearcher searcher) {
        this.localPageDao = localPageDao;
        this.searcher = searcher;
    }

    @Override
    public LinkedHashMap<LocalPage, Float> resolve(Language language, String phrase, int maxPages) throws DaoException {
        LinkedHashMap<LocalPage, Float> result = new LinkedHashMap<LocalPage, Float>();
        WikapidiaScoreDoc[] wikapidiaScoreDocs = searcher.getQueryBuilderByLanguage(language)
                .setPhraseQuery(phrase)
                .setNumHits(10)
                .search();
        if (wikapidiaScoreDocs.length == 0 && phrase.indexOf(" ") < 0) {
            String phraseMultiVersion = "";
            for (int i = 1; i < phrase.length(); i++) {
                phraseMultiVersion += (i > 2 ? phrase.substring(0, i) + " " : "");
                phraseMultiVersion += (phrase.length() - i > 2 ? phrase.substring(i, phrase.length()) + " " : "");
            }
            wikapidiaScoreDocs = searcher.getQueryBuilderByLanguage(language)
                    .setPhraseQuery(phraseMultiVersion)
                    .setNumHits(10)
                    .search();
        }

        float totalScore = 0;
        for (WikapidiaScoreDoc wikapidiaScoreDoc : wikapidiaScoreDocs) {
            totalScore += wikapidiaScoreDoc.score;
        }
        for (WikapidiaScoreDoc wikapidiaScoreDoc : wikapidiaScoreDocs) {
            int localPageId = searcher.getLocalIdFromDocId(wikapidiaScoreDoc.luceneId, language);
            LocalPage localPage = localPageDao.getById(language, localPageId);
            result.put(localPage, wikapidiaScoreDoc.score / totalScore);
        }
        return result;
    }

    public static class Provider extends org.wikapidia.conf.Provider<PhraseAnalyzer> {
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
    public void loadCorpus(LanguageSet langs) throws DaoException, IOException {
    }
}
