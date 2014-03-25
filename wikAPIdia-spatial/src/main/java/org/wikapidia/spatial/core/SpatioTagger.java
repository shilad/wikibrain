package org.wikapidia.spatial.core;

/**
 * Created by toby on 3/23/14.
 */

import org.wikapidia.conf.Configurator;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalArticleDao;

import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.Title;

import org.wikapidia.spatial.core.dao.SpatioTagDao;
import org.wikapidia.spatial.core.dao.postgis.PostGISSpatioTagDao;


import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class SpatioTagger {

    public static void main(String[] args){

        try{

            Env env = new EnvBuilder().build();
            Configurator configurator = env.getConfigurator();
            LocalArticleDao localArticleDao = configurator.get(LocalArticleDao.class, "sql");
            Language lang = Language.getByLangCode("en");
            SpatioTagDao spatioTagDao = configurator.get(SpatioTagDao.class, "postgis");
            Set<Integer> langIdSet = new HashSet<Integer>();
            langIdSet.add(new Integer(lang.getId()));

            SpatialGeomNameMapping spatialGeomNameMapping = new SpatialGeomNameMapping();

            SpatioTagger spatioTagger = new SpatioTagger(spatialGeomNameMapping, localArticleDao, langIdSet, spatioTagDao);
            spatioTagger.spatioTag();

        }catch(Exception e){
            e.printStackTrace();
        }

    }

    public static final Logger LOG = Logger.getLogger(SpatioTagger.class.getName());
    LocalArticleDao localArticleDao;
    SpatioTagDao spatioTagDao;
    Set<Integer> languageIdSet;
    SpatialGeomNameMapping spatialGeomNameMapping;

    public SpatioTagger(SpatialGeomNameMapping spatialGeomNameMapping, LocalArticleDao localArticleDao, Set<Integer> languageIdSet, SpatioTagDao spatioTagDao) {
        this.spatioTagDao = spatioTagDao;
        this.spatialGeomNameMapping = spatialGeomNameMapping;
        this.localArticleDao = localArticleDao;
        this.languageIdSet = languageIdSet;


    }

    public void spatioTag() throws DaoException{

        int counter = 1;
        int matchCounter = 0;
        //Integer algId = w.getUniversalConceptAlgorithmId();
        Set<String> alreadyFound = new HashSet<String>();
        spatioTagDao.beginSaveSpatioTags();


        for(SpatialGeomName geomName: spatialGeomNameMapping.getAllGeomNames()){
            if(! languageIdSet.contains(geomName.getLangId()))
                continue;
            Language lang = geomName.getLang();
            Title title = new Title(geomName.getGeomName(), lang);
            try{
                LocalArticle localArticle = localArticleDao.getByTitle(lang, title);
                //Integer algorithmId = 0;  /*Universal Article Algorithm Id*/
                //Integer univId = universalArticleDao.getUnivPageId(localArticle, algorithmId);
                String key = localArticle.toString();
                if (!alreadyFound.contains(key)){
                    Integer geomId = spatialGeomNameMapping.getGeomIdForGeomName(geomName);
                    spatioTagDao.saveSpatioTag(new SpatioTagDao.SpatioTagStruct(localArticle.toLocalId(), geomId));
                    matchCounter++;
                    alreadyFound.add(key);
                    continue; // only one match geom id
                }
                else {
                        LOG.info("Found add'l row for universal article with local id: " + new Integer(localArticle.getLocalId()).toString()  +
                                    " - " + key);
                }

            }
            catch(DaoException e){
                    // Failed to find an article with given name
                    // DO NOTHING
//		        	System.out.println("Found invalid ua: " + title);
            }

            if (counter % 10000 == 0){
                LOG.info(String.format("Done with %d name entries: matched %f%%. (Note: occassional violation of primary key is okay here.)", counter, ((100.0*matchCounter)/counter)));
            }
            counter++;

        }

        spatioTagDao.endSaveSpatioTags();

    }




}
