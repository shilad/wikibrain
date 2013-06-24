package org.wikapidia.phrases;

import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.jooq.tables.UniversalPage;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.phrases.dao.PhraseAnalyzerDao;

import java.util.LinkedHashMap;

/**
 * Simple implementation of a phrase analyzer.
 */
public class SimplePhraseAnalyzer implements PhraseAnalyzer {
    private PhraseAnalyzerDao phraseDao;
    private LocalPageDao pageDao;

    public SimplePhraseAnalyzer(PhraseAnalyzerDao phraseDao, LocalPageDao pageDao) {
        this.phraseDao = phraseDao;
        this.pageDao = pageDao;
    }

    @Override
    public LinkedHashMap<String, Float> describeLocal(Language language, LocalPage page, int maxPhrases) throws DaoException {
        return null;
    }

    @Override
    public LinkedHashMap<LocalPage, Float> resolveLocal(Language language, String phrase, int maxPages) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public LinkedHashMap<String, Float> describeUniversal(Language language, UniversalPage page, int maxPhrases) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LinkedHashMap<UniversalPage, Float> resolveUniversal(Language language, String phrase, int algorithmId, int maxPages) {
        throw new UnsupportedOperationException();
    }
}
