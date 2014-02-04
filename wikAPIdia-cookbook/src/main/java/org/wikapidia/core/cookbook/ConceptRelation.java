package org.wikapidia.core.cookbook;

import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalLinkDao;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalLink;
import org.wikapidia.core.model.LocalPage;
import org.wikapidia.core.model.NameSpace;
import org.wikapidia.sr.MonolingualSRMetric;
import org.wikapidia.wikidata.LocalWikidataStatement;
import org.wikapidia.wikidata.WikidataDao;
import org.wikapidia.wikidata.WikidataEntity;
import org.wikapidia.wikidata.WikidataStatement;

import java.io.IOException;
import java.util.*;

/**
 * @author Toby "Jiajun" Li
 *
 * ConceptRelation is a class used to find link connections between two Wikipedia articles or Wikidata items
 */


public class ConceptRelation {

    /**
     *
     * @param lang The language edition of Wikipedia to use
     * @throws ConfigurationException
     * @throws DaoException
     * @throws IOException
     */
    public ConceptRelation(Language lang)throws ConfigurationException, DaoException, IOException{
        Env env = new EnvBuilder().build();
        Configurator conf = env.getConfigurator();
        this.pDao = conf.get(LocalPageDao.class, "sql");   //Get DAOs from default environment & configuration
        this.lDao = conf.get(LocalLinkDao.class, "sql");
        this.wDao = conf.get(WikidataDao.class);
        this.lang = lang;

    }
    LocalPageDao pDao;
    LocalLinkDao lDao;
    Language lang;
    WikidataDao wDao;

    /**
     * Find the shortest chain between two articles (using naive uni-directional BFS, much slower than the bi-directional version)
     * Prints out the chain, the number of links visited and the number of nodes visited
     * @param srcId The page ID of source article
     * @param dstId The page ID of destination article
     * @return The number of degree of the chain found
     * @throws DaoException
     */
    public int getRelation (int srcId, int dstId) throws DaoException {
        Queue<Integer> queue = new LinkedList<Integer>();
        Set<Integer> vectorSet = new HashSet<Integer>();
        Map<Integer, Integer> father = new HashMap<Integer, Integer>();
        queue.add(srcId);
        vectorSet.add(srcId);
        father.put(srcId, -1);
        Integer globalBFSCounter = 0;
        Integer effectiveBFSCounter = 0;
        while(!queue.isEmpty()){
            Integer nowPageId = queue.remove();
            if(nowPageId == dstId){
                int counter = 0;
                while(true){
                    if(father.get(nowPageId) == -1){
                        System.out.println(pDao.getById(lang, nowPageId).getTitle().toString());
                        System.out.printf("BFS counter is %d\n", globalBFSCounter);
                        System.out.printf("Node visited is %d\n", effectiveBFSCounter);
                        return counter;
                    }
                    System.out.print(pDao.getById(lang, nowPageId).getTitle().toString());            //Get page title by page ID
                    System.out.print(" <- ");
                    nowPageId = father.get(nowPageId);
                    counter++;
                }
            }
            Iterable<LocalLink> outlinks = lDao.getLinks(lang, nowPageId, true);
            for(LocalLink outlink : outlinks){
                globalBFSCounter ++;
                if(outlink.isParseable() == false)
                    continue;
                if(vectorSet.contains(outlink.getDestId()))
                    continue;
                effectiveBFSCounter ++;
                father.put(outlink.getDestId(), nowPageId);
                vectorSet.add(outlink.getDestId());
                queue.add(outlink.getDestId());
            }
        }
        return -1;

    }

    /**
     * Find the shortest chain between two articles (using naive uni-directional BFS, much slower than the bi-directional version)
     * Prints out the chain, the number of links visited and the number of nodes visited
     * @param srcTitle The page title of source article
     * @param dstTitle The page title of destination article
     * @return The number of degree of the chain found
     * @throws DaoException
     */

    public int getRelation (String srcTitle, String dstTitle) throws DaoException{
        Integer srcId = pDao.getIdByTitle(srcTitle, lang, NameSpace.ARTICLE);                  //Get page id by page title
        Integer dstId = pDao.getIdByTitle(dstTitle, lang, NameSpace.ARTICLE);
        if(srcId == -1 || dstId == -1)
            throw new DaoException("Page not found");
        return getRelation(srcId, dstId);
    }

    /**
     * Find a chain between two articles (using the SR (semantic relatedness) between the current article and the destination article as heuristic)
     * Prints out the chain, the number of links visited and the number of nodes visited
     * @param srcId The page ID of source article
     * @param dstId The page ID of destination article
     * @return The number of degree of the chain found
     * @throws DaoException
     */
    public int getRelationSR (int srcId, int dstId) throws DaoException, ConfigurationException {
        final MonolingualSRMetric sr = new Configurator(new Configuration()).get(
                MonolingualSRMetric.class, "ensemble",      //can be change to "inlink" for another SR implementation
                "language", lang.getLangCode());            //initialize a SR resolver
        final int finalDstId = dstId;
        Comparator<Integer> QueueComparator = new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2){
                try {
                    if(sr.similarity(o1, finalDstId, false).getScore() < sr.similarity(o2, finalDstId, false).getScore())   //get SR similarity between o1/o2 and the destination article
                        return 1;
                    else
                        return -1;

                }
                catch (Exception e){
                    /*do nothing*/
                    //System.out.printf("Exception when processing page %d and %d\n", o1.intValue(), o2.intValue());
                }


                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }
        };
        PriorityQueue<Integer> queue = new PriorityQueue<Integer>(1000000, QueueComparator);
        Set<Integer> vectorSet = new HashSet<Integer>();
        Map<Integer, Integer> father = new HashMap<Integer, Integer>();
        Set<Integer> closedSet = new HashSet<Integer>();
        queue.add(srcId);
        vectorSet.add(srcId);
        father.put(srcId, -1);
        Integer globalBFSCounter = 0;
        Integer effectiveBFSCounter = 0;
        while(!queue.isEmpty()){
            Integer nowPageId = queue.poll();
            closedSet.add(nowPageId);
            if(nowPageId == dstId){
                int counter = 0;
                while(true){
                    if(father.get(nowPageId) == -1){
                        System.out.println(pDao.getById(lang, nowPageId).getTitle().toString());
                        System.out.printf("BFS counter is %d\n", globalBFSCounter);
                        System.out.printf("Node visited is %d\n", effectiveBFSCounter);
                        return counter;
                    }
                    System.out.print(pDao.getById(lang, nowPageId).getTitle().toString());
                    System.out.print(" <- ");
                    nowPageId = father.get(nowPageId);
                    counter++;
                }
            }
            Iterable<LocalLink> outlinks = lDao.getLinks(lang, nowPageId, true);
            for(LocalLink outlink : outlinks){
                globalBFSCounter ++;
                if(outlink.isParseable() == false)
                    continue;
                if(vectorSet.contains(outlink.getDestId()))
                    continue;
                effectiveBFSCounter ++;
                father.put(outlink.getDestId(), nowPageId);
                vectorSet.add(outlink.getDestId());
                queue.add(outlink.getDestId());
            }
        }
        return -1;

    }
    /**
     * Find a chain between two articles (using the SR (semantic relatedness) between the current article and the destination article as heuristic)
     * Prints out the chain, the number of links visited and the number of nodes visited
     * @param srcTitle The page title of source article
     * @param dstTitle The page title of destination article
     * @return The number of degree of the chain found
     * @throws DaoException
     */
    public int getRelationSR (String srcTitle, String dstTitle) throws DaoException, ConfigurationException{
        Integer srcId = pDao.getIdByTitle(srcTitle, lang, NameSpace.ARTICLE);
        Integer dstId = pDao.getIdByTitle(dstTitle, lang, NameSpace.ARTICLE);
        if(srcId == -1 || dstId == -1)
            throw new DaoException("Page not found");
        return getRelationSR(srcId, dstId);
    }

    /**
     * Find a shortest chain between two articles using the Bi-directional BFS
     * Prints out the chain, the number of links visited and the number of nodes visited
     * @param srcId The page ID of source article
     * @param dstId The page ID of destination article
     * @return The number of degree of the chain found
     * @throws DaoException
     */

    public int getRelationBidirectional (int srcId, int dstId) throws DaoException {
        Queue<Integer> srcQueue = new LinkedList<Integer>();
        Queue<Integer> dstQueue = new LinkedList<Integer>();
        LinkedList<Integer> resList = new LinkedList<Integer>();
        Set<Integer> srcVectorSet = new HashSet<Integer>();
        Set<Integer> dstVectorSet = new HashSet<Integer>();
        Map<Integer, Integer> srcFather = new HashMap<Integer, Integer>();
        Map<Integer, Integer> dstFather = new HashMap<Integer, Integer>();
        srcQueue.add(srcId);
        dstQueue.add(dstId);
        srcVectorSet.add(srcId);
        dstVectorSet.add(dstId);
        srcFather.put(srcId, -1);
        dstFather.put(dstId, -1);
        Integer globalBFSCounter = 0;
        Integer effectiveBFSCounter = 0;
        while(!(srcQueue.isEmpty() || dstQueue.isEmpty())){
            if(srcQueue.size() < dstQueue.size()){
                Integer nowPageId = srcQueue.remove();
                Iterable<LocalLink> outlinks = lDao.getLinks(lang, nowPageId, true);
                for(LocalLink outlink : outlinks){
                    globalBFSCounter ++;
                    if(outlink.isParseable() == false)
                        continue;
                    if(srcVectorSet.contains(outlink.getDestId()))
                        continue;
                    if(dstVectorSet.contains(outlink.getDestId())){
                        int counter = 0;
                        Integer backPageId = outlink.getDestId();
                        while(true){
                            resList.addLast(backPageId);
                            if(dstFather.get(backPageId) == -1){
                                break;
                            }
                            backPageId = dstFather.get(backPageId);
                            counter++;
                        }
                        while(true){
                            resList.addFirst(nowPageId);
                            if(srcFather.get(nowPageId) == -1){
                                for (Integer e : resList){
                                    System.out.print(pDao.getById(lang, e).getTitle().toString());
                                    if(e != dstId)
                                        System.out.print(" -> ");
                                }
                                System.out.print("\n");
                                System.out.printf("BFS counter is %d\n", globalBFSCounter);
                                System.out.printf("Node visited is %d\n", effectiveBFSCounter);
                                return counter + 1;
                            }
                            nowPageId = srcFather.get(nowPageId);
                            counter++;
                        }
                    }
                    effectiveBFSCounter ++;
                    srcFather.put(outlink.getDestId(), nowPageId);
                    srcVectorSet.add(outlink.getDestId());
                    srcQueue.add(outlink.getDestId());
                }
            }
            else{
                Integer nowPageId = dstQueue.remove();
                Iterable<LocalLink> outlinks = lDao.getLinks(lang, nowPageId, false);
                for(LocalLink outlink : outlinks){
                    globalBFSCounter ++;
                    if(outlink.isParseable() == false)
                        continue;
                    if(dstVectorSet.contains(outlink.getSourceId()))
                        continue;
                    if(srcVectorSet.contains(outlink.getSourceId())){
                        int counter = 0;
                        Integer backPageId = outlink.getSourceId();
                        while(true){
                            resList.addFirst(backPageId);
                            if(srcFather.get(backPageId) == -1){
                                break;
                            }
                            backPageId = srcFather.get(backPageId);
                            counter++;
                        }
                        while(true){
                            resList.addLast(nowPageId);
                            if(dstFather.get(nowPageId) == -1){
                                for (Integer e : resList){
                                    System.out.print(pDao.getById(lang, e).getTitle().toString());
                                    if(e != dstId)
                                        System.out.print(" -> ");
                                }
                                System.out.print("\n");
                                System.out.printf("BFS counter is %d\n", globalBFSCounter);
                                System.out.printf("Node visited is %d\n", effectiveBFSCounter);
                                return counter + 1;
                            }
                            nowPageId = dstFather.get(nowPageId);
                            counter++;
                        }
                    }
                    effectiveBFSCounter ++;
                    dstFather.put(outlink.getSourceId(), nowPageId);
                    dstVectorSet.add(outlink.getSourceId());
                    dstQueue.add(outlink.getSourceId());
                }
            }

        }
        return -1;

    }

    /**
     * Find a shortest chain between two articles using the Bi-directional BFS
     * Prints out the chain, the number of links visited and the number of nodes visited
     * @param srcTitle The page title of source article
     * @param dstTitle The page title of destination article
     * @return The number of degree of the chain found
     * @throws DaoException
     */

    public int getRelationBidirectional (String srcTitle, String dstTitle) throws DaoException, ConfigurationException{
        Integer srcId = pDao.getIdByTitle(srcTitle, lang, NameSpace.ARTICLE);
        Integer dstId = pDao.getIdByTitle(dstTitle, lang, NameSpace.ARTICLE);
        if(srcId == -1 || dstId == -1)
            throw new DaoException("Page not found");
        return getRelationBidirectional(srcId, dstId);
    }

    private String getName (String statement){
        return statement.substring(statement.indexOf("name=")+5, statement.indexOf("}"));
    }

    /**
     * Find a shortest chain between two Wikidata items using the uni-directional BFS
     * Prints out the chain, the number of links visited and the number of nodes visited
     * @param srcId The item ID of the source item
     * @param dstId The item ID of the destination item
     * @return The number of degree of the chain found
     * @throws DaoException
     */

    public int getWikidataRelation (int srcId, int dstId) throws DaoException {
        WikidataEntity srcEntity = wDao.getItem(srcId);
        WikidataEntity dstEntity = wDao.getItem(dstId);
        Queue<WikidataEntity> queue = new LinkedList<WikidataEntity>();
        Set<Integer> vectorSet = new HashSet<Integer>();
        Map<Integer, Integer> QFather = new HashMap<Integer, Integer>();
        Map<Integer, Integer> PFather = new HashMap<Integer, Integer>();
        queue.add(srcEntity);
        vectorSet.add(srcEntity.getId());
        QFather.put(srcEntity.getId(), -1);
        Integer globalBFSCounter = 0;
        Integer effectiveBFSCounter = 0;
        while(!queue.isEmpty()){
            WikidataEntity nowEntity = queue.poll();
            if(nowEntity.getId() == dstId){
                /*found*/
                int counter = 0;
                while(true){
                    if(QFather.get(nowEntity.getId()) == -1){
                        System.out.println(getName(nowEntity.toString()));
                        System.out.printf("BFS counter is %d\n", globalBFSCounter);
                        System.out.printf("Node visited is %d\n", effectiveBFSCounter);
                        return counter;
                    }
                    System.out.print(getName(nowEntity.toString()));
                    System.out.print(" <-(");
                    System.out.print(getName(wDao.getProperty(PFather.get(nowEntity.getId())).toString()));      //Get the name of an item by wikidataDao
                    System.out.print(")- ");
                    nowEntity = wDao.getItem(QFather.get(nowEntity.getId()));
                    counter++;
                }
            }

            Map<String, List<LocalWikidataStatement>> statementsMap = wDao.getLocalStatements(lang, nowEntity.getType(), nowEntity.getId());
            Map<WikidataEntity, Integer> sons = new HashMap<WikidataEntity, Integer>();
            for(String s : statementsMap.keySet()){
                for(LocalWikidataStatement l : statementsMap.get(s)){
                    if(l.getStatement().getValue().getTypeName() != "ITEM")
                        continue;
                    sons.put(wDao.getItem(l.getStatement().getValue().getIntValue()), l.getStatement().getProperty().getId());   //Get the item by wikidataDao
                }
            }
            for(WikidataEntity son : sons.keySet()){
                globalBFSCounter ++;
                if(vectorSet.contains(son.getId()))
                    continue;
                effectiveBFSCounter ++;
                QFather.put(son.getId(), nowEntity.getId());
                PFather.put(son.getId(), sons.get(son));
                vectorSet.add(son.getId());
                queue.add(son);
            }
        }
        return -1;

    }

}
