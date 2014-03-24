package org.wikapidia.phrases;

import com.typesafe.config.Config;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.RedirectDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.core.model.Title;
import org.wikapidia.core.model.UniversalPage;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implements a deterministic phrase analyzer that returns either a single Wikipedia entity or nothing
 * based on titles and/or redirects.
 *
 * Notes:
 *  - only operates in the article namespace
 *
 * Created by Brent Hecht on 12/29/13.
 */
public class TitleRedirectPhraseAnalyzer implements PhraseAnalyzer {

    private final boolean useRedirects;
    private final LocalPageDao lpDao;
    private final RedirectDao redirectDao;

    public TitleRedirectPhraseAnalyzer(boolean useRedirects, LocalPageDao lpDao, RedirectDao redirectDao) {
        this.useRedirects = useRedirects;
        this.lpDao = lpDao;
        this.redirectDao = redirectDao;
    }

    @Override
    public void loadCorpus(LanguageSet langs) throws DaoException, IOException {
        // nothing to do here
    }

    @Override
    public LinkedHashMap<String, Float> describe(Language language, LocalPage page, int maxPhrases) throws DaoException {
        throw new UnsupportedOperationException("TitleRedirectPhraseAnalyzer is for resolving only");
    }

    //@Override
    public LinkedHashMap<String, Float> describeUniversal(Language language, UniversalPage page, int maxPhrases) {
        throw new UnsupportedOperationException("TitleRedirectPhraseAnalyzer is for resolving only");
    }

    @Override
    public LinkedHashMap<LocalPage, Float> resolve(Language language, String phrase, int maxPages) throws DaoException {

        LinkedHashMap<LocalPage, Float> result = new LinkedHashMap<LocalPage, Float>();

        if (maxPages < 1) return result;

        LocalPage lp = lpDao.getByTitle(new Title(phrase, language), NameSpace.ARTICLE);
        if (lp == null) return result;

        if (lp.isRedirect()){
            Integer resolvedLocalId = redirectDao.resolveRedirect(language, lp.getLocalId());
            lp = lpDao.getById(language, resolvedLocalId);
        }
        result.put(lp, 1.0f);
        return result;

    }

    //@Override
    public LinkedHashMap<UniversalPage, Float> resolveUniversal(Language language, String phrase, int algorithmId, int maxPages) {
        throw new UnsupportedOperationException();
    }

    public static class Provider extends org.wikapidia.conf.Provider<PhraseAnalyzer> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class getType() {
            return PhraseAnalyzer.class;
        }

        @Override
        public String getPath() {
            return "phrases.analyzer";
        }

        @Override
        public PhraseAnalyzer get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            if (!config.getString("type").equals("titleandredirect")) {
                return null;
            }
            LocalPageDao lpDao = getConfigurator().get(LocalPageDao.class, config.getString("localPageDao"));
            RedirectDao redirectDao = getConfigurator().get(RedirectDao.class, config.getString("redirectDao"));
            Boolean useRedirects = Boolean.parseBoolean(config.getString("useRedirects"));
            return new TitleRedirectPhraseAnalyzer(useRedirects, lpDao, redirectDao);
        }
    }
}
