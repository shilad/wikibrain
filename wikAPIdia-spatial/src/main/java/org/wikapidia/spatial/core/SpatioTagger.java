package org.wikapidia.spatial.core;

/**
 * Created by toby on 3/23/14.
 */
import de.tudarmstadt.ukp.wikipedia.api.exception.WikiApiException;
import org.wikapidia.conf.Configurator;
import org.wikapidia.core.cmd.Env;
import org.wikapidia.core.cmd.EnvBuilder;
import org.wikapidia.core.dao.DaoException;
import org.wikapidia.core.dao.LocalArticleDao;
import org.wikapidia.core.dao.UniversalArticleDao;
import org.wikapidia.core.lang.Language;
import org.wikapidia.core.model.LocalArticle;
import org.wikapidia.core.model.Title;
import org.wikapidia.spatial.core.dao.SpatialDataDao;
import org.wikapidia.spatial.core.dao.SpatioTagDao;


import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class SpatioTagger {

    public static void main(String[] args){

        try{

            Env env = new EnvBuilder().build();
            Configurator configurator = env.getConfigurator();
            UniversalArticleDao universalArticleDao = configurator.get(UniversalArticleDao.class, "sql");
            LocalArticleDao localArticleDao = configurator.get(LocalArticleDao.class, "sql");
            Language lang = Language.getByLangCode("en");
            Set<Integer> langIdSet = new HashSet<Integer>();
            langIdSet.add(new Integer(lang.getId()));
            SpatioTagger spatioTagger = new SpatioTagger(universalArticleDao, localArticleDao, langIdSet);

            spatioTagger.spatioTag();

        }catch(Exception e){
            e.printStackTrace();
        }

    }

    public static final Logger LOG = Logger.getLogger(SpatioTagger.class.getName());
    UniversalArticleDao universalArticleDao;
    LocalArticleDao localArticleDao;
    SpatioTagDao spatioTagDao;
    SpatialDataDao spatialDataDao;
    Set<Integer> languageIdSet;

    public SpatioTagger(UniversalArticleDao universalArticleDao, LocalArticleDao localArticleDao, Set<Integer> languageIdSet) {
        this.universalArticleDao = universalArticleDao;
        this.localArticleDao = localArticleDao;
        this.languageIdSet = languageIdSet;
    }

    public void spatioTag() throws DaoException{


        try{





            //TODO: construct the SQL statement used to fetch the geometries from the database
            //TODO: Problem: how to match the "names" for each geometries in the new system?


            Statement ws = w.getDB().advanced_getStatement();
            WikapidiaStatement wStatement = new WikapidiaStatement(ws);
            wStatement.addElement("wapi_spatiotags");

            String sql = "SELECT lang_code, name, wapi_geometry_names.geom_id, layer_name, ref_sys_name, shape_type FROM wapi_geometry_names, wapi_geometries " +
                    "WHERE wapi_geometry_names.geom_id = ? AND wapi_geometry_names.geom_id = wapi_geometries.geom_id";
            PreparedStatement pps = w.getSpatialDb().advanced_prepareStatement(sql);


            Integer maxGeomId = spatialDataDao.getMaximumGeomId();


            int counter = 1;
            int matchCounter = 0;
            //Integer algId = w.getUniversalConceptAlgorithmId();
            Set<String> alreadyFound = new HashSet<String>();
            spatioTagDao.beginSaveSpatioTags();

            while(counter <= maxGeomId){



            //TODO: try to fetch the set of names for each geometry from the database

                pps.setInt(1, counter);
                ResultSet rs = pps.executeQuery();


                while(rs.next()){

                    String langCode = rs.getString("lang_code");
                    Integer langId = new Integer(Language.getByLangCode(langCode).getId());
//
                    if (languageIdSet.contains(langId)){
                        Language lang = Language.getById(langId);
                        Title title = new Title(rs.getString("name"), lang);

                        try{

                            LocalArticle localArticle = localArticleDao.getByTitle(lang, title);
                            Integer algorithmId = 0;  /*Universal Article Algorithm Id*/
                            Integer univId = universalArticleDao.getUnivPageId(localArticle, algorithmId);


                            String layerName = rs.getString("layer_name");
                            String refSysName = rs.getString("ref_sys_name");
                            String key = localArticle.toString();

                            if (!alreadyFound.contains(key)){

                                Integer geomId = rs.getInt("geom_id");
                                Integer shapeType = rs.getInt("shape_type");


                            //TODO: Problem: So we store local ID here instead of universal ID?


                                spatioTagDao.saveSpatioTag(new SpatioTagDao.SpatioTagStruct(localArticle.toLocalId(), geomId));

                                matchCounter++;
                                alreadyFound.add(key);

                                break; // only one match geom id

                            }else{
                                LOG.info("Found add'l row for universal article with id: " + univId.toString()  +
                                        " - " + key);
                            }

                        }catch(DaoException e){

                            // Failed to find an article with given name
                            // DO NOTHING



//							System.out.println("Found invalid ua: " + title);
                        }

                    }

                }
                if (counter % 10000 == 0){
                    LOG.info(String.format("Done with %d name entries: matched %f%%. (Note: occassional violation of primary key is okay here.)", counter, ((100.0*matchCounter)/counter)));
                }
                counter++;
            }
            spatioTagDao.endSaveSpatioTags();


        }





    }




}
