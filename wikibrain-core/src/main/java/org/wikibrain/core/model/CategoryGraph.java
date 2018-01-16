package org.wikibrain.core.model;

import gnu.trove.map.TIntIntMap;
import org.apache.commons.lang.ArrayUtils;
import org.wikibrain.core.lang.Language;

import java.io.Serializable;
import java.util.*;

/**
 * A compact graph representation of the category structure.
 *
 * The internals of this data structure are public for historical reasons.
 * TODO: It would probably be better to not expose the internal id scheme if it's not a performance hit.
 *
 * @author Matt Lesicko
 * @author Shilad Sen
 */
public class CategoryGraph implements Serializable{
    static final long serialVersionUID = -3429823331722647576l;
    public Language language;

    // Mapping from external local page id to internal dense index.
    public TIntIntMap catIndexes;

    // Dense internal category index to sparse external local page ids
    public int[] catIds;

    // the cost of travelling through each category based on page rank
    public double[] catCosts;

    // The category graph. Category to list of parents
    public int[][] catParents;

    // Category to list of local page ids (articles, not categories)
    public int[][] catPages;

    // Category to list of children dense internal category index
    public int[][] catChildren;

    // Names of categories indexed by internal dense index
    public String[] cats;

    // ??
    public double minCost = -1;

    public CategoryGraph(Language language){
        this.language = language;
    }

    public int catIdToIndex(int catId) {
        return catIndexes.containsKey(catId) ?  catIndexes.get(catId) : -1;
    }


    public int catIndexToId(int catIndex) { return (catIndex < 0) ? -1 : catIds[catIndex]; }

    /**
     * Return the wikipedia page ids for child of the specified category
     * @param wpId
     * @return
     */
    public int[] getFamilyMembersCategories(int wpId, String familyMember) {
        int index = catIdToIndex(wpId);
        if (index < 0) {
            return new int[0];
        }
        int[] denseIds;
        if(familyMember == "child") {
            denseIds = catChildren[index]; //gets the children of index
        }else if(familyMember == "parent"){
            denseIds = catParents[index]; //gets theparents of index
        }else{
            return new int[0];
        }

        int famMembersIds[] = new int[denseIds.length];
        for (int i = 0; i < denseIds.length; i++) {
            famMembersIds[i] = catIndexToId(denseIds[i]);
        }
        return famMembersIds;
    }

    /**
     *
     * @param wpId
     * @return
     */
    public int[] getCategoryPages(int wpId) {
        int parentIndex = catIdToIndex(wpId);
        if (parentIndex < 0) {
            return new int[0];
        }
        return catPages[parentIndex];
    }

    /**
     *
     * @param wpId
     * @return
     */
    public String getCategoryName(int wpId){
        int cid = catIdToIndex(wpId); //sparse to dense
        if(cid >=0) {
            String cname = cats[cid];
            return cname;
        }else{
            return "";
        }
    }

    /**
     *
     * @param wpId
     * @param max
     * @return
     */
    public  Integer getMaxMinParentPageRank(int wpId, boolean max){
        int[] parents = getFamilyMembersCategories(wpId, "parent");
        Integer arg = null;
        if (max) {
            for (int par : parents) {
                if (catIdToIndex(par) >= 0) {
                    arg = (arg == null || (catCosts[catIdToIndex(arg)] < catCosts[catIdToIndex(par)])) ? par : arg;
                }
            }
        } else {
            for (int par : parents) {
                if (catIdToIndex(par) >= 0) {
                    arg = (arg == null || (catCosts[catIdToIndex(arg)] > catCosts[catIdToIndex(par)])) ? par : arg;
                }
            }
        }

        return ((arg != null) ? arg: -1);

    }
}
