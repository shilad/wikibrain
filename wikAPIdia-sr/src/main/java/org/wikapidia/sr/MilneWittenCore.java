package org.wikapidia.sr;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Matt Lesicko
 * Date: 7/3/13
 * Time: 10:39 AM
 * To change this template use File | Settings | File Templates.
 */
public class MilneWittenCore {
    public MilneWittenCore(){

    }

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
