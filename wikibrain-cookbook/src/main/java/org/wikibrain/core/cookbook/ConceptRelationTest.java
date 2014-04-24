package org.wikibrain.core.cookbook;

import org.wikibrain.conf.Configuration;
import org.wikibrain.conf.ConfigurationException;
import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.DaoException;
import org.wikibrain.core.dao.LocalPageDao;
import org.wikibrain.core.dao.RedirectDao;
import org.wikibrain.core.lang.Language;
import org.wikibrain.core.model.LocalPage;
import java.io.IOException;
import org.wikibrain.core.cookbook.ConceptRelation;
import org.wikibrain.core.model.NameSpace;

/** An example shows the usage of ConceptRelation class
 * @author Toby "Jiajun" Li
 */


public class ConceptRelationTest{

//"getRelationSR" requires the initialization of SR matrix, "getWikidataRelation" requires the installation of wikidata.
    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
        ConceptRelation cr = new ConceptRelation(Language.getByLangCode("simple"));

        System.out.printf("Number of degree %d\n\n", cr.getRelationSR("Minnesota", "Peanut milk")); //Also support the usage "getRelationSR(pageId, pageId)
        System.out.printf("Number of degree %d\n", cr.getRelationBidirectional("Minnesota", "Peanut milk")); //Also support the usage getRelationBidirectional(pageId, pageId)
        //System.out.printf("Number of degree %d\n", cr.getRelation("New York", "Peanut milk"));
        Integer srcId = 1527;
        Integer dstId = 43788;
        //System.out.println(cr.getWikidataRelation(srcId, dstId));

    }


}
