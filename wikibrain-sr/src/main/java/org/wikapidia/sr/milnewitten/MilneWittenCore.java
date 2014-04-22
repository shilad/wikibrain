package org.wikapidia.sr.milnewitten;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.wikapidia.sr.Explanation;
import org.wikapidia.sr.SRResult;

import java.util.ArrayList;
import java.util.List;

/**
 * @author  Matt Lesicko
 * This class handles the core Milne-Witten functionality.
 * Specific Milne-Witten implementations should use an instance of this class.
 */
public class MilneWittenCore {
    public MilneWittenCore(){

    }

    /**
     * Calculate the similarity between two pages based on shared links.
     * Links can be either inlinks or outlinks, but should be consistent
     * between the two pages.
     * Explanations will be returned as page numbers with no special formatting.
     * @param links1 The links for the first page.
     * @param links2 The links for the second page.
     * @param numPages The number of pages in the corpus.
     * @param explanations Whether or not to generate explanations.
     * @return
     */
    public SRResult similarity(TIntSet links1, TIntSet links2, int numPages, boolean explanations){
        TIntSet i = new TIntHashSet(links1);
        i.retainAll(links2);
        if (i.isEmpty()){
            return new SRResult(0.0);
        }

        double a = Math.log(links1.size()) ;
        double b = Math.log(links2.size()) ;
        double ab = Math.log(i.size()) ;
        double m = Math.log(numPages);

        SRResult result = new SRResult(
                1.0 - (Math.max(a, b) -ab) / (m - Math.min(a, b)));

        if (explanations) {
            for (int id: i.toArray()) {
                List<Integer> formatPages = new ArrayList<Integer>();
                formatPages.add(id);
                result.addExplanation(new Explanation("?",formatPages));
            }
        }
        return result;
    }

}
