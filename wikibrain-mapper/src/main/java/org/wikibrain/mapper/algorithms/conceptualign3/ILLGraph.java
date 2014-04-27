package edu.collablab.wikapidia.graph;

import java.util.*;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.jgrapht.EdgeFactory;
import org.jgrapht.UndirectedGraph;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.InterLanguageLinkDao;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.InterLanguageLink;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.mapper.algorithms.conceptualign3.CombinedIllDao;
import org.wikibrain.mapper.algorithms.conceptualign3.ILLEdge;


public class ILLGraph implements UndirectedGraph<LocalId, ILLEdge>{


    private final CombinedIllDao dao;

    public ILLGraph(CombinedIllDao dao){
        this.dao = dao;
    }



    @Override
    public ILLEdge addEdge(LocalId arg0, LocalId arg1) {
        throw new RuntimeException("Read only graph");
    }

    @Override
    public boolean addEdge(LocalId arg0, LocalId arg1,
                           ILLEdge arg2) {
        throw new RuntimeException("Read only graph");
    }

    @Override
    public boolean addVertex(LocalId arg0) {
        throw new RuntimeException("Read only graph");
    }

    @Override
    public boolean containsEdge(ILLEdge arg0) {
        try{

           return dao.getFromSource(arg0.host).contains(arg0.dest);

        }catch(DaoException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean containsEdge(LocalId arg0, LocalId arg1) {

        ILLEdge edge = new ILLEdge(arg0, arg1);
        return containsEdge(edge);

    }

    @Override
    public boolean containsVertex(LocalId arg0) {
        return true; // this should always be true
    }


    @Override
    public Set<ILLEdge> edgeSet() {
        throw new RuntimeException("Too much memory?");
    }

    private Set<ILLEdge> toEdges(LocalId single, Collection<LocalId> multiples, boolean singleIsSource){

        Set<ILLEdge> rVal = Sets.newHashSet();

        for (LocalId multiple : multiples){

            ILLEdge edge;
            if (singleIsSource){
                edge = new ILLEdge(single, multiple);
            }else{
                edge = new ILLEdge(multiple, single);
            }

            rVal.add(edge);
        }

        return rVal;

    }

    @Override
    public Set<ILLEdge> edgesOf(LocalId arg0) {
        try{

            Set<ILLEdge> rVal = Sets.newHashSet();
            rVal.addAll(toEdges(arg0, dao.getFromSource(arg0), true));
            rVal.addAll(toEdges(arg0, dao.getToDest(arg0), false));

            return rVal;

        }catch(DaoException e){

            throw new RuntimeException(e);

        }
    }


    @Override
    public Set<ILLEdge> getAllEdges(LocalId arg0, LocalId arg1) {

       Set<ILLEdge> rVal = Sets.newHashSet();

       if (containsEdge(arg0, arg1)){
            rVal.add(new ILLEdge(arg0, arg1));
       }

       if (containsEdge(arg1, arg0)){
           rVal.add(new ILLEdge(arg1, arg0));
       }

       return rVal;

    }

    @Override
    public ILLEdge getEdge(LocalId arg0, LocalId arg1) {

        if (containsEdge(arg0, arg1)){
            return new ILLEdge(arg0, arg1);
        }
    }

    @Override
    public EdgeFactory<LocalId, ILLEdge> getEdgeFactory() {
        throw new RuntimeException("Read only graph");
    }

    @Override
    public LanguagedLocalId getEdgeSource(ILLEdge arg0) {
        return arg0.host;
    }

    @Override
    public LanguagedLocalId getEdgeTarget(ILLEdge arg0) {
        return arg0.dest;
    }

    @Override
    public double getEdgeWeight(ILLEdge arg0) {
        return 0;
    }

    @Override
    public boolean removeAllEdges(Collection<? extends ILLEdge> arg0) {
        throw new RuntimeException("Read-only graph");
    }

    @Override
    public Set<ILLEdge> removeAllEdges(LanguagedLocalId arg0,
                                       LanguagedLocalId arg1) {
        throw new RuntimeException("Read-only graph");
    }

    @Override
    public boolean removeAllVertices(Collection<? extends LanguagedLocalId> arg0) {
        throw new RuntimeException("Read-only graph");
    }

    @Override
    public boolean removeEdge(ILLEdge arg0) {
        throw new RuntimeException("Read-only graph");
    }

    @Override
    public ILLEdge removeEdge(LanguagedLocalId arg0, LanguagedLocalId arg1) {
        throw new RuntimeException("Read-only graph");
    }

    @Override
    public boolean removeVertex(LanguagedLocalId arg0) {
        throw new RuntimeException("Read-only graph");
    }

    @Override
    public Set<LanguagedLocalId> vertexSet() {

        try{
            HashSet<LanguagedLocalId> rVal = new HashSet<LanguagedLocalId>();
            LanguageSet ls = lcqs.getLanguageSet();
            for (Integer curLangId : ls.getLangIds()){
                List<Integer> allLocalIds = lcqs.getAllLocalIds(curLangId);
                for (Integer localId : allLocalIds){
                    rVal.add(new LanguagedLocalId(curLangId, localId));
                }
            }
            return rVal;
        }catch(WikapidiaException e){
            throw new RuntimeException(e);
        }

    }


    public int inDegreeOf(LanguagedLocalId arg0) {
        return incomingEdgesOf(arg0).size();
    }


    public Set<ILLEdge> incomingEdgesOf(LanguagedLocalId arg0) {
        try{
            HashSet<ILLEdge> rVal = new HashSet<ILLEdge>();
            List<LanguagedLocalId> inlinks = illqs.getInILLs(arg0.getLangId(), arg0.getLocalId());
            if (inlinks != null){
                rVal.addAll(makeEdges(arg0,inlinks, false));
            }
            return rVal;
        }catch(WikapidiaException e){
            throw new RuntimeException(e);
        }
    }


    public int outDegreeOf(LanguagedLocalId arg0) {
        return outgoingEdgesOf(arg0).size();
    }


    public Set<ILLEdge> outgoingEdgesOf(LanguagedLocalId arg0) {
        try{
            HashSet<ILLEdge> rVal = new HashSet<ILLEdge>();
            List<LanguagedLocalId> outlinks = illqs.getOutILLs(arg0.getLangId(), arg0.getLocalId());
            for (LanguagedLocalId outlink : outlinks){
                if (outlink.getLocalId() == 0){
                    System.out.println(arg0.getLocalId() + " --> " + outlink.getLocalId());
                }
            }
            if (outlinks != null){
                rVal.addAll(makeEdges(arg0,outlinks, true));
            }
//			for(LanguagedLocalId outlink : outlinks){
//				System.out.println(arg0.toString() + "-->" + outlink.toString());
//			}
            return rVal;
        }catch(WikapidiaException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public int degreeOf(LanguagedLocalId arg0) {
        return edgesOf(arg0).size();
    }





}

