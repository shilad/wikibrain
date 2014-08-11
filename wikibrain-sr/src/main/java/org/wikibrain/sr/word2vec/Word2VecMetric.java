package org.wikibrain.sr.word2vec;

import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.sr.disambig.Disambiguator;
import org.wikibrain.sr.vector.PhraseVectorCreator;
import org.wikibrain.sr.vector.VectorBasedSRMetric;
import org.wikibrain.sr.vector.VectorGenerator;
import org.wikibrain.sr.vector.VectorSimilarity;

/**
 * @author Shilad Sen
 */
public class Word2VecMetric extends VectorBasedSRMetric {
    public Word2VecMetric(String name, Language language, LocalPageDao dao, Disambiguator disambig, VectorGenerator generator, VectorSimilarity similarity, PhraseVectorCreator creator) {
        super(name, language, dao, disambig, generator, similarity, creator);
    }
}
