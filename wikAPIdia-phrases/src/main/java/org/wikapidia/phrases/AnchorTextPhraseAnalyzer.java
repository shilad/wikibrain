package org.wikapidia.phrases;

import com.typesafe.config.Config;
import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.LanguageSet;
import org.wikapidia.core.model.LocalLink;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;

/**
* Loads phrase to page mapping using anchor phrase in wiki links.
*/
public class AnchorTextPhraseAnalyzer extends BasePhraseAnalyzer {
    private static final Logger LOG = Logger.getLogger(AnchorTextPhraseAnalyzer.class.getName());

    private LocalLinkDao linkDao;

    public AnchorTextPhraseAnalyzer(PhraseAnalyzerDao phraseDao, LocalPageDao pageDao, LocalLinkDao linkDao) {
        super(phraseDao, pageDao);
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
        public PhraseAnalyzer get(String name, Config config) throws ConfigurationException {
            if (!config.getString("type").equals("anchortext")) {
                return null;
            }
            PhraseAnalyzerDao paDao = getConfigurator().get(PhraseAnalyzerDao.class, config.getString("phraseDao"));
            LocalPageDao lpDao = getConfigurator().get(LocalPageDao.class, config.getString("localPageDao"));
            LocalLinkDao llDao = getConfigurator().get(LocalLinkDao.class, config.getString("localLinkDao"));
            return new AnchorTextPhraseAnalyzer(paDao, lpDao, llDao);
        }
    }
}
