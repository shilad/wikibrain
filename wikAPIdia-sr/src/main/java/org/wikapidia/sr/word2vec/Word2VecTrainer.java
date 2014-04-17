package org.wikapidia.sr.word2vec;

import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.DaoFilter;
import org.wikapidia.core.dao.RawPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.RawPage;
import org.wikapidia.lucene.tokenizers.LanguageTokenizer;

/**
 * @author Shilad Sen
 */
public class Word2VecTrainer {
    private final Language language;
    private final LanguageTokenizer tokenizer;
    private final RawPageDao dao;

    public Word2VecTrainer(RawPageDao dao, Language language, LanguageTokenizer tokenizer) {
        this.dao = dao;
        this.language = language;
        this.tokenizer = tokenizer;
    }

    public void train() throws DaoException {
        for (RawPage page : dao.get(new DaoFilter())) {

        }
    }
}
