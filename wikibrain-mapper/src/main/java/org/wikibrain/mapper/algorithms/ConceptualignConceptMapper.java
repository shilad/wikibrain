package org.wikibrain.mapper.algorithms;

import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.InterLanguageLinkDao;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.InterLanguageLink;
import org.wikibrain.core.model.LocalPage;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.mapper.ConceptMapper;

import java.io.File;
import java.util.Iterator;

/**
 * Created by bjhecht on 4/24/14.
 */
public class ConceptualignConceptMapper extends ConceptMapper{

    private final PureWikidataConceptMapper wdMapper;
    private final InterLanguageLinkDao illDao;


    public ConceptualignConceptMapper(File wikidataFilePath, int id, LocalPageDao<LocalPage> localPageDao, InterLanguageLinkDao illDao) {

        super(id, localPageDao);
        wdMapper = new PureWikidataConceptMapper(wikidataFilePath, -1, localPageDao);
        this.illDao = illDao;
    }

    @Override
    public Iterator<UniversalPage> getConceptMap(LanguageSet ls) throws WikiBrainException, DaoException {

        return null;

    }
}
