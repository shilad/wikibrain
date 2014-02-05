package org.wikapidia.core.cookbook;

import org.wikapidia.conf.Configuration;
import org.wikapidia.conf.ConfigurationException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalPageDao;
import org.wikapidia.core.dao.RedirectDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalPage;
import java.io.IOException;
import org.wikapidia.core.cookbook.ConceptRelation;
import org.wikapidia.core.model.NameSpace;

/** An example shows the usage of ConceptRelation class
 * @author Toby "Jiajun" Li
 */


public class ConceptRelationTest{

//"getRelationSR" requires the initialization of SR matrix, "getWikidataRelation" requires the installation of wikidata.
    public static void main(String args[]) throws ConfigurationException, DaoException, IOException {
        ConceptRelation cr = new ConceptRelation(Language.getByLangCode("simple"));

        System.out.printf("Number of degree %d\n\n", cr.getRelationSR("Minnesota", "Database")); //Also support the usage "getRelationSR(pageId, pageId)
        System.out.printf("Number of degree %d\n", cr.getRelationBidirectional("Minnesota", "Database")); //Also support the usage getRelationBidirectional(pageId, pageId)
        Integer srcId = 1527;
        Integer dstId = 43788;
        System.out.println(cr.getWikidataRelation(srcId, dstId));

    }


}
