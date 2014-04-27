package org.wikibrain.mapper.algorithms.conceptualign3;

import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.InterLanguageLinkDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.lang.LocalId;
import org.wikibrain.core.model.InterLanguageLink;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.mapper.ConceptMapper;
import org.wikibrain.mapper.algorithms.PureWikidataConceptMapper;

import java.io.File;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by bjhecht on 4/24/14.
 */
public class ConceptualignConceptMapper extends ConceptMapper{

    private final PureWikidataConceptMapper wdMapper;
    private final InterLanguageLinkDao illDao;

    private static Logger LOG = Logger.getLogger(ConceptualignConceptMapper.class.getName());


    public ConceptualignConceptMapper(File wikidataFilePath, int id, LocalPageDao<LocalPage> localPageDao, InterLanguageLinkDao illDao) {

        super(id, localPageDao);
        wdMapper = new PureWikidataConceptMapper(wikidataFilePath, -1, localPageDao);
        this.illDao = illDao;
    }

    @Override
    public Iterator<UniversalPage> getConceptMap(LanguageSet ls) throws WikiBrainException, DaoException {


        LOG.log(Level.INFO, "Loading Wikidata concept mappings");
        Iterator<UniversalPage> uPages = wdMapper.getConceptMap(ls);



    }

    private Collection<LocalId> getConnected
}
