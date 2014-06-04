package org.wikibrain.sr.milnewitten;

import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.lang.LocalString;
import org.wikibrain.phrases.PhraseAnalyzer;
import org.wikibrain.sr.MonolingualSRMetric;
import org.wikibrain.sr.disambig.Disambiguator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * @author Shilad Sen
 */
public class MilneWittenDisambiguator extends Disambiguator {
    private final Language language;
    private final PhraseAnalyzer analyzer;
    private final MonolingualSRMetric submetric;

    public MilneWittenDisambiguator(Language language, PhraseAnalyzer analyzer, MonolingualSRMetric submetric) {
        this.language = language;
        this.analyzer = analyzer;
        this.submetric = submetric;
    }

    @Override
    public List<LinkedHashMap<LocalId, Float>> disambiguate(List<LocalString> phrases, Set<LocalString> context) throws DaoException {

        /*

        LinkedHashMap<LocalId, Float> candidates1 = analyzer.resolve(language, phrase1, 100);
        LinkedHashMap<LocalId, Float> candidates2 = analyzer.resolve(language, phrase2, 100);
        if (candidates1 == null || candidates2 == null) {
            return null;
        }

        double highestScore = Double.NEGATIVE_INFINITY;
        for (LocalId lid1 : candidates1.keySet()) {
            for (LocalId lid2 : candidates2.keySet()) {
                double score = similarity(lid1.getId(), lid2.getId(), false).getScore();
                if (score > highestScore) {
                    highestScore = score;
                }
            }
        }

        double result = 0.0;
        double highestPop = Double.NEGATIVE_INFINITY;

        for (LocalId lid1 : candidates1.keySet()) {
            for (LocalId lid2 : candidates2.keySet()) {
                double pop = candidates1.get(lid1) * candidates2.get(lid2);
                if (pop > highestPop) {
                    highestPop = pop;
                    result = similarity(lid1.getId(), lid2.getId(), false).getScore();
                }
            }
        }

        int n1 = getPhraseCount(phrase1 + " " + phrase2);
        int n2 = getPhraseCount(phrase2 + " " + phrase1);
        if (n1 + n2 > 0) {
            result += Math.log(n1 + n2 + 1) / 10;
        }

        return new SRResult(result);
        */

        return null;
    }
}
