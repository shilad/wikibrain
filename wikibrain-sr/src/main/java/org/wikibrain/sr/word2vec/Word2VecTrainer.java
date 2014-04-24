package org.wikibrain.sr.word2vec;

import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.DaoFilter;
import org.wikibrain.core.dao.RawPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.RawPage;
import org.wikibrain.lucene.tokenizers.LanguageTokenizer;

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
