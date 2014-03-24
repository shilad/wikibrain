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
public class LucenePhraseAnalyzer implements PhraseAnalyzer {
    private static final Logger LOG = Logger.getLogger(PhraseAnalyzer.class.getName());

    /**
     * Analyzer retrieves this times as many results as needed and
     * sums scores for all results. The effect of this variable is
     * smoothing out and lowering probability scores in the returned results.
     */
    public static final int DOC_MULTIPLIER = 3;

    private final LuceneSearcher searcher;

    protected LocalPageDao localPageDao;

    public LucenePhraseAnalyzer(LocalPageDao localPageDao, LuceneSearcher searcher) {
        this.localPageDao = localPageDao;
        this.searcher = searcher;
    }

    @Override
    public LinkedHashMap<LocalPage, Float> resolve(Language language, String phrase, int maxPages) throws DaoException {
        LinkedHashMap<LocalPage, Float> result = new LinkedHashMap<LocalPage, Float>();

        // query the title field for the phrase.
        WikapidiaScoreDoc[] wikapidiaScoreDocs = searcher.getQueryBuilderByLanguage(language)
                                    .setPhraseQuery(new TextFieldElements().addTitle(), phrase)
                                    .setNumHits(maxPages * DOC_MULTIPLIER)
                                    .search();


        if (wikapidiaScoreDocs.length == 0) {
            // If there is no result from title field query, query the plaintext field.
            wikapidiaScoreDocs = searcher.getQueryBuilderByLanguage(language)
                                        .setPhraseQuery(new TextFieldElements().addPlainText(), phrase)
                                        .setNumHits(maxPages * DOC_MULTIPLIER)
                                        .search();
        }

        // When the phrase does not get an result from title or plaintext, this section
        // creates a search query of substrings with at least 3 characters and concatenate
        // them with space. This aims at phrases like "bioarchaeology" of which part
        // of the phrase may get a result while the phrase as a whole may not.
        if (wikapidiaScoreDocs.length == 0 && !phrase.contains(" ") && phrase.length() > 3) {
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
        // The scores represent probability so they should sum to 1.
        for (WikapidiaScoreDoc wikapidiaScoreDoc : wikapidiaScoreDocs) {
            totalScore += wikapidiaScoreDoc.score;
        }
        for (WikapidiaScoreDoc wikapidiaScoreDoc : wikapidiaScoreDocs) {
            LocalPage localPage = localPageDao.getById(language, wikapidiaScoreDoc.wpId);
//            if (result.isEmpty()) {
//                System.out.println("top result for " + phrase + " is " + localPage.getTitle());
//            }
            result.put(localPage, wikapidiaScoreDoc.score / totalScore);
            if (result.size() >= maxPages) {
                break;
            }
        }
        return result;
    }

    /**
     * Use a Provider to get configuration in phrases.analyzer.
     */
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
            if (!config.getString("type").equals("lucene")) {
                return null;
            }
            LocalPageDao localPageDao = getConfigurator().get(LocalPageDao.class, config.getString("localPageDao"));
            LanguageSet langs = getConfigurator().get(LanguageSet.class);
            LuceneSearcher searcher = new LuceneSearcher(langs, getConfigurator().get(LuceneOptions.class));

            return new LucenePhraseAnalyzer(localPageDao, searcher);
        }
    }

    @Override
    public LinkedHashMap<String, Float> describe(Language language, LocalPage page, int maxPhrases) throws DaoException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadCorpus(LanguageSet langs) throws DaoException, IOException {
    }
}
