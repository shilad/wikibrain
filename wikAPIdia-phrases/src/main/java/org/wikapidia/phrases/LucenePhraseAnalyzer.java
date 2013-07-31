package org.wikapidia.phrases;

import com.typesafe.config.Config;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.UniversalPage;
import org.wikapidia.lucene.LuceneSearcher;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

/**
 *
 */
public class LucenePhraseAnalyzer implements PhraseAnalyzer {
    private static final Logger LOG = Logger.getLogger(PhraseAnalyzer.class.getName());
    private final LuceneSearcher searcher;

    public LucenePhraseAnalyzer(LuceneSearcher searcher) {
        this.searcher = searcher;
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
        public PhraseAnalyzer get(String name, Config config) throws ConfigurationException {
            if (!config.getString("type").equals("lucene")) {
                return null;
            }

            return null;
        }
    }

    @Override
    public LinkedHashMap<LocalPage, Float> resolveLocal(Language language, String phrase, int maxPages) throws DaoException {
        LinkedHashMap<LocalPage, Float> result = new LinkedHashMap<LocalPage, Float>();

        return null;
    }

    @Override
    public LinkedHashMap<UniversalPage, Float> resolveUniversal(Language language, String phrase, int algorithmId, int maxPages) {
        return null;
    }

    @Override
    public LinkedHashMap<String, Float> describeUniversal(Language language, UniversalPage page, int maxPhrases) {
        return null;
    }

    @Override
    public LinkedHashMap<String, Float> describeLocal(Language language, LocalPage page, int maxPhrases) throws DaoException {
        return null;
    }

    @Override
    public void loadCorpus(LanguageSet langs) throws DaoException, IOException {
    }
}
