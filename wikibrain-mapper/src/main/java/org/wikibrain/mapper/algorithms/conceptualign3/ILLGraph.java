package org.wikibrain.mapper.algorithms.conceptualign3;

import java.util.*;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.jgrapht.EdgeFactory;
import org.jgrapht.UndirectedGraph;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.InterLanguageLink;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.mapper.algorithms.conceptualign3.CombinedIllDao;
import org.wikibrain.mapper.algorithms.conceptualign3.ILLEdge;


/**
 * Graph representation of ILL graph.
 */
public class ILLGraph implements UndirectedGraph<LocalId, ILLEdge>{


    private final CombinedIllDao dao;
    private final LocalPageDao lpDao;
    private final MetaInfoDao miDao;

    private Set<LocalId> validLocalIds;

    private final LanguageSet loadedLangs;

    public ILLGraph(CombinedIllDao dao, LocalPageDao lpDao, MetaInfoDao miDao) throws WikiBrainException {

        try {

            // *** do basic setup ***

            this.dao = dao;
            this.lpDao = lpDao;
            this.miDao = miDao;

            loadedLangs = miDao.getLoadedLanguages();

            // *** get valid local ids (aka vertices) ***

            validLocalIds = Sets.newHashSet();

            Iterable<LocalPage> lPages = lpDao.get(new DaoFilter().setNameSpaces(NameSpace.ARTICLE));
            validLocalIds.addAll(LocalPage.toLocalIds(lPages));

        }catch(DaoException e){

            throw new WikiBrainException(e);

        }

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
        return validLocalIds.contains(arg0);
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
        }else{
            throw new RuntimeException("Could not find expected edge");
        }
    }

    @Override
    public EdgeFactory<LocalId, ILLEdge> getEdgeFactory() {
        throw new RuntimeException("Read only graph");
    }

    @Override
    public LocalId getEdgeSource(ILLEdge arg0) {
        return arg0.host;
    }

    @Override
    public LocalId getEdgeTarget(ILLEdge arg0) {
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
    public Set<ILLEdge> removeAllEdges(LocalId arg0,
                                       LocalId arg1) {
        throw new RuntimeException("Read-only graph");
    }

    @Override
    public boolean removeAllVertices(Collection<? extends LocalId> arg0) {
        throw new RuntimeException("Read-only graph");
    }

    @Override
    public boolean removeEdge(ILLEdge arg0) {
        throw new RuntimeException("Read-only graph");
    }

    @Override
    public ILLEdge removeEdge(LocalId arg0, LocalId arg1) {
        throw new RuntimeException("Read-only graph");
    }

    @Override
    public boolean removeVertex(LocalId arg0) {
        throw new RuntimeException("Read-only graph");
    }

    @Override
    public Set<LocalId> vertexSet() {

        return this.validLocalIds;

    }

    public int inDegreeOf(LocalId arg0) {
        return incomingEdgesOf(arg0).size();
    }


    private Set<ILLEdge> makeEdges(LocalId single, Set<LocalId> multiples, boolean outlinks){

        Set<ILLEdge> rVal = Sets.newHashSet();
        for (LocalId multiple : multiples){

            if (validLocalIds.contains(multiple)) { // filter out languages that are not loaded

                ILLEdge curEdge = null;
                if (outlinks) {
                    curEdge = new ILLEdge(single, multiple);
                } else {
                    curEdge = new ILLEdge(multiple, single);
                }

                rVal.add(curEdge);
            }
        }
        return rVal;

    }


    public Set<ILLEdge> incomingEdgesOf(LocalId arg0) {

        try{

            Set<LocalId> incomingLocalIds = dao.getToDest(arg0);
            return makeEdges(arg0, incomingLocalIds, false);

        }catch(DaoException e){
            throw new RuntimeException(e);
        }

    }


    public int outDegreeOf(LocalId arg0) {
        return outgoingEdgesOf(arg0).size();
    }


    public Set<ILLEdge> outgoingEdgesOf(LocalId arg0) {

        try{

            Set<LocalId> outgoingLocalIds = dao.getFromSource(arg0);
            return makeEdges(arg0, outgoingLocalIds, true);

        }catch(DaoException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public int degreeOf(LocalId arg0) {
        return edgesOf(arg0).size();
    }





}

