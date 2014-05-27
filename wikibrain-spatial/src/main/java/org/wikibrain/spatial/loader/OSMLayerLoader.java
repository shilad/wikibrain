package org.wikibrain.spatial.loader;

import org.wikibrain.core.WikiBrainException;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataFilter;
import org.wikibrain.wikidata.WikidataStatement;

/**
 * Created by bjhecht on 5/21/14.
 *
 * Starting point for Aaron's OSM work.
 */
public class OSMLayerLoader {



    private final WikidataDao wdDao;

    private static final int OSM_RELATION_ID = 402;

    public OSMLayerLoader(WikidataDao wdDao){

        this.wdDao = wdDao;

    }

    public void printGeometries() throws WikiBrainException{

        try {

            WikidataFilter filter = (new WikidataFilter.Builder()).withPropertyId(OSM_RELATION_ID).build();
            Iterable<WikidataStatement> osmRelations = wdDao.get(filter);

            for (WikidataStatement osmRelation : osmRelations){
                osmRelation.getValue();

                //http://wiki.openstreetmap.org/wiki/API_v0.6#Read:_GET_.2Fapi.2F0.6.2F.5Bnode.7Cway.7Crelation.5D.2F.23id
            }


        }catch(DaoException e){
            throw new WikiBrainException(e);
        }

    }

}
