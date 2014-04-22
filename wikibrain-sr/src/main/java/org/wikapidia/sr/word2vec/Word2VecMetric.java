package org.wikapidia.sr.word2vec;

import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.sr.disambig.Disambiguator;
import org.wikapidia.sr.vector.PhraseVectorCreator;
import org.wikapidia.sr.vector.VectorBasedMonoSRMetric;
import org.wikapidia.sr.vector.VectorGenerator;
import org.wikapidia.sr.vector.VectorSimilarity;

/**
 * @author Shilad Sen
 */
public class Word2VecMetric extends VectorBasedMonoSRMetric {
    public Word2VecMetric(String name, Language language, LocalPageDao dao, Disambiguator disambig, VectorGenerator generator, VectorSimilarity similarity, PhraseVectorCreator creator) {
        super(name, language, dao, disambig, generator, similarity, creator);
    }
}
