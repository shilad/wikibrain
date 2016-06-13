package org.wikibrain.cookbook.regionlabeling;

import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.LocalPage;


/**
 * Created by Anja Beth Swoap on 6/10/16.
 */
public class CategoryCount implements Comparable<CategoryCount> {
    private LocalPage page;
    private int count;



    public CategoryCount(LocalPage p) {
        page = p;
        count = 1;
    }

    public CategoryCount(LocalPage p, int r) {
        page = p;
        count = r;
    }

   public LocalPage getPage(){
       return page;
   }

    public int getCount() {
        return count;
    }

    public int compareTo(CategoryCount p) {
        return Double.compare(count, p.getCount());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof CategoryCount)) {
            return false;
        }

        CategoryCount cc = (CategoryCount) obj;

        return (page.getLocalId() == cc.getPage().getLocalId());
    }

    public String toString(){
        return page.getTitle().toString() + " count: " + count;
    }

}