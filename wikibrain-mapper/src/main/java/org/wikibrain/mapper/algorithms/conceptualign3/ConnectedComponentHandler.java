package org.wikibrain.mapper.algorithms.conceptualign3;

import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.lang.LocalId;

import java.util.List;

/**
 * Created by bjhecht on 5/20/14.
 */
public interface ConnectedComponentHandler {

    public List<ClusterResult> handle(List<LocalId> curVertices, ILLGraph graph, int componentId) throws WikiBrainException;

}
