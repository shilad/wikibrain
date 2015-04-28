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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by bjhecht on 4/24/14.
 *
 * Only supports article namespaces for now (no disambiguation pages), but this can be changed easily by manipulating the filters
 * in ILLGraph and adapting ClusterResult to contain namespace info (i.e. MapperIterator below can't just return Namespace.Article
 *
 * Disambiguation page support will be more difficult given that we have split articles and disambiguation pages into
 * separate namespaces.
 *
 * Due to the changes associated with Wikidata, much more research is needed in this area. MaxEdges has been set to 2 as this
 * seems reasonable, but again, more research is definitely need.
 *
 */
public class ConceptualignConceptMapper extends ConceptMapper{

    private PureWikidataConceptMapper wdMapper;
    private final InterLanguageLinkDao illDao;
    private final MetaInfoDao miDao;

    private Iterable<UniversalPage> uPages;

    private LanguageSet uPageLs;

    private final boolean print;

    private static Logger LOG = LoggerFactory.getLogger(ConceptualignConceptMapper.class);


    public ConceptualignConceptMapper(File wikidataFilePath, int id,
                 LocalPageDao localPageDao, InterLanguageLinkDao illDao, MetaInfoDao miDao, boolean print) {

        super(id, localPageDao);
        this.illDao = illDao;
        this.miDao = miDao;
        this.print = print;
        wdMapper = new PureWikidataConceptMapper(wikidataFilePath, -1, localPageDao);

    }

    /**
     * For testing purposes only
     * @param uPages
     * @param id
     * @param localPageDao
     * @param illDao
     * @param miDao
     */
    public ConceptualignConceptMapper(Iterable<UniversalPage> uPages, LanguageSet uPagesLs, int id, LocalPageDao localPageDao,
                                      InterLanguageLinkDao illDao, MetaInfoDao miDao, boolean print) {
        super(id, localPageDao);
        this.illDao = illDao;
        this.miDao = miDao;
        this.uPages = uPages;
        this.print = print;

        this.uPageLs = uPagesLs;

    }

    @Override
    public Iterator<UniversalPage> getConceptMap(LanguageSet ls) throws WikiBrainException, DaoException {

        // parameters
        int maxEdge = 2; // see Bao et al. 2012 for definition
        double minLang = 1.0; // see Bao et al. 2012 for definition

        // load Wikidata mappings
        if (uPages == null) {
            LOG.info("Loading Wikidata concept mappings");
            Iterator<UniversalPage> uPages = wdMapper.getConceptMap(ls);
        }else{
            if (!ls.equals(uPageLs)){
                throw new WikiBrainException("LanguageSet mismatch");
            }
        }

        // perform Conceptualign
        CombinedIllDao combinedDao = new CombinedIllDao(uPages.iterator(), illDao);
        ILLGraph illGraph = new ILLGraph(combinedDao, localPageDao, miDao);

        BreadthFirstIterator<LocalId, ILLEdge> bfi = new BreadthFirstIterator<LocalId, ILLEdge>(illGraph);
		List<ConnectedComponentHandler> ccHandlers = new ArrayList<ConnectedComponentHandler>();
		ccHandlers.add(new Conceptualign3ConnectedComponentHandler(minLang, maxEdge, true, this.localPageDao));


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
