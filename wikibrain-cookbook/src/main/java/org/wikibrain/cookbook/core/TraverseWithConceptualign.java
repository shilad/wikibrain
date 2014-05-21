package org.wikibrain.cookbook.core;

import org.wikibrain.conf.Configurator;
import org.wikibrain.core.cmd.Env;
import org.wikibrain.core.cmd.EnvBuilder;
import org.wikibrain.core.dao.*;
import org.wikibrain.core.lang.LanguageSet;
import org.wikibrain.core.model.InterLanguageLink;
import org.wikibrain.core.model.NameSpace;
import org.wikibrain.core.model.UniversalPage;
import org.wikibrain.mapper.algorithms.conceptualign3.ConceptualignConceptMapper;
import org.wikibrain.wikidata.WikidataDao;
import org.wikibrain.wikidata.WikidataFilter;

/**
 * Created by bjhecht on 5/21/14.
 */
public class TraverseWithConceptualign {

    public static void main(String[] args){

        try {

            Env env = EnvBuilder.envFromArgs(args);
            Configurator c = env.getConfigurator();

            UniversalPageDao upDao = c.get(UniversalPageDao.class);
            MetaInfoDao miDao = c.get(MetaInfoDao.class);
            LocalPageDao lpDao = c.get(LocalPageDao.class);
            InterLanguageLinkDao illDao = c.get(InterLanguageLinkDao.class);

            LanguageSet loadedLangs = miDao.getLoadedLanguages(UniversalPageDao.class);

            Iterable<UniversalPage> uPages = upDao.get(new DaoFilter().setLanguages(loadedLangs).setNameSpaces(NameSpace.ARTICLE));

            ConceptualignConceptMapper conceptualignMapper = new ConceptualignConceptMapper(uPages, loadedLangs, -1, lpDao, illDao, miDao, true);

            conceptualignMapper.getConceptMap(loadedLangs);

        }catch(Exception e){
            e.printStackTrace();;
        }

    }

}
