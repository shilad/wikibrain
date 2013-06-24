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
        LinkedHashMap<String, Float> result = new LinkedHashMap<String, Float>();
        PhraseAnalyzerDao.PrunedCounts<String> counts = phraseDao.getPageCounts(language, page.getLocalId());
        for (String phrase : counts.keySet()) {
            result.put(phrase, (float)1.0 * counts.get(phrase) / counts.getTotal());
            if (counts.size() >= maxPhrases) {
                break;
            }
        }
        return result;
    }

    @Override
    public LinkedHashMap<LocalPage, Float> resolveLocal(Language language, String phrase, int maxPages) throws DaoException {
        LinkedHashMap<LocalPage, Float> result = new LinkedHashMap<LocalPage, Float>();
        PhraseAnalyzerDao.PrunedCounts<Integer> counts = phraseDao.getPhraseCounts(language, phrase);
        for (Integer wpId : counts.keySet()) {
            result.put(pageDao.getById(language, wpId),
                    (float)1.0 * counts.get(wpId) / counts.getTotal());
            if (counts.size() >= maxPages) {
                break;
            }
        }
        return result;
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
