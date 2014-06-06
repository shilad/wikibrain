package org.wikibrain.mapper.algorithms.conceptualign3;

import com.google.common.collect.*;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;

import java.util.List;

/**
 * Created by bjhecht on 5/21/14.
 */
public class ConceptualignHelper {


    public static ScanResult scanVerticesOfComponent(List<LocalId> curVertices){


        HashMultiset<Language> langs = HashMultiset.create();

        for (LocalId curVertex : curVertices){
            langs.add(curVertex.getLanguage());
        }

        Integer langCount = langs.entrySet().size();
        Integer articleCount = curVertices.size();

        Double clarity = ((double)langCount/(double)articleCount);

        ScanResult scanResult = new ScanResult(clarity, langCount, articleCount);
        return scanResult;

    }

    public static class ScanResult{

        public final Double clarity;
        public final Integer langCount;
        public final Integer articleCount;

        public ScanResult(Double clarity, Integer langCount, Integer articleCount){
            this.clarity = clarity;
            this.langCount = langCount;
            this.articleCount = articleCount;
        }

    }

}
