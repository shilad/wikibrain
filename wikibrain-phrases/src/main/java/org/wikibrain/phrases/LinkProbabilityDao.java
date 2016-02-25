package org.wikibrain.phrases;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.RawPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.StringNormalizer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Shilad Sen
 * Calculates the probability of a link
 */
public class LinkProbabilityDao {
    private static final Logger LOG = LoggerFactory.getLogger(LinkProbabilityDao.class);

    private final File path;
    private final RawPageDao pageDao;
    private final PhraseAnalyzerDao phraseDao;
    private final LanguageSet langs;
    private final StringNormalizer normalizer;
    private final Map<Language, LangLinkProbabilityDao> langDaos;


    public LinkProbabilityDao(File path, LanguageSet langs, RawPageDao pageDao, PhraseAnalyzerDao phraseDao) throws DaoException {
        this.path = path;
        this.langs = langs;
        this.pageDao = pageDao;
        this.phraseDao = phraseDao;
        this.normalizer = phraseDao.getStringNormalizer();

        langDaos = new HashMap<Language, LangLinkProbabilityDao>();
        for (Language lang : langs) {
            LOG.info("Loading link probability dao for language " + lang);
            langDaos.put(lang, new LangLinkProbabilityDao(
                    pageDao,
                    phraseDao,
                    lang,
                    normalizer,
                    new File(path, lang.getLangCode()))
            );
        }
    }

    public void useCache(boolean useCache) {
        for (LangLinkProbabilityDao dao : langDaos.values()) {
            dao.useCache(useCache);
        }
    }

    public boolean isBuilt() {
        for (LangLinkProbabilityDao dao : langDaos.values()) {
            if (!dao.isBuilt()) return false;
        }
        return true;
    }

    public boolean isSubgram(Language lang, String phrase, boolean normalize) {
        if (!langDaos.containsKey(lang)) {
            throw new IllegalArgumentException("No link probability dao for language: " + lang);
        }
        return langDaos.get(lang).isSubgram(phrase, normalize);
    }


    public double getLinkProbability(Language language, String mention) throws DaoException {
        return getLinkProbability(language, mention, true);
    }

    /**
     * Fixme: Check the cache here...
     * @param lang
     * @param mention
     * @return
     * @throws DaoException
     */
    public double getLinkProbability(Language lang, String mention, boolean normalize) throws DaoException {
        if (!langDaos.containsKey(lang)) {
            throw new IllegalArgumentException("No link probability dao for language: " + lang);
        }
        return langDaos.get(lang).getLinkProbability(mention, normalize);
    }

    public void build() throws DaoException {
        for (LangLinkProbabilityDao dao : langDaos.values()) {
            LOG.info("Building link probability dao for language " + dao.getLang());
            dao.build();
        }
    }


    public static class Provider extends org.wikibrain.conf.Provider<LinkProbabilityDao> {
        public Provider(Configurator configurator, Configuration config) throws ConfigurationException {
            super(configurator, config);
        }

        @Override
        public Class<LinkProbabilityDao> getType() {
            return LinkProbabilityDao.class;
        }

        @Override
        public String getPath() {
            return "phrases.linkProbability";
        }

        @Override
        public LinkProbabilityDao get(String name, Config config, Map<String, String> runtimeParams) throws ConfigurationException {
            LanguageSet ls = getConfigurator().get(LanguageSet.class);
            File path = new File(config.getString("path"));
            String pageName = config.hasPath("rawPageDao") ? config.getString("rawPageDao") : null;
            String phraseName = config.hasPath("phraseAnalyzer") ? config.getString("phraseAnalyzer") : null;
            RawPageDao rpd = getConfigurator().get(RawPageDao.class, pageName);
            PhraseAnalyzer pa = getConfigurator().get(PhraseAnalyzer.class, phraseName);
            if (!(pa instanceof AnchorTextPhraseAnalyzer)) {
                throw new ConfigurationException("LinkProbabilityDao's phraseAnalyzer must be an AnchorTextPhraseAnalyzer");
            }
            PhraseAnalyzerDao pad = ((AnchorTextPhraseAnalyzer)pa).getDao();
            try {
                return new LinkProbabilityDao(path, ls, rpd, pad);
            } catch (DaoException e) {
                throw new ConfigurationException(e);
            }
        }
    }

}
