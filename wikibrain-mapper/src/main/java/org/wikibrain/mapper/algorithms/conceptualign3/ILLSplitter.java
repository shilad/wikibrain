package org.wikibrain.mapper.algorithms.conceptualign3;

import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.LocalId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by bjhecht on 5/21/14.
 */
public class ILLSplitter {

    private static Logger LOG = LoggerFactory.getLogger(ConceptualignConceptMapper.class);

    public static Set<Set<LocalId>> split(Map<LocalId, List<LocalId>> ills,
                                                   int minVotes, int maxVotesPerLang, boolean print, LocalPageDao lpDao) throws WikiBrainException {

        HashMap<LocalId, SummingHashMap<Integer>> counter = new HashMap<LocalId, SummingHashMap<Integer>>();
        HashMap<LocalId, SummingHashMap<Integer>> outCounter = new HashMap<LocalId, SummingHashMap<Integer>>();
        HashMap<LocalId, LocalId> outFoundLinks = new HashMap<LocalId, LocalId>();
        for (LocalId curSource : ills.keySet()){
            outCounter.put(curSource,new SummingHashMap<Integer>());
            for(LocalId curDest : ills.get(curSource)){
                if (!outCounter.get(curSource).containsKey(curDest.getLanguage().getId())){
                    outCounter.get(curSource).addValue(new Integer(curDest.getLanguage().getId()), 1.0);
                    outFoundLinks.put(curSource, curDest);
                }else{
                    if(!outFoundLinks.get(curSource).equals(curDest)){ // prevent duplicates from counting as second links
                        outCounter.get(curSource).addValue(new Integer(curDest.getLanguage().getId()), 1.0);
                    }
                }
                if (!counter.containsKey(curDest)){
                    counter.put(curDest, new SummingHashMap<Integer>());
                }
                counter.get(curDest).addValue(new Integer(curSource.getLanguage().getId()), 1.0);
            }
        }

        int edgeCounter = 0;
        DirectedSparseGraph<LocalId,Integer> graph = new DirectedSparseGraph<LocalId, Integer>();
        for (LocalId curSource : ills.keySet()){
            graph.addVertex(curSource);
            for (LocalId curDest : ills.get(curSource)){
                if (outCounter.get(curSource).get(new Integer(curDest.getLanguage().getId())) <= maxVotesPerLang){
                    int totalVotes = counter.get(curDest).keySet().size();
                    if (totalVotes >= minVotes){
                        if (counter.get(curDest).get(new Integer(curSource.getLanguage().getId())) <= maxVotesPerLang){
                            graph.addEdge(edgeCounter++, curSource, curDest);
                        }
                    }else{
                        if (print) {
                            try {
                                LOG.info("Removing edge: " + lpDao.getById(curSource).getTitle() + " --> " + lpDao.getById(curDest).getTitle());
                            }catch(DaoException e){
                                throw new WikiBrainException(e);
                            }
                        }
                    }
                }else{
                    LOG.warn("Found duplicate ILLs to same lang from same article exceeding maxVotes! " +
                            "Enforcing policy not allowing this!:\t" +curSource + " ---> " + curDest);
                }
            }
        }

        WeakComponentClusterer<LocalId, Integer> clusterer = new WeakComponentClusterer<LocalId, Integer>();
        Set<Set<LocalId>> clusters = clusterer.transform(graph);

        if (print){
            int maxSize = 0;
            Set<LocalId> maxCluster = null;
            for (Set<LocalId> cluster : clusters){
                StringBuilder sb = new StringBuilder();
                for (LocalId clusterMemb : cluster){
                    try {
                        sb.append(lpDao.getById(clusterMemb).getTitle().toString());
                        sb.append(",");
                    }catch(DaoException e){
                        LOG.error("Error while getting title of LocalId: " + clusterMemb.toString());
                    }
                }
                LOG.info("Cluster:\t" + sb.toString());
                maxSize = (maxSize > cluster.size()) ? maxSize : cluster.size();
                maxCluster = (maxSize > cluster.size()) ? maxCluster : cluster;
            }
            LOG.info("Clusters identified = " + clusters.size());
            LOG.info("Maximum Size = " + maxSize);
        }

        return clusters;
    }


}
