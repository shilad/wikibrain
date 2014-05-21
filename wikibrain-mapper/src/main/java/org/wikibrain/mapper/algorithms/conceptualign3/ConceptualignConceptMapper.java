package org.wikibrain.mapper.algorithms.conceptualign3;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.InterLanguageLinkDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.MetaInfoDao;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.InterLanguageLink;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.mapper.ConceptMapper;
import org.wikibrain.mapper.MapperIterator;
import org.wikibrain.mapper.algorithms.PureWikidataConceptMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by bjhecht on 4/24/14.
 *
 * Only supports article namespaces for now (no disambiguation pages), but this can be changed easily by manipulating the filters
 * in ILLGraph and adapting ClusterResult to contain namespace info (i.e. MapperIterator below can't just return Namespace.Article
 *
 * Disambiguation page support will be more difficult given that we have split articles and disambiguation pages into
 * separate namespaces.
 *
 */
public class ConceptualignConceptMapper extends ConceptMapper{

    private final PureWikidataConceptMapper wdMapper;
    private final InterLanguageLinkDao illDao;
    private final MetaInfoDao miDao;


    private static Logger LOG = Logger.getLogger(ConceptualignConceptMapper.class.getName());


    public ConceptualignConceptMapper(File wikidataFilePath, int id,
                 LocalPageDao<LocalPage> localPageDao, InterLanguageLinkDao illDao, MetaInfoDao miDao) {

        super(id, localPageDao);
        this.illDao = illDao;
        this.miDao = miDao;
        wdMapper = new PureWikidataConceptMapper(wikidataFilePath, -1, localPageDao);

    }

    @Override
    public Iterator<UniversalPage> getConceptMap(LanguageSet ls) throws WikiBrainException, DaoException {

        // parameters
        int maxEdge = 1; // see Bao et al. 2012 for definition
        double minLang = 0.5; // see Bao et al. 2012 for definition

        // load Wikidata mappings
        LOG.log(Level.INFO, "Loading Wikidata concept mappings");
        Iterator<UniversalPage> uPages = wdMapper.getConceptMap(ls);

        // perform Conceptualign
        CombinedIllDao combinedDao = new CombinedIllDao(uPages, illDao);
        ILLGraph illGraph = new ILLGraph(combinedDao, localPageDao, miDao);

        BreadthFirstIterator<LocalId, ILLEdge> bfi = new BreadthFirstIterator<LocalId, ILLEdge>(illGraph);
		List<ConnectedComponentHandler> ccHandlers = new ArrayList<ConnectedComponentHandler>();
		ccHandlers.add(new Conceptualign3ConnectedComponentHandler(0.5, 1, true, this.localPageDao));


		ConnectedComponentTraversalListener listener =
				new ConnectedComponentTraversalListener(illGraph, ccHandlers);
		bfi.addTraversalListener(listener);
		while (bfi.hasNext()){
			LocalId localId = bfi.next();
		}



        return new MapperIterator<UniversalPage>(listener.getClusterResults()) {
            @Override
            public UniversalPage transform(Object obj) {
                ClusterResult curCluster = (ClusterResult)obj;
                return new UniversalPage(curCluster.univId, getId(), NameSpace.ARTICLE, curCluster.vertices);
            }
        };

    }


}
