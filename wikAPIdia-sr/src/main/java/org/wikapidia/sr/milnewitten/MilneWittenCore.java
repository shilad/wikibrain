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
        TIntSet I = new TIntHashSet(links1);
        I.retainAll(links2);
        if (I.size()==0){
            return new SRResult(0.0);
        }



        SRResult result = new SRResult(1.0-(
                (Math.log(Math.max(links1.size(),links2.size()))-Math.log(I.size()))
                / (Math.log(numPages) - Math.log(Math.min(links1.size(),links2.size())))));

        if (explanations){
            for (int id: I.toArray()){
                List<Integer> formatPages = new ArrayList();
                formatPages.add(id);
                result.addExplanation(new Explanation("?",formatPages));
            }
        }

        return result;
    }

}
