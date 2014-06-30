package org.wikibrain.phrases;

import com.typesafe.config.Config;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalPage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Shilad Sen
 */
public class CascadingAnalyzer implements PhraseAnalyzer {
    private final PhraseAnalyzer[] delegates;

    public CascadingAnalyzer(PhraseAnalyzer [] delegates) {
        if (delegates == null || delegates.length == 0) {
            throw new IllegalArgumentException("must have at least one delegate");
        }
        this.delegates = delegates;
    }

    @Override
    public int loadCorpus(LanguageSet langs) throws DaoException, IOException {
        int n = 0;
        for (PhraseAnalyzer d : delegates) {
            try {
                n = Math.max(n, d.loadCorpus(langs));
            } catch (UnsupportedOperationException e) {
                // continue to next delegate
            }
        }
        return n;
    }

    @Override
    public LinkedHashMap<String, Float> describe(Language language, LocalPage page, int maxPhrases) throws DaoException {
        LinkedHashMap<String, Float> result = new LinkedHashMap<String, Float>();
        for (PhraseAnalyzer d : delegates) {
            try {
                result = d.describe(language, page, maxPhrases);
                if (result != null && !result.isEmpty()) {
                    break;
                }
            } catch (UnsupportedOperationException e) {
                // continue to next delegate
            }
        }
        return result;
    }

    @Override
    public LinkedHashMap<LocalId, Float> resolve(Language language, String phrase, int maxPages) throws DaoException {
        LinkedHashMap<LocalId, Float> result = new LinkedHashMap<LocalId, Float>();
        for (PhraseAnalyzer d : delegates) {
            try {
                result = d.resolve(language, phrase, maxPages);
                if (result != null && !result.isEmpty()) {
                    break;
                }
            } catch (UnsupportedOperationException e) {
                // continue to next delegate
            }
        }
        return result;
    }

    /**
     * Use a Provider to get configuration in phrases.analyzer.
     */
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
            if (!config.getString("type").equals("cascading")) {
                return null;
            }
            List<PhraseAnalyzer> delegates = new ArrayList<PhraseAnalyzer>();
            for (String delegateName : config.getStringList("delegates")) {
                delegates.add(getConfigurator().get(PhraseAnalyzer.class, delegateName));
            }
            return new CascadingAnalyzer(delegates.toArray(new PhraseAnalyzer[0]));
        }
    }
}
