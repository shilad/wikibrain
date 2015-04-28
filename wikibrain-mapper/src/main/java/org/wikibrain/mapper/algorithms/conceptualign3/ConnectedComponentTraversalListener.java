package org.wikibrain.mapper.algorithms.conceptualign3;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.VertexTraversalEvent;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.lang.LocalId;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by bjhecht on 5/20/14.
 */
public class ConnectedComponentTraversalListener implements TraversalListener<LocalId, ILLEdge> {

    private List<LocalId> curVertices;
    private ILLGraph graph;
    private List<ConnectedComponentHandler> ccHandlers;
    private int curComponentId;
    private final List<ClusterResult> clusterResults;

    private static Logger LOG = LoggerFactory.getLogger(ConceptualignConceptMapper.class);

    public ConnectedComponentTraversalListener(ILLGraph graph, List<ConnectedComponentHandler> ccHandlers) throws WikiBrainException {

        this.graph = graph;
        this.ccHandlers = ccHandlers;
        this.curComponentId = 0;
        this.clusterResults = Lists.newArrayList();

        newConnectedComponent();

    }

    private void newConnectedComponent(){
        //log.trace(""); // stupid hack for a new line
        curVertices = Lists.newArrayList();
        curComponentId++;
    }

//	private int debug_counter = 0;

    @Override
    public void connectedComponentFinished(ConnectedComponentTraversalEvent arg0) {

        try {
            if (curVertices.size() > 0) { // make sure we're dealing with a valid component (e.g. not a disambig page)

                for (ConnectedComponentHandler ccHandler : ccHandlers) {
                    List<ClusterResult> clusters = ccHandler.handle(curVertices, graph, curComponentId);
                    clusterResults.addAll(clusters);
                }

                if (curComponentId % 1000 == 0) {
                    LOG.info(String.format("Traversed through %d connected components", curComponentId));
                }

            }
        }catch(WikiBrainException e){
            LOG.error(e.getMessage());
        }

    }
    @Override
    public void connectedComponentStarted(ConnectedComponentTraversalEvent arg0) {
        newConnectedComponent();
    }
    @Override
    public void edgeTraversed(EdgeTraversalEvent<LocalId, ILLEdge> arg0) {
       // do nothing

    }
    @Override
    public void vertexFinished(VertexTraversalEvent<LocalId> arg0) {
        // do nothing
    }
    @Override
    public void vertexTraversed(VertexTraversalEvent<LocalId> arg0) {

        curVertices.add(arg0.getVertex());

    }

    public List<ClusterResult> getClusterResults(){
        return this.clusterResults;
    }



}
