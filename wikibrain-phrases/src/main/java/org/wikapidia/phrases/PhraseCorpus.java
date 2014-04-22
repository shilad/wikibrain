package org.wikapidia.phrases;

import org.wikapidia.core.dao.DaoException;
import org.wikapidia.phrases.PhraseAnalyzerDao;

import java.io.IOException;

/**
 */
public interface PhraseCorpus {
    /**
     * Loads a single phrase corpus into the database.
     * @throws java.io.IOException, DaoException
     */
    void loadCorpus(PhraseAnalyzerDao dao) throws DaoException, IOException;
}
