package org.wikibrain.phrases;

import com.typesafe.config.Config;
import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.LocalLinkDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.StringNormalizer;
import org.wikibrain.core.model.LocalLink;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Loads phrase to page mapping using anchor phrase in wiki links.
*/
public class AnchorTextPhraseAnalyzer extends BasePhraseAnalyzer {
    private static final Logger LOG = LoggerFactory.getLogger(AnchorTextPhraseAnalyzer.class);

    private LocalLinkDao linkDao;

    public AnchorTextPhraseAnalyzer(PhraseAnalyzerDao phraseDao, LocalPageDao pageDao, LocalLinkDao linkDao, PrunedCounts.Pruner<String> phrasePruner, PrunedCounts.Pruner<Integer> pagePruner) {
        super(phraseDao, pageDao, phrasePruner, pagePruner);
        this.linkDao = linkDao;
    }

    /**
     * Loads language links into the database.
     */
    @Override
    public Iterable<BasePhraseAnalyzer.Entry> getCorpus(final LanguageSet langs) throws IOException, DaoException {
        return new Iterable<BasePhraseAnalyzer.Entry>() {
            @Override
            public Iterator<BasePhraseAnalyzer.Entry> iterator() {
                try {
                    return new Iter(linkDao.get(new DaoFilter().setLanguages(langs)).iterator());
                } catch (DaoException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public class Iter implements Iterator<BasePhraseAnalyzer.Entry> {
        Iterator<LocalLink> iter;
        private BasePhraseAnalyzer.Entry buffer = null;
        private boolean finished = false;

        Iter(Iterator<LocalLink> iter) {
            this.iter = iter;
        }

        @Override
        public boolean hasNext() {
            if (buffer != null) {
                return true;
            }
            if (finished) {
                return false;
            }
            fillBuffer();
            return buffer != null;
        }

        @Override
        public BasePhraseAnalyzer.Entry next() {
            fillBuffer();
            BasePhraseAnalyzer.Entry tmp = buffer;
            buffer = null;
            return tmp;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private void fillBuffer() {
            if (finished || buffer != null) {
                return;
            }
            if (!iter.hasNext()) {
                finished = true;
                return;
            }
            LocalLink ll = iter.next();
            if (ll == null) {
                finished = true;
                return;
            }
            buffer = new BasePhraseAnalyzer.Entry(
                    ll.getLanguage(), ll.getDestId(), ll.getAnchorText(), 1
                );
        }
    }


    public static class Provider extends org.wikibrain.conf.Provider<PhraseAnalyzer> {
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
            if (!config.getString("type").equals("anchortext")) {
                return null;
            }
            PhraseAnalyzerDao paDao = getConfigurator().construct(
                    PhraseAnalyzerDao.class, name, config.getConfig("dao"),
                    new HashMap<String, String>());
            LocalPageDao lpDao = getConfigurator().get(LocalPageDao.class, config.getString("localPageDao"));
            LocalLinkDao llDao = getConfigurator().get(LocalLinkDao.class, config.getString("localLinkDao"));
            PrunedCounts.Pruner<String> phrasePruner = getConfigurator().construct(
                    PrunedCounts.Pruner.class, null, config.getConfig("phrasePruner"), null);
            PrunedCounts.Pruner<Integer> pagePruner = getConfigurator().construct(
                    PrunedCounts.Pruner.class, null, config.getConfig("pagePruner"), null);
            return new AnchorTextPhraseAnalyzer(paDao, lpDao, llDao, phrasePruner, pagePruner);
        }
    }
}
