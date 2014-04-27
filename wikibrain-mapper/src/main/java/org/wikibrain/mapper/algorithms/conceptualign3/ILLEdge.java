package org.wikibrain.mapper.algorithms.conceptualign3;

import org.wikibrain.core.lang.LocalId;

/**
 * Created by bjhecht on 4/25/14.
 * Adaptation of Conceptualign2's ILLEdge
 */

public class ILLEdge {

    public final LocalId host;
    public final LocalId dest;

    public ILLEdge(LocalId host, LocalId dest){
        this.host = host;
        this.dest = dest;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(host.toString());
        sb.append("_");
        sb.append(dest.toString());
        return sb.toString();
    }

    @Override
    public boolean equals(Object o){
        if (o instanceof ILLEdge){
            ILLEdge theirs = (ILLEdge)o;
            return (theirs.host.equals(this.host) && theirs.dest.equals(this.dest));
        }
        return false;
    }

    @Override
    public int hashCode(){
        return this.toString().hashCode();
    }

}
