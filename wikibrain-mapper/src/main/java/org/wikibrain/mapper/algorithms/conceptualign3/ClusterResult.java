package org.wikibrain.mapper.algorithms.conceptualign3;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;

import java.util.Collection;
import java.util.List;

/**
 * Created by bjhecht on 5/20/14.
 */
public class ClusterResult {

    public final Integer univId;
    public final Multimap<Language, LocalId> vertices;

    //Multimap<Language, LocalId> localPages
    public ClusterResult(Integer univId, Multimap<Language, LocalId> vertices){
        this.univId = univId;
        this.vertices = vertices;
    }

    public ClusterResult(Integer univId, Collection<LocalId> vertexCollection){

        Multimap<Language, LocalId> mmap = HashMultimap.create();
        for(LocalId curVertex : vertexCollection){
            mmap.put(curVertex.getLanguage(), curVertex);
        }

        this.univId = univId;
        this.vertices = mmap;

    }
}
